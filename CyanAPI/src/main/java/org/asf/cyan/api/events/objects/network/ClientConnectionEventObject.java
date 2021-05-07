package org.asf.cyan.api.events.objects.network;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;

/**
 * 
 * Client Connection Event Object
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 * 
 */
public class ClientConnectionEventObject extends EventObject {
	private Connection connection;
	private Minecraft client;
	private String serverBrand;

	public ClientConnectionEventObject(Connection connection, Minecraft client, String brand) {
		this.connection = connection;
		this.client = client;
		this.serverBrand = brand;
	}

	/**
	 * Retrieves the server brand
	 * 
	 * @return Server brand
	 */
	public String getServerBrand() {
		return serverBrand;
	}

	/**
	 * Retrieves the game client
	 */
	public Minecraft getClient() {
		return client;
	}

	/**
	 * Instantiates a new server-bound custom payload packet for Cyan mods
	 * 
	 * @param id     Packet id
	 * @param buffer Packet buffer
	 * @return ServerboundCustomPayloadPacket instance
	 */
	public ServerboundCustomPayloadPacket newServerboundCyanPacket(String id, FriendlyByteBuf buffer) {
		return new ServerboundCustomPayloadPacket(new ResourceLocation("cyan." + id.toLowerCase()), buffer);
	}

	public Connection getConnection() {
		return connection;
	}

	/**
	 * Instantiates and sends ServerboundCustomPayloadPackets
	 * 
	 * @param id     Packet id
	 * @param buffer Packet buffer
	 */
	public void sendNewServerPacket(String id, FriendlyByteBuf buffer) {
		connection.send(newServerboundCyanPacket(id, buffer));
	}
}
