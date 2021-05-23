package org.asf.cyan.internal.modkitimpl.util;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.modloader.IPostponedComponent;
import org.asf.cyan.api.modloader.TargetModloader;
import org.asf.cyan.api.versioning.Version;

import modkit.advanced.Client;
import modkit.network.channels.ServerPacketProcessor;

@TargetModloader(CyanLoader.class)
public class ClientImpl extends Client implements IPostponedComponent {

	private Object player;

	private String playerName = null;
	private UUID playerUUID = null;

	private String clientBrand = "Not present";

	private double clientProtocol = -1;
	private Version clientGameVersion = null;

	private Version clientModloaderVersion = null;
	private double clientModloaderProtocol = -1;

	private HashMap<String, String> clientMods = new HashMap<String, String>();

	private static boolean runningHandler = false;
	private static HashMap<String, ClientImpl> clients = new HashMap<String, ClientImpl>();

	public static void assignBrand(UUID uuid, String brand) {
		ClientImpl info = clients.getOrDefault(uuid.toString(), new ClientImpl());
		info.clientBrand = brand;
		clients.put(uuid.toString(), info);
	}

	public static Client getClientInfo(Object player) {
		if (player == null)
			return new ClientImpl();
		return getForUUID(HandshakeUtils.getImpl().getUUID(player));
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
			}
			runningHandler = false;
		}, "ModKit Protocol Information Server Thread").start();
	}

	public static void assignProtocol(ServerPacketProcessor processor, double protocolVersion) {
		String uuid = HandshakeUtils.getImpl().getUUID(processor).toString();
		ClientImpl info = clients.getOrDefault(uuid, new ClientImpl());
		info.clientProtocol = protocolVersion;
		clients.put(uuid, info);
	}

	public static void assignModloader(ServerPacketProcessor processor, Version gameVersion, double clientProtocol,
			Version version) {
		String uuid = HandshakeUtils.getImpl().getUUID(processor).toString();
		ClientImpl info = clients.getOrDefault(uuid, new ClientImpl());
		info.clientModloaderProtocol = clientProtocol;
		info.clientGameVersion = gameVersion;
		info.clientModloaderVersion = version;
		clients.put(uuid, info);
	}

	public static void assignMod(ServerPacketProcessor processor, String id, Version version) {
		String uuid = HandshakeUtils.getImpl().getUUID(processor).toString();
		ClientImpl info = clients.getOrDefault(uuid, new ClientImpl());
		info.clientMods.put(id, version.toString());
		clients.put(uuid, info);
	}

	public static void assignPlayer(ServerPacketProcessor processor) {
		UUID uuid = HandshakeUtils.getImpl().getUUID(processor);
		ClientImpl info = clients.getOrDefault(uuid.toString(), new ClientImpl());
		info.playerUUID = uuid;
		info.playerName = HandshakeUtils.getImpl().getPlayerName(processor);
		info.player = HandshakeUtils.getImpl().getPlayerObject(processor);
		clients.put(uuid.toString(), info);
	}

	public static void assignPlayerObjects(String name, UUID uuid, Object player) {
		ClientImpl info = clients.getOrDefault(uuid.toString(), new ClientImpl());
		info.playerUUID = uuid;
		info.playerName = name;
		info.player = player;
		clients.put(uuid.toString(), info);
	}

	@Override
	protected Client getFor(UUID uuid) {
		if (uuid == null)
			return new ClientImpl();
		return clients.getOrDefault(uuid.toString(), new ClientImpl());
	}

	@Override
	public void initComponent() {
		provider = this;
	}

	@Override
	public String getName() {
		return playerName;
	}

	@Override
	protected Object toGameTypeImpl() {
		return player;
	}

	@Override
	public UUID getUUID() {
		return playerUUID;
	}

	@Override
	public String getBrand() {
		return clientBrand;
	}

	@Override
	public double getProtocol() {
		return clientProtocol;
	}

	@Override
	public Version getGameVersion() {
		return clientGameVersion;
	}

	@Override
	public Version getModloaderVersion() {
		return clientModloaderVersion;
	}

	@Override
	public double getModloaderProtocol() {
		return clientModloaderProtocol;
	}

	@Override
	public Map<String, String> getMods() {
		return Map.copyOf(clientMods);
	}

}
