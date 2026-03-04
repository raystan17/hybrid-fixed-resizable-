package com.abu.hybridinventory;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.GameState;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * Hybrid Inventory Plugin - Displays a fixed-classic style inventory panel on the right
 * side of the screen in resizable mode, with the default inventory widget hidden.
 *
 * Features:
 * - 28 slots in 4x7 classic grid layout
 * - Fixed width (~241px), no stretching or scaling
 * - Anchored to right edge, repositions on window resize
 * - All slots fully clickable and functional
 * - Auto-disables in fixed mode
 */
@Slf4j
@PluginDescriptor(
	name = "Hybrid Inventory",
	description = "Displays a fixed-classic style inventory panel on the right in resizable mode"
)
public class HybridInventoryPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private HybridInventoryOverlay overlay;

	@Inject
	private HybridInventoryConfig config;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
		updateInventoryWidgetVisibility();
		log.debug("Hybrid Inventory started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		restoreInventoryWidget();
		log.debug("Hybrid Inventory stopped");
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.INVENTORY.getId())
		{
			// Overlay re-renders every frame; next frame will show updated items
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		// Viewport mode changed (fixed/resizable) - update widget visibility
		updateInventoryWidgetVisibility();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		// Update widget visibility when logging in (widget may not exist before)
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			updateInventoryWidgetVisibility();
		}
	}

	/**
	 * Hides or restores the default inventory widget based on config and viewport mode.
	 */
	private void updateInventoryWidgetVisibility()
	{
		if (!config.enabled())
		{
			restoreInventoryWidget();
			return;
		}

		// Only hide in resizable mode (viewport mode 1 or 2)
		if (isResizableMode())
		{
			hideInventoryWidget();
		}
		else
		{
			restoreInventoryWidget();
		}
	}

	/**
	 * Hides the default inventory widget.
	 */
	private void hideInventoryWidget()
	{
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		if (inventoryWidget != null)
		{
			inventoryWidget.setHidden(true);
		}
	}

	/**
	 * Restores the default inventory widget visibility.
	 */
	private void restoreInventoryWidget()
	{
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
		if (inventoryWidget != null)
		{
			inventoryWidget.setHidden(false);
		}
	}

	/**
	 * Returns true if the client is in resizable (or fullscreen) mode.
	 */
	private boolean isResizableMode()
	{
		try
		{
			int viewportMode = client.getVarcIntValue(43);
			return viewportMode >= 1;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	@Provides
	HybridInventoryConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HybridInventoryConfig.class);
	}
}
