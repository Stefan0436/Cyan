package org.example.examplemod.transformers;

import org.asf.cyan.api.fluid.annotations.PlatformOnly;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;

import org.apache.logging.log4j.Logger;

// This is the spigot variant of our transformer, since the method we are targeting
// is a little different in spigot, we need to use a backup transformer to have support for it.
//
// To find out why, download fernflower and decompile the 'patched' jar
// in any paper server, it is located in paper's cache folder.
//
// Search for the 'loadSpawn' method in MinecraftServer. (the name of the prepareLevels method in spigot servers)
// You might see that it has different method parameters, which breaks our transformer.
//
// @TargetClass specifies the target class
// @PlatformOnly makes sure this transformer only applies to SPIGOT platforms
@FluidTransformer
@TargetClass(target = "net.minecraft.server.MinecraftServer")
@PlatformOnly(LaunchPlatform.SPIGOT)
public abstract class SpigotExampleTransformer {

	private static Logger LOGGER = null;

	// Spigot provides the world in the method parameters.
	// We don't need the overworld method in spigot.
	@InjectAt(location = InjectLocation.TAIL)
	private void prepareLevels(
			
		@TargetType(target = "net.minecraft.server.level.progress.ChunkProgressListener") 
		ChunkProgressListener listener,
		
		@TargetType(target = "net.minecraft.server.level.ServerLevel")
		ServerLevel world
		
	) {
		BlockPos spawn = world.getSharedSpawnPos();
		String loc = world.dimension().location().toString();

		LOGGER.info("loadInitialChunks finished, World: " + loc + ", SPAWN: " + spawn.getX() + ":" + spawn.getY());
	}
}
