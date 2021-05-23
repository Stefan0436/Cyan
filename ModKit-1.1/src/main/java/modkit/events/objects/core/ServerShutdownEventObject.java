package modkit.events.objects.core;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.server.MinecraftServer;

/**
 * 
 * Server shutdown event container -- contains the server
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ServerShutdownEventObject extends EventObject {
	private MinecraftServer server;
	
	public ServerShutdownEventObject(MinecraftServer server) {
		this.server = server;
	}
	
	/**
	 * Retrieves the server that is shutting down
	 */
	public MinecraftServer getServer() {
		return server;
	}
}
