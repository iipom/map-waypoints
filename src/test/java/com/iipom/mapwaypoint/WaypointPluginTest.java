package com.iipom.mapwaypoint;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class WaypointPluginTest {
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(MapWaypointPlugin.class);
        RuneLite.main(args);
    }
}
