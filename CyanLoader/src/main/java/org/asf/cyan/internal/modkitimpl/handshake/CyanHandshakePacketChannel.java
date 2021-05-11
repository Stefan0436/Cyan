package org.asf.cyan.internal.modkitimpl.handshake;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.asf.cyan.api.network.channels.PacketChannel;
import org.asf.cyan.api.network.channels.ServerPacketProcessor;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.internal.modkitimpl.handshake.packets.processors.HandshakeCompletionPacketProcessor;
import org.asf.cyan.internal.modkitimpl.handshake.packets.processors.HandshakeLoaderPacketProcessor;
import org.asf.cyan.internal.modkitimpl.handshake.packets.processors.HandshakeModPacketProcessor;
import org.asf.cyan.internal.modkitimpl.handshake.packets.processors.HandshakeProtocolPacketProcessor;
import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;

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

	public static void assignBrand(UUID uuid, String brand) {
		ClientInformation info = clients.getOrDefault(uuid.toString(), new ClientInformation());
		info.clientBrand = brand;
		clients.put(uuid.toString(), info);
	}

	public static ClientInformation getClientInfo(Object player) {
		if (player == null)
			return new ClientInformation();
		return clients.getOrDefault(HandshakeUtils.getImpl().getUUID(player).toString(), new ClientInformation());
	}

	public static void startInfoHandler(ServerPacketProcessor processor) {
		if (runningHandler)
			return;
		runningHandler = true;
		new Thread(() -> {
			while (HandshakeUtils.getImpl().isServerRunning(processor)) {
				while (true) {
					try {
						String[] keys = clients.keySet().toArray(t -> new String[t]);
						for (String key : keys) {
							if (!HandshakeUtils.getImpl().isUUIDPresentInPlayerList(processor, key)) {
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

	public static void assignProtocol(ServerPacketProcessor processor, double protocolVersion) {
		String uuid = HandshakeUtils.getImpl().getUUID(processor).toString();
		ClientInformation info = clients.getOrDefault(uuid, new ClientInformation());
		info.clientProtocol = Double.toString(protocolVersion);
		clients.put(uuid, info);
	}

	public static void assignModloader(ServerPacketProcessor processor, Version gameVersion, double clientProtocol, Version version) {
		String uuid = HandshakeUtils.getImpl().getUUID(processor).toString();
		ClientInformation info = clients.getOrDefault(uuid, new ClientInformation());
		info.clientModloaderProtocol = Double.toString(clientProtocol);
		info.clientGameVersion = gameVersion.toString();
		info.clientModloaderVersion = version.toString();
		clients.put(uuid, info);
	}

	public static void assignMod(ServerPacketProcessor processor, String id, Version version) {
		String uuid = HandshakeUtils.getImpl().getUUID(processor).toString();
		ClientInformation info = clients.getOrDefault(uuid, new ClientInformation());
		info.clientMods.put(id, version.toString());
		clients.put(uuid, info);
	}

}
