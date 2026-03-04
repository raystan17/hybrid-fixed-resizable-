package com.abu.hybridinventory;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

/**
 * Configuration for the Hybrid Inventory plugin.
 * Controls the visibility and behavior of the custom inventory panel.
 */
@ConfigGroup("hybridinventory")
public interface HybridInventoryConfig extends Config
{
	@ConfigItem(
		keyName = "enabled",
		name = "Enable Hybrid Inventory",
		description = "When enabled, hides the default inventory widget and displays a fixed-classic style panel on the right. Only active in resizable mode."
	)
	default boolean enabled()
	{
		return true;
	}
}
