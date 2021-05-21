package org.asf.cyan.api.events.objects.network;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 
 * Server Connection Event Object
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 * 
 */
public class ServerConnectionEventObject extends EventObject {

	private Connection connection;
	private MinecraftServer server;
	private String clientBrand;
	private ServerPlayer player;

	public ServerConnectionEventObject(Connection connection, MinecraftServer server, ServerPlayer player,
			String brand) {
		this.connection = connection;
		this.server = server;
		this.clientBrand = brand;
		this.player = player;
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
	 * Retrieves the player
	 * 
	 * @return Packet sender
	 */
	public ServerPlayer getPlayer() {
		return player;
	}

	/**
	 * Instantiates a new client-bound custom payload packet for Cyan mods
	 * 
	 * @param id     Packet id
	 * @param buffer Packet buffer
	 * @return ClientboundCustomPayloadPacket instance
	 */
	public ClientboundCustomPayloadPacket newClientboundCyanPacket(String id, FriendlyByteBuf buffer) {
		return new ClientboundCustomPayloadPacket(new ResourceLocation("cyan", id.toLowerCase()), buffer);
	}

	/**
	 * Instantiates a new client-bound custom payload packet for Cyan mods
	 * 
	 * @param id      Packet id
	 * @param channel Packet channel (cyan by default)
	 * @param buffer  Packet buffer
	 * @return ClientboundCustomPayloadPacket instance
	 */
	public ClientboundCustomPayloadPacket newClientboundCyanPacket(String id, String channel, FriendlyByteBuf buffer) {
		return new ClientboundCustomPayloadPacket(new ResourceLocation(channel, id.toLowerCase()), buffer);
	}

	public Connection getConnection() {
		return connection;
	}

	/**
	 * Instantiates and sends ClientboundCustomPayloadPackets
	 * 
	 * @param id     Packet id
	 * @param buffer Packet buffer
	 */
	public void sendNewClientPacket(String id, FriendlyByteBuf buffer) {
		connection.send(newClientboundCyanPacket(id, buffer));
	}

	/**
	 * Instantiates and sends ClientboundCustomPayloadPackets
	 * 
	 * @param id      Packet id
	 * @param channel Packet channel (cyan by default)
	 * @param buffer  Packet buffer
	 */
	public void sendNewClientPacket(String id, String channel, FriendlyByteBuf buffer) {
		connection.send(newClientboundCyanPacket(id, channel, buffer));
	}
}
