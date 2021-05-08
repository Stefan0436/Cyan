package org.asf.cyan.api.network.channels;

import java.util.ArrayList;

import org.asf.cyan.api.modloader.information.game.GameSide;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public abstract class ServerPacketProcessor extends AbstractPacketProcessor {

	private static ArrayList<String> presentedLanguageKeys = new ArrayList<String>();

	public static void registerLanguageKey(String key) {
		if (!presentedLanguageKeys.contains(key))
			presentedLanguageKeys.add(key);
	}

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
	 * @param languageKey Language key to use, uses fallback if not possible
	 * @param fallback    Fallback message to use if language is unavailable
	 */
	protected void disconnect(String languageKey, String fallback) {
		if (getChannel().getPlayer() == null)
			return;
		if (getChannel().getRemoteBrand() == null || !getChannel().getRemoteBrand().startsWith("Cyan")
				|| !presentedLanguageKeys.contains(languageKey)) {
			getChannel().getConnection().disconnect(new TextComponent(fallback));
		} else {
			getChannel().getConnection().disconnect(new TranslatableComponent(languageKey));
		}
	}

	@Override
	protected boolean prepare(PacketChannel channel) {
		return channel.getSide() == GameSide.SERVER;
	}

}
