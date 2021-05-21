package org.asf.cyan.internal.modkitimpl.util;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.UUID;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;
import org.asf.cyan.api.network.channels.ClientPacketProcessor;
import org.asf.cyan.api.network.channels.PacketChannel;
import org.asf.cyan.api.network.channels.ServerPacketProcessor;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeFailedPacket;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeLoaderPacket;

import com.google.gson.JsonObject;

public abstract class HandshakeUtils extends CyanComponent {

	protected static HandshakeUtils impl;

	public static HandshakeUtils getImpl() {
		return impl;
	}

	public abstract UUID getUUID(ServerPacketProcessor processor);

	public abstract UUID getUUID(Object player);

	public abstract boolean isServerRunning(ServerPacketProcessor processor);

	public abstract boolean isUUIDPresentInPlayerList(ServerPacketProcessor processor, String key);

	public abstract <T extends PacketChannel> T getChannel(Class<T> type, ClientConnectionEventObject event);

	public abstract void dispatchConnectionEvent(ClientPacketProcessor processor);

	public abstract void disconnectSimple(ServerPacketProcessor processor, String lang, Object... args);

	public abstract void disconnect(ClientPacketProcessor processor, HandshakeFailedPacket response,
			HandshakeLoaderPacket packet);

	public abstract void logWarnModsClientOnly(ServerPacketProcessor processor, HashMap<String, String> mods,
			String modsPretty);

	public abstract void logWarnModsServerOnly(ServerPacketProcessor processor, HashMap<String, String> mods,
			String modsPretty);

	public abstract void logWarnModsBothSides(ServerPacketProcessor processor, HashMap<String, String> mods1,
			HashMap<String, String> mods2, String modsPretty1, String modsPretty2);

	public abstract void disconnectColored1(ServerPacketProcessor processor, HandshakeFailedPacket response,
			double protocol);

	public abstract void switchStateConnected(ServerPacketProcessor processor, boolean state);

	public abstract void dispatchFinishEvent(ServerPacketProcessor processor);

	public abstract String getPlayerName(ServerPacketProcessor processor);

	public abstract Object getPlayerObject(ServerPacketProcessor processor);

	public abstract void onSerializeJson(JsonObject data);

}
