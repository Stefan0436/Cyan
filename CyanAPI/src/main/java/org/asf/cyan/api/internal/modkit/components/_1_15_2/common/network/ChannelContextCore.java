package org.asf.cyan.api.internal.modkit.components._1_15_2.common.network;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;
import org.asf.cyan.api.events.objects.network.ServerConnectionEventObject;
import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.network.PacketWriter;
import org.asf.cyan.api.network.channels.AbstractPacketProcessor;
import org.asf.cyan.api.network.channels.PacketChannel;
import org.asf.cyan.api.network.channels.PacketChannelContext;
import org.asf.cyan.api.network.channels.PacketChannelList;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ChannelContextCore extends PacketChannelContext implements IModKitComponent {

	private PacketChannelList channels;

	@Override
	public void initializeComponent() {
		coreImplementation = this;
	}

	@Override
	protected void findChannels() {
		channels = new PacketChannelList();
		info("Finding mod network channels...");
		for (Class<? extends PacketChannel> channel : findClasses(getMainImplementation(), PacketChannel.class)) {
			if (!Modifier.isAbstract(channel.getModifiers())) {
				info("Loading channel " + channel.getTypeName() + "...");
				try {
					Constructor<? extends PacketChannel> constr = channel.getDeclaredConstructor();
					constr.setAccessible(true);
					PacketChannel inst = constr.newInstance();
					channels.add(inst);
				} catch (InvocationTargetException | NoSuchMethodException | SecurityException | InstantiationException
						| IllegalAccessException | IllegalArgumentException e) {
					throw new RuntimeException("Channel could not be instantiated", e);
				}
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T extends PacketChannel> T getChannel(Class<T> type, PacketChannelContext context) {
		for (PacketChannel ch : channels) {
			if (ch.getClass().getTypeName().equals(type.getTypeName()))
				return (T) ch.instantiate(context);
		}
		for (PacketChannel ch : channels) {
			if (type.isAssignableFrom(ch.getClass()))
				return (T) ch.instantiate(context);
		}
		return null;
	}

	@Override
	protected PacketChannel getChannel(String id, PacketChannelContext context) {
		for (PacketChannel ch : channels) {
			if (ch.id().equals(id))
				return ch.instantiate(context);
		}
		return null;
	}

	@Override
	protected PacketChannelContext setupNew(ServerConnectionEventObject serverConnection) {
		return null;
	}

	@Override
	protected PacketChannelContext setupNew(ClientConnectionEventObject clientConnection) {
		return null;
	}

	@Override
	protected PacketChannelContext setupNew(Minecraft client) {
		return null;
	}

	@Override
	protected PacketChannelContext setupNew(ServerPlayer server) {
		return null;
	}

	@Override
	protected Connection getConnection() {
		return null;
	}

	@Override
	protected Player getPlayer() {
		return null;
	}

	@Override
	protected String getRemoteBrand() {
		return null;
	}

	@Override
	protected GameSide getSide() {
		return null;
	}

	@Override
	protected Level getLevel() {
		return null;
	}

	@Override
	protected void sendPacket(String channel, String id, PacketWriter writer) {
	}

	@Override
	protected PacketWriter newPacketWriter() {
		return null;
	}

	@Override
	protected <T extends AbstractPacketProcessor> void registerProcessor(Class<T> processor) {
	}

	@Override
	protected AbstractPacketProcessor getProcessor(String id) {
		return null;
	}

}
