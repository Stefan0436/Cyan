package org.asf.cyan.api.network.channels;

import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;
import org.asf.cyan.api.events.objects.network.ServerConnectionEventObject;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.network.ByteFlow;
import org.asf.cyan.api.network.PacketWriter;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 
 * Abstract Packet Channel
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class PacketChannel {

	private static PacketChannelContext coreContext;

	/**
	 * Packet channel namespace
	 */
	public abstract String id();

	/**
	 * Called to add processors
	 */
	public abstract void setup();

	/**
	 * Called to create instances of the channel
	 */
	protected abstract PacketChannel newInstance();

	/**
	 * Instantiates a new instance of this channel
	 * 
	 * @param context Context to use
	 */
	public PacketChannel instantiate(PacketChannelContext context) {
		PacketChannel inst = newInstance();
		inst.context = context;
		inst.setup();
		inst.side = context.getSide();
		return inst;
	}

	private GameSide side;
	private PacketChannelContext context;

	/**
	 * Retrieves a packet channel
	 * 
	 * @param id      Channel id
	 * @param context Channel context
	 */
	public static PacketChannel getChannel(String id, PacketChannelContext context) {
		if (coreContext == null) {
			coreContext = PacketChannelContext.getCore();
			coreContext.findChannels();
		}
		return coreContext.getChannel(id, context);
	}

	/**
	 * Retrieves a packet channel
	 * 
	 * @param id     Channel id
	 * @param client Game client
	 */
	public static PacketChannel getChannel(String id, Minecraft client) {
		return getChannel(id, PacketChannelContext.getForClient(client));
	}

	/**
	 * Retrieves a packet channel
	 * 
	 * @param id           Channel id
	 * @param serverPlayer Server player
	 */
	public static PacketChannel getChannel(String id, ServerPlayer serverPlayer) {
		return getChannel(id, PacketChannelContext.getForServer(serverPlayer));
	}

	/**
	 * Retrieves a packet channel
	 * 
	 * @param id               Channel id
	 * @param clientConnection Client connection object
	 */
	public static PacketChannel getChannel(String id, ClientConnectionEventObject clientConnection) {
		return getChannel(id, PacketChannelContext.getFor(clientConnection));
	}

	/**
	 * Retrieves a packet channel
	 * 
	 * @param id               Channel id
	 * @param serverConnection Server connection object
	 */
	public static PacketChannel getChannel(String id, ServerConnectionEventObject serverConnection) {
		return getChannel(id, PacketChannelContext.getFor(serverConnection));
	}

	/**
	 * Retrieves a packet channel
	 * 
	 * @param <T>     Channel type
	 * @param type    Channel class
	 * @param context Channel context
	 */
	public static <T extends PacketChannel> T getChannel(Class<T> type, PacketChannelContext context) {
		if (coreContext == null) {
			coreContext = PacketChannelContext.getCore();
			coreContext.findChannels();
		}
		return coreContext.getChannel(type, context);
	}

	/**
	 * Retrieves a packet channel
	 * 
	 * @param <T>    Channel type
	 * @param type   Channel class
	 * @param client Game client
	 */
	public static <T extends PacketChannel> T getChannel(Class<T> type, Minecraft client) {
		return getChannel(type, PacketChannelContext.getForClient(client));
	}

	/**
	 * Retrieves a packet channel
	 * 
	 * @param <T>          Channel type
	 * @param type         Channel class
	 * @param serverPlayer Server player
	 */
	public static <T extends PacketChannel> T getChannel(Class<T> type, ServerPlayer serverPlayer) {
		return getChannel(type, PacketChannelContext.getForServer(serverPlayer));
	}

	/**
	 * Retrieves a packet channel
	 * 
	 * @param <T>              Channel type
	 * @param type             Channel class
	 * @param clientConnection Client connection object
	 */
	public static <T extends PacketChannel> T getChannel(Class<T> type, ClientConnectionEventObject clientConnection) {
		return getChannel(type, PacketChannelContext.getFor(clientConnection));
	}

	/**
	 * Retrieves a packet channel
	 * 
	 * @param <T>              Channel type
	 * @param type             Channel class
	 * @param serverConnection Server connection object
	 */
	public static <T extends PacketChannel> T getChannel(Class<T> type, ServerConnectionEventObject serverConnection) {
		return getChannel(type, PacketChannelContext.getFor(serverConnection));
	}

	/**
	 * Retrieves the remote game brand (server brand if called from client, client
	 * brand if called from server)
	 */
	public String getRemoteBrand() {
		return context.getRemoteBrand();
	}

	/**
	 * Retrieves the player connected to this channel
	 */
	public Player getPlayer() {
		return context.getPlayer();
	}

	/**
	 * Retrieves the connection of this channel
	 */
	public Connection getConnection() {
		return context.getConnection();
	}

	/**
	 * Retrieves the side the channel is running on
	 */
	public GameSide getSide() {
		return side;
	}

	/**
	 * Retrieves the game world
	 */
	public Level getWorld() {
		return context.getLevel();
	}

	/**
	 * Instantiates a new packet writer
	 */
	public PacketWriter newPacket() {
		return context.newPacketWriter();
	}

	/**
	 * Sends a packet
	 * 
	 * @param id Packet id
	 */
	public void sendPacket(String id) {
		sendPacket(id(), id, null);
	}

	/**
	 * Sends a packet
	 * 
	 * @param channel Channel name
	 * @param id      Packet id
	 */
	public void sendPacket(String channel, String id) {
		sendPacket(channel, id, null);
	}

	/**
	 * Sends a packet
	 * 
	 * @param id     Packet id
	 * @param writer Packet content
	 */
	public void sendPacket(String id, PacketWriter writer) {
		sendPacket(id(), id, writer);
	}

	/**
	 * Sends a packet
	 * 
	 * @param channel Channel name
	 * @param id      Packet id
	 * @param writer  Packet content
	 */
	public void sendPacket(String channel, String id, PacketWriter writer) {
		context.sendPacket(channel, id, writer);
	}

	/**
	 * Registers a packet processor
	 * 
	 * @param <T>       Processor type
	 * @param processor Processor class
	 */
	protected <T extends AbstractPacketProcessor> void register(Class<T> processor) {
		context.registerProcessor(processor);
	}

	/**
	 * Processes a packet
	 * 
	 * @param id   Packet id
	 * @param flow Packet content flow
	 * @return True if compatible, false otherwise
	 */
	public boolean process(String id, ByteFlow flow) {
		AbstractPacketProcessor processor = context.getProcessor(id);
		if (processor == null)
			return false;

		return processor.run(this, id, flow);
	}

	/**
	 * Disconnect the client (Client side only)
	 */
	public void disconnectClient() {
		if (getSide() == GameSide.CLIENT)
			context.disconnectClient();
	}

}
