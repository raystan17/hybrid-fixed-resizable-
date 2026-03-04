package com.snoop;

import com.snoop.config.BackgroundMode;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

/**
 * Overlay for the right-side panel: background, gap borders, transparency warning, and watermark.
 */
@Slf4j
public class FixedResizableBySnoopOverlay extends Overlay
{
	private static final int OVERLAY_WIDTH = 249;

	// Watermark: shown on all background options, in empty spacer space
	private static final String WATERMARK_TEXT = "All Bruised Up (ABU) — by Snoop";
	private static final float WATERMARK_FONT_SIZE = 14f; // ~14px
	private static final int WATERMARK_ALPHA = 70; // ~25-30% opacity, subtle but readable

	private final Client client;
	private final FixedResizableBySnoopConfig config;

	private static final java.awt.Image GAP_BORDER =
		ImageUtil.loadImageResource(FixedResizableBySnoopPlugin.class, "/border15px.png");
	private static final java.awt.Image TRANSPARENCY_WARNING =
		ImageUtil.loadImageResource(FixedResizableBySnoopPlugin.class, "/transparencyWarning.png");
	private static final BufferedImage TILABLE_BACKGROUND =
		ImageUtil.loadImageResource(FixedResizableBySnoopPlugin.class, "/tilable_background.png");

	private volatile BufferedImage customImage;
	private volatile String lastCustomImagePath;

	private BufferedImage backgroundCache;
	private int lastClientHeight = -1;
	private BackgroundMode lastBackgroundMode;
	private Color lastBackgroundColor;

	@Inject
	public FixedResizableBySnoopOverlay(Client client, FixedResizableBySnoopConfig config, FixedResizableBySnoopPlugin plugin)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.UNDER_WIDGETS);
		updateCustomImage(config.customImagePath());
	}

	public void updateCustomImage(String path)
	{
		if (path == null || path.isEmpty())
		{
			customImage = null;
			lastCustomImagePath = null;
			invalidateCache();
			return;
		}
		if (path.equals(lastCustomImagePath))
		{
			return;
		}
		try
		{
			File imageFile = new File(path);
			if (imageFile.exists())
			{
				customImage = ImageIO.read(imageFile);
				lastCustomImagePath = path;
				invalidateCache();
			}
			else
			{
				customImage = null;
				lastCustomImagePath = null;
				invalidateCache();
				log.warn("Custom background image file not found at path: {}", path);
			}
		}
		catch (IOException e)
		{
			log.error("Failed to load custom background image", e);
			customImage = null;
			lastCustomImagePath = null;
			invalidateCache();
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Dimension clientDimensions = client.getRealDimensions();
		int clientWidth = (int) clientDimensions.getWidth();
		int clientHeight = (int) clientDimensions.getHeight();
		Rectangle overlayBounds = new Rectangle(clientWidth - OVERLAY_WIDTH, 0, OVERLAY_WIDTH, clientHeight);

		Widget inventoryWidget = client.getWidget(InterfaceID.ToplevelOsrsStretch.SIDE_MENU);
		Widget minimapWidget = client.getWidget(InterfaceID.Orbs.UNIVERSE);

		// 1) Background
		updateBackgroundCache(overlayBounds);
		if (backgroundCache != null)
		{
			graphics.drawImage(backgroundCache, overlayBounds.x, overlayBounds.y, null);
		}

		// 2) Gap borders (optional)
		if (config.useGapBorders())
		{
			if (inventoryWidget != null)
			{
				int borderX = inventoryWidget.getCanvasLocation().getX();
				int borderY = inventoryWidget.getCanvasLocation().getY() - 15;
				graphics.drawImage(GAP_BORDER, borderX, borderY, null);
			}
			if (minimapWidget != null)
			{
				int borderX = minimapWidget.getCanvasLocation().getX();
				int borderY = minimapWidget.getCanvasLocation().getY() + 158;
				graphics.drawImage(GAP_BORDER, borderX, borderY, null);
			}
		}

		// 3) Inventory transparency warning
		if (config.invBackgroundWarning() && TRANSPARENCY_WARNING != null && inventoryWidget != null && !inventoryWidget.isHidden())
		{
			int invX = inventoryWidget.getCanvasLocation().getX();
			int invY = inventoryWidget.getCanvasLocation().getY();
			int invWidth = inventoryWidget.getWidth();
			int invHeight = inventoryWidget.getHeight();
			Rectangle inventoryBounds = new Rectangle(invX, invY, invWidth, invHeight);
			Rectangle paintBounds = inventoryBounds.intersection(overlayBounds);
			if (!paintBounds.isEmpty())
			{
				Shape oldClip = graphics.getClip();
				graphics.setClip(paintBounds);
				graphics.drawImage(TRANSPARENCY_WARNING, invX, invY, null);
				graphics.setClip(oldClip);
			}
		}

		// 4) Global tint over the column
		Color tint = config.gapBackgroundTint();
		if (tint.getAlpha() > 0)
		{
			Composite oldComposite = graphics.getComposite();
			graphics.setComposite(AlphaComposite.SrcAtop);
			graphics.setColor(tint);
			graphics.fillRect(overlayBounds.x, overlayBounds.y, overlayBounds.width, overlayBounds.height);
			graphics.setComposite(oldComposite);
		}

		// 5) Watermark in gap/spacer area - shown on all background options
		drawWatermark(graphics, overlayBounds, inventoryWidget, minimapWidget);

		// 6) ABU branded: large "ABU" text in gap (when that mode is selected)
		if (config.backgroundMode() == BackgroundMode.ABU_BRANDED)
		{
			drawAbuBranding(graphics, overlayBounds, inventoryWidget, minimapWidget);
		}

		return overlayBounds.getSize();
	}

	/**
	 * Draws ABU logo/branding at 70% of spacer height, centered horizontally.
	 * Fully readable but subtle. Only when ABU Branded mode is selected.
	 */
	private void drawAbuBranding(Graphics2D graphics, Rectangle overlayBounds, Widget inventoryWidget, Widget minimapWidget)
	{
		int gapTop = minimapWidget != null
			? minimapWidget.getCanvasLocation().getY() + minimapWidget.getHeight()
			: 207;
		int gapBottom = (inventoryWidget != null && !inventoryWidget.isHidden())
			? inventoryWidget.getCanvasLocation().getY() - overlayBounds.y
			: 230;
		int gapHeight = gapBottom - gapTop;
		if (gapHeight < 18) return;

		// ABU at 70% of spacer height - scale font to fill ~70% of gap
		float abuFontSize = Math.max(18f, Math.min(42f, gapHeight * 0.7f));
		graphics.setFont(graphics.getFont().deriveFont(abuFontSize));
		graphics.setColor(ColorUtil.colorWithAlpha(new Color(215, 200, 180), 65)); // Readable but subtle
		String abuText = "ABU";
		int abuW = graphics.getFontMetrics().stringWidth(abuText);
		int abuX = overlayBounds.x + (OVERLAY_WIDTH - abuW) / 2;
		int abuY = overlayBounds.y + gapTop + (gapHeight / 2) + (int)(abuFontSize * 0.35); // Centered vertically
		graphics.drawString(abuText, abuX, abuY);
	}

	/**
	 * Draws watermark in the empty spacer area (above inventory, below minimap).
	 * Shown on all background options. ~14px font, 25-30% opacity, subtle but readable.
	 */
	private void drawWatermark(Graphics2D graphics, Rectangle overlayBounds, Widget inventoryWidget, Widget minimapWidget)
	{
		// Gap is between minimap bottom (~207px) and inventory top
		int gapTop = 0;
		int gapBottom = overlayBounds.height;

		if (minimapWidget != null)
		{
			int minimapY = minimapWidget.getCanvasLocation().getY();
			int minimapH = minimapWidget.getHeight();
			gapTop = minimapY + minimapH;
		}
		else
		{
			gapTop = 207; // Default minimap height
		}

		if (inventoryWidget != null && !inventoryWidget.isHidden())
		{
			int invY = inventoryWidget.getCanvasLocation().getY();
			gapBottom = invY - overlayBounds.y;
		}
		else
		{
			gapBottom = 230; // Fallback
		}

		int gapHeight = gapBottom - gapTop;
		if (gapHeight < 12)
		{
			return; // Not enough space, skip watermark
		}

		// When ABU branded, place watermark below ABU branding to avoid overlap
		int watermarkOffset = (config.backgroundMode() == BackgroundMode.ABU_BRANDED)
			? (gapHeight * 3) / 4  // Lower part of gap
			: gapHeight / 2;       // Center for other modes

		graphics.setFont(graphics.getFont().deriveFont(WATERMARK_FONT_SIZE));
		graphics.setColor(ColorUtil.colorWithAlpha(Color.LIGHT_GRAY, WATERMARK_ALPHA)); // ~25-30% opacity
		int textWidth = graphics.getFontMetrics().stringWidth(WATERMARK_TEXT);
		int watermarkX = overlayBounds.x + (OVERLAY_WIDTH - textWidth) / 2;
		int watermarkY = overlayBounds.y + gapTop + watermarkOffset + 5;
		graphics.drawString(WATERMARK_TEXT, watermarkX, watermarkY);
	}

	private void updateBackgroundCache(Rectangle overlayBounds)
	{
		BackgroundMode currentMode = config.backgroundMode();
		Color currentBgColor = config.backgroundColor();
		int currentHeight = overlayBounds.height;

		if (backgroundCache != null && currentHeight == lastClientHeight
			&& currentMode == lastBackgroundMode && currentBgColor.equals(lastBackgroundColor))
		{
			return;
		}

		lastClientHeight = currentHeight;
		lastBackgroundMode = currentMode;
		lastBackgroundColor = currentBgColor;

		backgroundCache = new BufferedImage(overlayBounds.width, overlayBounds.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = backgroundCache.createGraphics();
		try
		{
			switch (currentMode)
			{
				case DEFAULT_CLEAN:
					drawDefaultClean(g, overlayBounds.width, overlayBounds.height);
					break;
				case ABU_BRANDED:
					drawAbuBranded(g, overlayBounds.width, overlayBounds.height);
					break;
				case TILED_CUSTOM_IMAGE:
					if (customImage != null)
					{
						drawTiledImage(g, overlayBounds.width, overlayBounds.height, customImage);
					}
					else
					{
						g.setColor(currentBgColor);
						g.fillRect(0, 0, overlayBounds.width, overlayBounds.height);
					}
					break;
				case TILED_STONE:
					if (TILABLE_BACKGROUND != null)
					{
						drawTiledImage(g, overlayBounds.width, overlayBounds.height, TILABLE_BACKGROUND);
					}
					else
					{
						g.setColor(currentBgColor);
						g.fillRect(0, 0, overlayBounds.width, overlayBounds.height);
					}
					break;
				case SOLID_COLOR:
				default:
					g.setColor(currentBgColor);
					g.fillRect(0, 0, overlayBounds.width, overlayBounds.height);
					break;
			}
		}
		finally
		{
			g.dispose();
		}
	}

	/** Default modern textured gradient: #2F2F2F top to #3A3A3A bottom, faint diagonal lines at 10% opacity. */
	private void drawDefaultClean(Graphics2D g, int width, int height)
	{
		// Gradient from dark gray #2F2F2F at top to slightly lighter #3A3A3A at bottom
		GradientPaint gradient = new GradientPaint(
			0, 0, new Color(0x2F, 0x2F, 0x2F),
			0, height, new Color(0x3A, 0x3A, 0x3A)
		);
		g.setPaint(gradient);
		g.fillRect(0, 0, width, height);
		// Faint diagonal lines every 8-10px at 10% opacity for depth
		g.setColor(new Color(255, 255, 255, 26));
		int spacing = 9;
		for (int i = -height; i < width + height; i += spacing)
		{
			g.drawLine(i, -height, i + height, height * 2);
		}
	}

	/** ABU branded: same modern gradient base with amber accent. (ABU logo drawn in gap during render.) */
	private void drawAbuBranded(Graphics2D g, int width, int height)
	{
		// Base gradient #2F2F2F to #3A3A3A (matches default)
		GradientPaint gradient = new GradientPaint(
			0, 0, new Color(0x2F, 0x2F, 0x2F),
			0, height, new Color(0x3A, 0x3A, 0x3A)
		);
		g.setPaint(gradient);
		g.fillRect(0, 0, width, height);
		// Muted amber overlay for branded feel
		g.setColor(new Color(55, 45, 40, 35));
		g.fillRect(0, 0, width, height);
		// Faint diagonal lines at 10% opacity
		g.setColor(new Color(255, 255, 255, 26));
		int spacing = 9;
		for (int i = -height; i < width + height; i += spacing)
		{
			g.drawLine(i, -height, i + height, height * 2);
		}
		// Thin accent line for polish
		g.setColor(new Color(90, 70, 58, 70));
		g.fillRect(0, 0, width, 2);
	}

	private void drawTiledImage(Graphics2D g, int width, int height, BufferedImage image)
	{
		int imageH = image.getHeight();
		if (imageH <= 0) return;
		for (int y = 0; y < height; y += imageH)
		{
			g.drawImage(image, 0, y, width, imageH, null);
		}
	}

	private void invalidateCache()
	{
		lastClientHeight = -1;
	}
}
