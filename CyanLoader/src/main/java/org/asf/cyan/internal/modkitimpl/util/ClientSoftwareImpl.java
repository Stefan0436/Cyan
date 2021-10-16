package org.asf.cyan.internal.modkitimpl.util;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.modloader.IPostponedComponent;
import org.asf.cyan.api.modloader.TargetModloader;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeFinishedPacket;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeLoaderListPacket;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeLoaderListPacket.Entry;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeLoaderListPacket.Mod;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeLoaderListPacket.ModloaderData;

import modkit.network.channels.ServerPacketProcessor;
import modkit.protocol.ModKitModloader.ModKitProtocolRules;
import modkit.util.Colors;
import modkit.util.remotedata.RemoteModloader;
import modkit.util.server.ClientSoftware;

@TargetModloader(CyanLoader.class)
public class ClientSoftwareImpl extends ClientSoftware implements IPostponedComponent {

	private boolean hasHandshaked = false;
	private Object player;

	public boolean hasHandshaked() {
		return hasHandshaked;
	}

	private String playerName = null;
	private UUID playerUUID = null;

	private String clientBrand = "Not present";

	private double clientProtocol = -1;
	private double clientProtocolMin = -1;
	private double clientProtocolMax = -1;

	private RemoteModloader[] loaders = new RemoteModloader[0];
	private RemoteModloader root;

	private static boolean runningHandler = false;
	private static HashMap<String, ClientSoftwareImpl> clients = new HashMap<String, ClientSoftwareImpl>();

	public static void assignBrand(UUID uuid, String brand) {
		ClientSoftwareImpl info = clients.getOrDefault(uuid.toString(), new ClientSoftwareImpl());
		info.clientBrand = brand;
		clients.put(uuid.toString(), info);
	}

	public static ClientSoftware getClientInfo(Object player) {
		if (player == null)
			return new ClientSoftwareImpl();
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
			clients.clear();
			runningHandler = false;
		}, "ModKit Protocol Information Server Thread").start();
	}

	public static void assignProtocol(ServerPacketProcessor processor, double protocolVersion, double min, double max) {
		String uuid = HandshakeUtils.getImpl().getUUID(processor).toString();
		ClientSoftwareImpl info = clients.getOrDefault(uuid, new ClientSoftwareImpl());
		if (info.hasHandshaked)
			return;

		info.clientProtocolMin = min;
		info.clientProtocolMax = max;
		info.clientProtocol = protocolVersion;
		clients.put(uuid, info);
	}

	public static void setModInfo(ServerPacketProcessor processor, HandshakeLoaderListPacket list) {
		UUID uuid = HandshakeUtils.getImpl().getUUID(processor);
		ClientSoftwareImpl info = clients.getOrDefault(uuid.toString(), new ClientSoftwareImpl());
		if (info.hasHandshaked)
			return;

		ArrayList<RemoteModloader> loaders = new ArrayList<RemoteModloader>();
		for (ModloaderData data : list.loaders) {
			Map<String[], String> mods = new LinkedHashMap<String[], String>();
			Map<String[], String> coremods = new LinkedHashMap<String[], String>();
			Map<String, Version> entries = new LinkedHashMap<String, Version>();

			for (Mod m : data.mods) {
				mods.put(new String[] { m.id, m.displayName }, m.version);
			}
			for (Mod m : data.coremods) {
				coremods.put(new String[] { m.id, m.displayName }, m.version);
			}
			for (Entry e : data.entries) {
				entries.put(e.key, Version.fromString(e.version));
			}

			RemoteModloader ldr = new RemoteModloader(data.name, data.simpleName, data.typeName,
					Version.fromString(data.version), data.hasProtocols ? new ModKitProtocolRules() {

						@Override
						public double modloaderProtocol() {
							return data.currentLoaderProtocol;
						}

						@Override
						public double modloaderMinProtocol() {
							return data.minLoaderProtocol;
						}

						@Override
						public double modloaderMaxProtocol() {
							return data.maxLoaderProtocol;
						}

						@Override
						public double modkitProtocolVersion() {
							return data.modkit;
						}
					} : null, data.gameVersion == null ? null : Version.fromString(data.version), coremods, mods,
					entries);
			loaders.add(ldr);

			if (data.isRoot)
				info.root = ldr;
		}

		info.loaders = loaders.toArray(t -> new RemoteModloader[t]);
	}

	public static void completeHandshake(ServerPacketProcessor processor) {
		UUID uuid = HandshakeUtils.getImpl().getUUID(processor);
		ClientSoftwareImpl info = clients.getOrDefault(uuid.toString(), new ClientSoftwareImpl());
		if (info.hasHandshaked)
			return;

		info.playerUUID = uuid;
		info.playerName = HandshakeUtils.getImpl().getPlayerName(processor);
		info.player = HandshakeUtils.getImpl().getPlayerObject(processor);

		// Complete the handshake process
		HandshakeUtils.getImpl().switchStateConnected(processor, true);
		HandshakeUtils.getImpl().dispatchFinishEvent(processor);
		new HandshakeFinishedPacket().write(processor.getChannel());
		info.hasHandshaked = true;

		// Log finished message
		info(Colors.GOLD + "Player " + HandshakeUtils.getImpl().getPlayerName(processor)
				+ " logged in with a CYAN client, " + info.getAllMods().length + " mod"
				+ (info.getAllMods().length == 1 ? "" : "s") + " installed.");

		clients.put(uuid.toString(), info);
	}

	public static void assignPlayerObjects(String name, UUID uuid, Object player) {
		ClientSoftwareImpl info = clients.getOrDefault(uuid.toString(), new ClientSoftwareImpl());
		if (info.hasHandshaked)
			return;

		info.playerUUID = uuid;
		info.playerName = name;
		info.player = player;
		clients.put(uuid.toString(), info);
	}

	@Override
	protected ClientSoftware getFor(UUID uuid) {
		if (uuid == null)
			return new ClientSoftwareImpl();
		return clients.getOrDefault(uuid.toString(), new ClientSoftwareImpl());
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
	public String getBrandName() {
		return clientBrand;
	}

	@Override
	public double getModKitProtocol() {
		return clientProtocol;
	}

	@Override
	public double getMaxModKitProtocol() {
		return clientProtocolMax;
	}

	@Override
	public double getMinModKitProtocol() {
		return clientProtocolMin;
	}

	@Override
	public RemoteModloader[] getClientModloaders() {
		return loaders;
	}

	@Override
	public RemoteModloader getRootModloader() {
		return root;
	}

	@Override
	public void disconnect(String message) {
		HandshakeUtils.getImpl().disconnect(player, message);
	}

	@Override
	public void disconnectTranslatable(String message, Object... args) {
		HandshakeUtils.getImpl().disconnect(player, message, args);
	}

}
