package org.asf.cyan.api.network.channels;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.util.server.language.ClientLanguage;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public abstract class ServerPacketProcessor extends AbstractPacketProcessor {

	/**
	 * Retrieves the server instance
	 */
	protected MinecraftServer getServer() {
		return getChannel().getPlayer().getServer();
	}

	/**
	 * Retrieves the player
	 */
	protected ServerPlayer getPlayer() {
		return (ServerPlayer) getChannel().getPlayer();
	}

	/**
	 * Retrieves the world
	 */
	protected Level getWorld() {
		return getChannel().getWorld();
	}

	/**
	 * Retrieves the client connection
	 */
	protected Connection getConnection() {
		return getChannel().getConnection();
	}

	/**
	 * Retrieves the client brand (mod name) of the remote client
	 */
	protected String getClientBrand() {
		return getChannel().getRemoteBrand();
	}

	/**
	 * Disconnects the sender
	 * 
	 * @param languageKey Language key to use, uses fallback if not present on
	 *                    client
	 */
	protected void disconnect(String languageKey) {
		if (getChannel().getPlayer() == null)
			return;
		getChannel().getConnection().disconnect(createComponent(languageKey));
	}

	/**
	 * Creates a language or text component depending on the client presented keys
	 * 
	 * @param languageKey Language key to use, uses fallback if not present on
	 *                    client
	 */
	protected BaseComponent createComponent(String languageKey) {
		if (getChannel().getPlayer() == null)
			return null;
		return ClientLanguage.createComponent(getPlayer(), languageKey);
	}

	@Override
	protected boolean prepare(PacketChannel channel) {
		return channel.getSide() == GameSide.SERVER;
	}

}
