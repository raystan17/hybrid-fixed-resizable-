package com.snoop;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Development launcher for Fixed Resizable by Snoop plugin.
 * Run this class to start RuneLite with the plugin loaded.
 */
public class FixedResizableBySnoopTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FixedResizableBySnoopPlugin.class);
		RuneLite.main(args);
	}
}
