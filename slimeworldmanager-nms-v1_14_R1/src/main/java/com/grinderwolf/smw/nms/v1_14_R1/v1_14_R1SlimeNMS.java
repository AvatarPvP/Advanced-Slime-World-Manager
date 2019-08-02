package com.grinderwolf.smw.nms.v1_14_R1;

import com.grinderwolf.smw.api.world.SlimeWorld;
import com.grinderwolf.smw.crlfixer.CRLFixer;
import com.grinderwolf.smw.nms.CraftSlimeWorld;
import com.grinderwolf.smw.nms.SlimeNMS;
import lombok.Getter;
import net.minecraft.server.v1_14_R1.DimensionManager;
import net.minecraft.server.v1_14_R1.MinecraftServer;
import net.minecraft.server.v1_14_R1.WorldServer;
import net.minecraft.server.v1_14_R1.WorldSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class v1_14_R1SlimeNMS implements SlimeNMS {

    private static final Logger LOGGER = LogManager.getLogger("SMW");

    public v1_14_R1SlimeNMS() {
        CRLFixer.setLoader(new CustomChunkLoader());
    }

    @Getter
    private final boolean v1_13WorldFormat = true;

    @Override
    public void generateWorld(SlimeWorld world) {
        String worldName = world.getName();

        if (Bukkit.getWorld(worldName) != null) {
            throw new IllegalArgumentException("World " + worldName + " already exists! Maybe it's an outdated SlimeWorld object?");
        }

        LOGGER.info("Loading world " + world.getName());
        long startTime = System.currentTimeMillis();

        CustomNBTStorage dataManager = new CustomNBTStorage(world);
        MinecraftServer mcServer = MinecraftServer.getServer();
        int dimension = CraftWorld.CUSTOM_DIMENSION_OFFSET + mcServer.worldServer.size();

        for (WorldServer server : mcServer.getWorlds()) {
            if (server.getWorldProvider().getDimensionManager().getDimensionID() == dimension) {
                dimension++;
            }
        }

        DimensionManager actualDimension = DimensionManager.a(0);
        DimensionManager dimensionManager = DimensionManager.register(worldName, new DimensionManager(dimension, actualDimension.getSuffix(),
                actualDimension.folder, actualDimension.providerFactory::apply, actualDimension.hasSkyLight(), actualDimension));
        WorldServer server = new CustomWorldServer((CraftSlimeWorld) world, dataManager, dimensionManager);

        mcServer.initWorld(server, dataManager.getWorldData(), new WorldSettings(dataManager.getWorldData()));

        Bukkit.getPluginManager().callEvent(new WorldInitEvent(server.getWorld()));
        MinecraftServer.getServer().loadSpawn(server.getChunkProvider().playerChunkMap.worldLoadListener, server);
        Bukkit.getPluginManager().callEvent(new WorldLoadEvent(server.getWorld()));

        LOGGER.info("World " + world.getName() + " loaded in " + (System.currentTimeMillis() - startTime) + "ms.");
    }

    @Override
    public SlimeWorld getSlimeWorld(World world) {
        CraftWorld craftWorld = (CraftWorld) world;

        if (!(craftWorld.getHandle() instanceof CustomWorldServer)) {
            return null;
        }

        CustomWorldServer worldServer = (CustomWorldServer) craftWorld.getHandle();

        return worldServer.getSlimeWorld();
    }
}