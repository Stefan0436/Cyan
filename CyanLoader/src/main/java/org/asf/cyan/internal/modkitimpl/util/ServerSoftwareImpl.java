package org.asf.cyan.internal.modkitimpl.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.asf.cyan.api.modloader.IPostponedComponent;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.core.CyanInfo;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeLoaderListPacket;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeProtocolPacket;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeLoaderListPacket.Entry;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeLoaderListPacket.Mod;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeLoaderListPacket.ModloaderData;
import org.asf.cyan.internal.modkitimpl.handshake.processors.HandshakeResetPacketProcessor;
import org.asf.cyan.internal.modkitimpl.info.Protocols;

import modkit.network.channels.ClientPacketProcessor;
import modkit.protocol.ModKitModloader;
import modkit.protocol.ModKitProtocol;
import modkit.protocol.ModKitModloader.ModKitProtocolRules;
import modkit.util.client.ServerSoftware;
import modkit.util.remotedata.RemoteModloader;
import modkit.util.server.language.ClientLanguage;

public class ServerSoftwareImpl extends ServerSoftware implements IPostponedComponent {

	private boolean hasHandshaked;

	public boolean hasHandshaked() {
		return hasHandshaked;
	}

	private double protocol = -1;
	private double minProtocol;
	private double maxProtocol;

	private String brand;
	private RemoteModloader[] loaders;

	private Version gameVersion;
	private RemoteModloader rootLoader;

	public ServerSoftwareImpl() {
		if (HandshakeUtils.getImpl() != null)
			brand = HandshakeUtils.getImpl().getServerBrand();
		protocol = -1;
		minProtocol = -1;
		maxProtocol = -1;
		loaders = new RemoteModloader[0];
		gameVersion = null;
		rootLoader = null;
		hasHandshaked = false;
	}

	public ServerSoftwareImpl(String brand, RemoteModloader[] loaders, double protocol, double minProtocol,
			double maxProtocol, Version gameVersion, RemoteModloader rootLoader) {
		this.gameVersion = gameVersion;
		this.protocol = protocol;
		this.minProtocol = minProtocol;
		this.maxProtocol = maxProtocol;
		this.brand = brand;
		this.loaders = loaders;
		this.rootLoader = rootLoader;
		hasHandshaked = false;
	}

	@Override
	public void initComponent() {
		implementation = this;
	}

	public static ServerSoftwareImpl getCurrent() {
		if (CyanInfo.getSide() == GameSide.CLIENT) {
			if (!HandshakeUtils.getImpl().isInGame()) {
				return null;
			} else {
				return (ServerSoftwareImpl) HandshakeUtils.getImpl().getSoftware();
			}
		} else {
			ArrayList<RemoteModloader> loaders = new ArrayList<RemoteModloader>();
			RemoteModloader root = null;

			for (Modloader loader : Modloader.getAllModloaders()) {
				Map<String[], String> mods = new LinkedHashMap<String[], String>();
				Map<String[], String> coremods = new LinkedHashMap<String[], String>();
				for (IModManifest manifest : loader.getLoadedCoremods()) {
					coremods.put(new String[] { manifest.id(), manifest.displayName() }, manifest.version().toString());
				}
				for (IModManifest manifest : loader.getLoadedMods()) {
					mods.put(new String[] { manifest.id(), manifest.displayName() }, manifest.version().toString());
				}

				ModKitModloader.ModKitProtocolRules protocols = null;
				Version gameVersion = loader.getGameVersion() == null ? null
						: Version.fromString(loader.getGameVersion());
				if (loader instanceof ModKitModloader && loader instanceof ModKitModloader.ModKitProtocolRules) {
					protocols = (ModKitModloader.ModKitProtocolRules) loader;
				}

				RemoteModloader ldr = new RemoteModloader(loader.getName(), loader.getSimpleName(),
						loader.getClass().getTypeName(), loader.getVersion(), protocols, gameVersion, coremods, mods,
						loader.getRuleEntries());
				loaders.add(ldr);

				if (loader instanceof ModKitModloader) {
					ModKitModloader ld = (ModKitModloader) loader;
					if (ld.isRootModloader())
						root = ldr;
				}
			}

			return new ServerSoftwareImpl(Modloader.getModloaderGameBrand(),
					loaders.toArray(t -> new RemoteModloader[t]), Protocols.MODKIT_PROTOCOL,
					ModKitProtocol.MIN_PROTOCOL, ModKitProtocol.MAX_PROTOCOL,
					Version.fromString(CyanInfo.getMinecraftVersion()), root);
		}
	}

	@Override
	protected ServerSoftware describeCurrent() {
		return getCurrent();
	}

	@Override
	public String getBrandName() {
		return brand;
	}

	@Override
	public RemoteModloader[] getServerModloaders() {
		return loaders;
	}

	@Override
	public double getModKitProtocol() {
		return protocol;
	}

	@Override
	public double getMaxModKitProtocol() {
		return maxProtocol;
	}

	@Override
	public double getMinModKitProtocol() {
		return minProtocol;
	}

	@Override
	public Version getGameVersion() {
		return gameVersion;
	}

	@Override
	public RemoteModloader getRootModloader() {
		return rootLoader;
	}

	public void reset() {
		if (CallTrace.traceCall(1).getTypeName().equals(HandshakeResetPacketProcessor.class.getTypeName())) {
			brand = HandshakeUtils.getImpl().getServerBrand();
			protocol = -1;
			minProtocol = -1;
			maxProtocol = -1;
			loaders = new RemoteModloader[0];
			gameVersion = null;
			rootLoader = null;
			hasHandshaked = false;
		}
	}

	public void assignProtocols(HandshakeProtocolPacket packet) {
		if (protocol == -1) {
			this.protocol = packet.currentProtocolVersion;
			this.minProtocol = packet.minProtocolVersion;
			this.maxProtocol = packet.maxProtocolVersion;
		}
	}

	public void assignMods(HandshakeLoaderListPacket list) {
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

			if (data.isRoot) {
				this.rootLoader = ldr;
				this.gameVersion = Version.fromString(data.gameVersion);
			}
		}

		this.loaders = loaders.toArray(t -> new RemoteModloader[t]);
	}

	public void completeHandshake(ClientPacketProcessor processor) {
		if (!hasHandshaked) {
			ClientLanguage.writeKnownKeys(processor.getChannel());
			HandshakeUtils.getImpl().dispatchConnectionEvent(processor);
			HandshakeUtils.getImpl().closeLevelScreen();
			hasHandshaked = true;
		}
	}

}
