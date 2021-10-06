package modkit.events.objects.core;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.server.MinecraftServer;

/**
 * 
 * Server event container -- contains the server
 * 
 * @since ModKit 1.0, renamed in 1.3
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ServerEventObject extends EventObject {
	private MinecraftServer server;

	public ServerEventObject(MinecraftServer server) {
		this.server = server;
	}

	/**
	 * Retrieves the server that is shutting down
	 */
	public MinecraftServer getServer() {
		return server;
	}
}
