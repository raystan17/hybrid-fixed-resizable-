package com.abu.hybridinventory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.util.ColorUtil;

/**
 * Overlay that renders a fixed-classic style inventory panel on the right side of the screen.
 * Displays 28 slots in a 4x7 grid, anchored to the right edge of the client window.
 * Does not stretch or scale; maintains ~241px classic width.
 */
public class HybridInventoryOverlay extends Overlay
{
	private static final int CLASSIC_INVENTORY_WIDTH = 241;
	private static final int SLOT_SIZE = 42;
	private static final int SLOT_PADDING = 1;
	private static final int COLS = 4;
	private static final int ROWS = 7;
	private static final int SLOTS = 28;

	// Panel dimensions (fixed, no scaling)
	private static final int PANEL_WIDTH = CLASSIC_INVENTORY_WIDTH;
	private static final int PANEL_PADDING = 6;
	private static final int WATERMARK_HEIGHT = 16;

	// VarClientInt for viewport mode: 0 = fixed, 1 = resizable classic, 2 = resizable modern
	private static final int VIEWPORT_MODE_VARC = 43;

	private final Client client;
	private final ItemManager itemManager;
	private final HybridInventoryConfig config;

	private final PanelComponent panelComponent = new PanelComponent();

	@Inject
	public HybridInventoryOverlay(Client client, ItemManager itemManager, HybridInventoryConfig config)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.config = config;

		setPosition(OverlayPosition.TOP_RIGHT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.HIGH);
		setPreferredSize(new Dimension(PANEL_WIDTH, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.enabled())
		{
			return null;
		}

		// Only show in resizable mode
		if (!isResizableMode())
		{
			return null;
		}

		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory == null)
		{
			return null;
		}

		// Calculate panel height: slots + watermark
		int slotsHeight = ROWS * (SLOT_SIZE + SLOT_PADDING) - SLOT_PADDING;
		int panelHeight = PANEL_PADDING * 2 + slotsHeight + WATERMARK_HEIGHT;

		// Clear previous menu entries and add dynamic menu entries for slot under mouse
		List<OverlayMenuEntry> entries = getMenuEntries();
		entries.clear();

		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		Rectangle overlayBounds = getBounds();
		if (overlayBounds != null && overlayBounds.contains(mouse.getX(), mouse.getY()))
		{
			int slot = getSlotAtPoint(mouse.getX(), mouse.getY(), overlayBounds);
			if (slot >= 0 && slot < SLOTS)
			{
				Item item = inventory.getItem(slot);
				if (item != null && item.getId() != -1)
				{
					Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
					if (inventoryWidget != null)
					{
						Widget slotWidget = inventoryWidget.getChild(slot);
						if (slotWidget != null)
						{
							String itemName = itemManager.getItemComposition(item.getId()).getName();
							int widgetId = slotWidget.getId();
							int itemId = item.getId();

							// Add menu entries for all item actions (Use, Drop, Eat, Wear, etc.)
							String[] actions = itemManager.getItemComposition(itemId).getInventoryActions();
							boolean hasExamine = false;
							for (int opIndex = 0; opIndex < actions.length; opIndex++)
							{
								String action = actions[opIndex];
								if (action != null && !action.isEmpty())
								{
									if ("Examine".equalsIgnoreCase(action))
									{
										hasExamine = true;
									}
									final int op = opIndex + 1; // CC_OP uses 1-indexed ops
									addMenuEntry(MenuAction.RUNELITE_OVERLAY, action, itemName, e -> invokeSlotAction(op, widgetId, slot, itemId, action, itemName));
								}
							}
							// Examine is always available (add if not in actions)
							if (!hasExamine)
							{
								addMenuEntry(MenuAction.RUNELITE_OVERLAY, "Examine", itemName, e -> invokeSlotAction(10, widgetId, slot, itemId, "Examine", itemName));
							}
						}
					}
				}
			}
		}

		// Draw panel background
		panelComponent.getChildren().clear();
		panelComponent.setPreferredSize(new Dimension(PANEL_WIDTH, panelHeight));
		panelComponent.setBackgroundColor(ComponentConstants.STANDARD_BACKGROUND_COLOR);
		panelComponent.render(graphics);

		// Draw inventory slots (4x7 grid) with Graphics2D for precise control
		int offsetX = PANEL_PADDING;
		int offsetY = PANEL_PADDING;

		for (int slot = 0; slot < SLOTS; slot++)
		{
			int col = slot % COLS;
			int row = slot / COLS;
			int x = offsetX + col * (SLOT_SIZE + SLOT_PADDING);
			int y = offsetY + row * (SLOT_SIZE + SLOT_PADDING);

			// Draw slot border
			graphics.setColor(Color.GRAY);
			graphics.drawRect(x, y, SLOT_SIZE, SLOT_SIZE);

			Item item = inventory.getItem(slot);
			if (item != null && item.getId() != -1)
			{
				BufferedImage image = itemManager.getImage(item.getId(), item.getQuantity(), item.getQuantity() > 1);
				if (image != null)
				{
					graphics.drawImage(image, x, y, SLOT_SIZE, SLOT_SIZE, null);
				}
			}
		}

		// Draw watermark at bottom
		String watermark = "All Bruised Up (ABU)";
		int watermarkY = offsetY + slotsHeight + (WATERMARK_HEIGHT / 2) - 6;
		graphics.setColor(ColorUtil.colorWithAlpha(Color.LIGHT_GRAY, 80));
		graphics.setFont(graphics.getFont().deriveFont(10f));
		int textWidth = graphics.getFontMetrics().stringWidth(watermark);
		graphics.drawString(watermark, (PANEL_WIDTH - textWidth) / 2, watermarkY);

		return new Dimension(PANEL_WIDTH, panelHeight);
	}

	/**
	 * Invokes the inventory slot action on the hidden widget.
	 * Forwards the click to the game's inventory handling.
	 * CC_OP params: param0=widgetId, param1=op (1-indexed action)
	 */
	private void invokeSlotAction(int op, int widgetId, int slot, int itemId, String option, String itemName)
	{
		client.menuAction(widgetId, op, MenuAction.CC_OP, slot, itemId, option, itemName);
	}

	/**
	 * Converts a point (relative to canvas) to the slot index under it.
	 */
	private int getSlotAtPoint(int mouseX, int mouseY, Rectangle overlayBounds)
	{
		int relX = mouseX - overlayBounds.x - PANEL_PADDING;
		int relY = mouseY - overlayBounds.y - PANEL_PADDING;

		if (relX < 0 || relY < 0)
		{
			return -1;
		}

		int col = relX / (SLOT_SIZE + SLOT_PADDING);
		int row = relY / (SLOT_SIZE + SLOT_PADDING);

		if (col >= COLS || row >= ROWS)
		{
			return -1;
		}

		// Verify we're within the slot bounds (not in padding)
		int slotX = relX % (SLOT_SIZE + SLOT_PADDING);
		int slotY = relY % (SLOT_SIZE + SLOT_PADDING);
		if (slotX >= SLOT_SIZE || slotY >= SLOT_SIZE)
		{
			return -1;
		}

		return row * COLS + col;
	}

	/**
	 * Returns true if the client is in resizable (or fullscreen) mode.
	 */
	private boolean isResizableMode()
	{
		try
		{
			int viewportMode = client.getVarcIntValue(VIEWPORT_MODE_VARC);
			return viewportMode >= 1;
		}
		catch (Exception e)
		{
			return false;
		}
	}
}
