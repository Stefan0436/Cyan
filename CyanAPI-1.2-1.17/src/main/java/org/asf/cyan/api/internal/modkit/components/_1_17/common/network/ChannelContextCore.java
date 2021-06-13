package org.asf.cyan.api.internal.modkit.components._1_17.common.network;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.function.Supplier;

import org.asf.cyan.api.internal.ClientPacketListenerExtension;
import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.internal.ServerGamePacketListenerExtension;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.core.CyanCore;

import modkit.events.objects.network.ClientConnectionEventObject;
import modkit.events.objects.network.ServerConnectionEventObject;
import modkit.network.PacketWriter;
import modkit.network.channels.AbstractPacketProcessor;
import modkit.network.channels.PacketChannel;
import modkit.network.channels.PacketChannelContext;
import modkit.network.channels.PacketChannelList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
		ClassLoader loader = getClass().getClassLoader();
		channels = new PacketChannelList();
		info("Finding mod network channels...");
		Class<? extends PacketChannel>[] classes = findClasses(getMainImplementation(), PacketChannel.class, loader,
				CyanCore.getClassLoader(), CyanCore.getCoreClassLoader());
		for (Class<? extends PacketChannel> channel : classes) {
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
	protected PacketChannel[] getChannels(String id, PacketChannelContext context) {
		ArrayList<PacketChannel> matchingChannels = new ArrayList<PacketChannel>();
		for (PacketChannel ch : channels) {
			if (ch.id().equals(id))
				matchingChannels.add(ch.instantiate(context));
		}
		return matchingChannels.toArray(t -> new PacketChannel[t]);
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
	protected PacketChannelContext setupNew(Supplier<Minecraft> clientSupplier) {
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

	@Override
	protected Supplier<Minecraft> getClientSupplier(Object connection) {
		return () -> ((ClientPacketListenerExtension) ((LocalPlayer) connection).connection).cyanGetMinecraft();
	}

	@Override
	public String getClientBrand(ServerPlayer player) {
		return ((ServerGamePacketListenerExtension) player.connection).cyanClientBrand();
	}
}
