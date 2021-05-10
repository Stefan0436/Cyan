package org.asf.cyan.api.network.channels;

import org.asf.cyan.api.modloader.information.game.GameSide;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.world.level.Level;

public abstract class ClientPacketProcessor extends AbstractPacketProcessor {

	/**
	 * Retrieves the client
	 */
	protected Minecraft getClient() {
		return PacketChannelContext.getCore().getClient(getPlayer());
	}

	/**
	 * Retrieves the player
	 */
	protected LocalPlayer getPlayer() {
		return (LocalPlayer) getChannel().getPlayer();
	}

	/**
	 * Retrieves the world
	 */
	protected Level getWorld() {
		return getChannel().getWorld();
	}

	/**
	 * Retrieves the local world
	 */
	protected Level getLocalWorld() {
		return getPlayer().clientLevel;
	}

	/**
	 * Retrieves the server connection
	 */
	protected Connection getConnection() {
		return getChannel().getConnection();
	}

	/**
	 * Retrieves the brand (mod name) of the remote server
	 */
	protected String getServerBrand() {
		return getChannel().getRemoteBrand();
	}

	/**
	 * Disconnects the client
	 */
	protected void disconnect() {
		getChannel().disconnectClient();
	}

	@Override
	protected boolean prepare(PacketChannel channel) {
		return channel.getSide() == GameSide.CLIENT;
	}

}
