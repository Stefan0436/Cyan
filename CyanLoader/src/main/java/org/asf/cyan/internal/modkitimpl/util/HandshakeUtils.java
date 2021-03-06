package org.asf.cyan.internal.modkitimpl.util;

import java.util.HashMap;
import java.util.UUID;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeFailedPacket;

import com.google.gson.JsonObject;

import modkit.network.channels.ClientPacketProcessor;
import modkit.network.channels.PacketChannel;
import modkit.network.channels.ServerPacketProcessor;
import modkit.util.client.ServerSoftware;

public abstract class HandshakeUtils extends CyanComponent {

	protected static HandshakeUtils impl;

	public static HandshakeUtils getImpl() {
		return impl;
	}

	public abstract UUID getUUID(ServerPacketProcessor processor);

	public abstract UUID getUUID(Object player);

	public ServerSoftware getSoftware() {
		return null;
	}

	public abstract boolean isServerRunning(ServerPacketProcessor processor);

	public abstract boolean isUUIDPresentInPlayerList(ServerPacketProcessor processor, String key);

	public abstract <T extends PacketChannel> T getChannel(Class<T> type, Object client);

	public abstract void dispatchConnectionEvent(ClientPacketProcessor processor);

	public abstract void disconnectSimple(ServerPacketProcessor processor, String lang, Object... args);

	public abstract void disconnect(ClientPacketProcessor processor, HandshakeFailedPacket response, String version);

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

	public abstract boolean beginHandshake(Object client, Object parent, String ip, int port);

	public abstract int validateModKitProtocol(double serverProtocol, double serverMinProtocol,
			double serverMaxProtocol, double clientProtocol, double clientMinProtocol, double clientMaxProtocol);

	public abstract int validateLoaderProtocol(double loaderProtocol, double loaderMinProtocol,
			double loaderMaxProtocol, double clientProtocol, double clientMinProtocol, double clientMaxProtocol);

	public abstract boolean handhakeCheck(JsonObject cyanGetServerData);

	public abstract void disconnect(Object player, String message);

	public abstract void disconnect(Object player, String message, Object[] args);

	public abstract boolean isInGame();

	public abstract String getServerBrand();

	public abstract void reopenLevelScreen();

	public abstract void closeLevelScreen();

}
