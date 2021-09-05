package modkit.events.objects.ingame.level;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * 
 * Server Level Load Event Object -- Contains information about the main server
 * level.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ServerLevelLoadEventObject extends EventObject {

	private MinecraftServer server;
	private ServerLevel world;
	private BlockPos spawn;
	private ResourceLocation dimensionLocation;

	public ServerLevelLoadEventObject(MinecraftServer server, ServerLevel world, BlockPos spawn,
			ResourceLocation dimensionLocation) {
		this.server = server;
		this.world = world;
		this.spawn = spawn;
		this.dimensionLocation = dimensionLocation;
	}

	/**
	 * Retrieves the server instance
	 */
	public MinecraftServer getServer() {
		return server;
	}

	/**
	 * Retrieves the loaded world
	 */
	public ServerLevel getWorld() {
		return world;
	}

	/**
	 * Retrieves the world spawn
	 */
	public BlockPos getWorldSpawn() {
		return spawn;
	}

	/**
	 * Retrieves the dimension location
	 */
	public ResourceLocation getDimensionLocation() {
		return dimensionLocation;
	}

}
