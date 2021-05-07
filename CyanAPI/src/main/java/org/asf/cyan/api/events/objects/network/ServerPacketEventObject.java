package org.asf.cyan.api.events.objects.network;

import java.util.ArrayList;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 
 * Network Event Object -- Event for all mod network packets. (server side)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ServerPacketEventObject extends EventObject {

	private ServerboundCustomPayloadPacket packet;
	private String id;

	private Connection connection;
	private ServerPlayer player;

	private String clientBrand;
	private ArrayList<String> presentedLanguageKeys;

	public void defineClientLanguageKey(String key) {
		if (!presentedLanguageKeys.contains(key))
			presentedLanguageKeys.add(key);
	}

	private MinecraftServer server;

	public ServerPacketEventObject(Connection connection, ServerboundCustomPayloadPacket packet, ServerPlayer player,
			String brand, MinecraftServer server, String id) {
		this.packet = packet;
		this.connection = connection;
		this.player = player;
		this.clientBrand = brand;
		this.server = server;
		this.presentedLanguageKeys = new ArrayList<String>();
		this.id = id;
	}

	/**
	 * Disconnects the sender
	 * 
	 * @param languageKey Language key to use, uses fallback if not possible
	 * @param fallback    Fallback message to use if language is unavailable
	 */
	public void disconnectPlayer(String languageKey, String fallback) {
		if (player == null)
			return;
		if (clientBrand == null || !clientBrand.startsWith("Cyan") || !presentedLanguageKeys.contains(languageKey)) {
			player.connection.disconnect(new TextComponent(fallback));
		} else {
			player.connection.disconnect(new TranslatableComponent(languageKey));
		}
	}

	/**
	 * Retrieves the player
	 * 
	 * @return Packet sender
	 */
	public ServerPlayer getPlayer() {
		return player;
	}

	/**
	 * Retrieves the packet
	 * 
	 * @return Received packet
	 */
	public ServerboundCustomPayloadPacket getPacket() {
		return packet;
	}

	/**
	 * Retrieves the client connection
	 * 
	 * @return Client connection
	 */
	public Connection getConnection() {
		return connection;
	}

	/**
	 * Retrieves the server instance
	 * 
	 * @return Game server instance
	 */
	public MinecraftServer getServer() {
		return server;
	}

	/**
	 * Retrieves the client brand
	 * 
	 * @return Client brand
	 */
	public String getClientBrand() {
		return clientBrand;
	}
	
	/**
	 * Retrieves the packet id
	 */
	public String getId() {
		return id;
	}
}
