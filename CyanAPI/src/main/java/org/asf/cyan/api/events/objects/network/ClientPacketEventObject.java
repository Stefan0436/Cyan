package org.asf.cyan.api.events.objects.network;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;

/**
 * 
 * Network Event Object -- Event for all mod network packets. (client side)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ClientPacketEventObject extends EventObject {

	private ClientboundCustomPayloadPacket packet;
	private Connection connection;
	private String id;

	private String serverBrand;
	private Minecraft client;

	public ClientPacketEventObject(Connection connection, ClientboundCustomPayloadPacket packet, String brand,
			Minecraft client, String id) {
		this.packet = packet;
		this.connection = connection;
		this.packet = packet;
		this.serverBrand = brand;
		this.client = client;
		this.id = id;
	}

	/**
	 * Retrieves the packet
	 * 
	 * @return Received packet
	 */
	public ClientboundCustomPayloadPacket getPacket() {
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
	 * Retrieves the server brand
	 * 
	 * @return Server brand
	 */
	public String getServerBrand() {
		return serverBrand;
	}

	/**
	 * Retrieves the client processing the packet
	 */
	public Minecraft getClient() {
		return client;
	}

	/**
	 * Retrieves the packet id
	 */
	public String getId() {
		return id;
	}
}
