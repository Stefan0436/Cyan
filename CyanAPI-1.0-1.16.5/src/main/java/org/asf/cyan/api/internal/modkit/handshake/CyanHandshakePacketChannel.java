package org.asf.cyan.api.internal.modkit.handshake;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;

import org.asf.cyan.api.internal.modkit.handshake.packets.HandshakeCompletionPacketProcessor;
import org.asf.cyan.api.internal.modkit.handshake.packets.HandshakeLoaderPacketProcessor;
import org.asf.cyan.api.internal.modkit.handshake.packets.HandshakeModPacketProcessor;
import org.asf.cyan.api.internal.modkit.handshake.packets.HandshakeProtocolPacketProcessor;
import org.asf.cyan.api.network.channels.PacketChannel;
import org.asf.cyan.api.versioning.Version;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class CyanHandshakePacketChannel extends PacketChannel {

	@Override
	public String id() {
		return "cyan.handshake";
	}

	@Override
	public void setup() {
		register(HandshakeProtocolPacketProcessor.class);
		register(HandshakeLoaderPacketProcessor.class);
		register(HandshakeCompletionPacketProcessor.class);
		register(HandshakeModPacketProcessor.class);
	}

	@Override
	protected PacketChannel newInstance() {
		return new CyanHandshakePacketChannel();
	}

	private static boolean runningHandler = false;
	private static HashMap<String, ClientInformation> clients = new HashMap<String, ClientInformation>();

	public static class ClientInformation {
		private String clientBrand = "Server";

		private String clientProtocol = "Not present";
		private String clientGameVersion = "Not present";
		private String clientModloaderVersion = "Not present";
		private String clientModloaderProtocol = "Not present";

		private HashMap<String, String> clientMods = new HashMap<String, String>();

		public String getBrand() {
			return clientBrand;
		}

		public String getProtocol() {
			return clientProtocol;
		}

		public String getGameVersion() {
			return clientGameVersion;
		}

		public String getModloaderVersion() {
			return clientModloaderVersion;
		}

		public String getModloaderProtocol() {
			return clientModloaderProtocol;
		}

		public Map<String, String> getMods() {
			return Map.copyOf(clientMods);
		}

	}

	public static void assignBrand(ServerPlayer player, String brand) {
		ClientInformation info = clients.getOrDefault(player.getUUID().toString(), new ClientInformation());
		info.clientBrand = brand;
		clients.put(player.getUUID().toString(), info);
	}

	public static ClientInformation getClientInfo(ServerPlayer player) {
		if (player == null)
			return new ClientInformation();
		return clients.getOrDefault(player.getUUID().toString(), new ClientInformation());
	}

	public static void startInfoHandler(MinecraftServer server) {
		if (runningHandler)
			return;
		runningHandler = true;
		new Thread(() -> {
			while (server.isRunning()) {
				while (true) {
					try {
						String[] keys = clients.keySet().toArray(t -> new String[t]);
						for (String key : keys) {
							if (!server.getPlayerList().getPlayers().stream()
									.anyMatch(t -> t.getUUID().toString().equals(key))) {
								clients.remove(key);
							}
						}
						break;
					} catch (ConcurrentModificationException | IndexOutOfBoundsException e) {
					}
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					break;
				}
				runningHandler = false;
			}
		}, "ModKit Protocol Information Server Thread").start();
	}

	public static void assignProtocol(ServerPlayer player, double protocolVersion) {
		ClientInformation info = clients.getOrDefault(player.getUUID().toString(), new ClientInformation());
		info.clientProtocol = Double.toString(protocolVersion);
		clients.put(player.getUUID().toString(), info);
	}

	public static void assignModloader(ServerPlayer player, Version gameVersion, double clientProtocol,
			Version version) {
		ClientInformation info = clients.getOrDefault(player.getUUID().toString(), new ClientInformation());
		info.clientModloaderProtocol = Double.toString(clientProtocol);
		info.clientGameVersion = gameVersion.toString();
		info.clientModloaderVersion = version.toString();
		clients.put(player.getUUID().toString(), info);
	}

	public static void assignMod(ServerPlayer player, String id, Version version) {
		ClientInformation info = clients.getOrDefault(player.getUUID().toString(), new ClientInformation());
		info.clientMods.put(id, version.toString());
		clients.put(player.getUUID().toString(), info);
	}

}
