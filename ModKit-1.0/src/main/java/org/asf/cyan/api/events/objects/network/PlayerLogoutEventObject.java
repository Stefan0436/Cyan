package org.asf.cyan.api.events.objects.network;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 
 * Server-side Logout Event Object -- ServerConnectionEventObject with the logout message
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class PlayerLogoutEventObject extends ServerConnectionEventObject {

	private Component message;

	public PlayerLogoutEventObject(Connection connection, MinecraftServer server, ServerPlayer player, String brand,
			Component message) {
		super(connection, server, player, brand);
		this.message = message;
	}

	/**
	 * Retrieves the client disconnect message
	 */
	public Component getMessage() {
		return message;
	}

}
