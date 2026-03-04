package com.abu.hybridinventory;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Development launcher for the Hybrid Inventory plugin.
 * Run this class to start RuneLite with the plugin loaded.
 */
public class HybridInventoryPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(HybridInventoryPlugin.class);
		RuneLite.main(args);
	}
}
