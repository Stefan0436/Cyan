package org.asf.cyan.internal.modkitimpl.handshake.packets;

import java.util.ArrayList;
import java.util.Map;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.versioning.Version;

import modkit.events.network.AbstractPacket;
import modkit.network.PacketReader;
import modkit.network.PacketWriter;
import modkit.protocol.ModKitModloader;

public class HandshakeLoaderListPacket extends AbstractPacket<HandshakeLoaderListPacket> {

	@Override
	protected String id() {
		return "mods";
	}

	public static class ModloaderData {
		public boolean hasProtocols = false;
		public boolean isRoot = false;

		public double modkit = -1;
		public double currentLoaderProtocol = -1;
		public double minLoaderProtocol = -1;
		public double maxLoaderProtocol = -1;

		public String name;
		public String simpleName;
		public String typeName;
		public String version;

		public String gameVersion;

		public ArrayList<Mod> mods = new ArrayList<Mod>();
		public ArrayList<Mod> coremods = new ArrayList<Mod>();

		public ArrayList<Entry> entries = new ArrayList<Entry>();
	}

	public static class Mod {
		public Mod() {
		}

		public Mod(String id, String displayName, String version) {
			this.id = id;
			this.displayName = displayName;
			this.version = version;
		}

		public String id;
		public String displayName;
		public String version;
	}

	public static class Entry {
		public Entry() {
		}

		public Entry(String key, String version) {
			this.key = key;
			this.version = version;
		}

		public String key;
		public String version;
	}

	public double currentLoaderProtocol = -1;
	public double minLoaderProtocol = -1;
	public double maxLoaderProtocol = -1;

	public void fill() {
		loaders.clear();

		currentLoaderProtocol = CyanLoader.getModloader(CyanLoader.class).modloaderProtocol();
		minLoaderProtocol = CyanLoader.getModloader(CyanLoader.class).modloaderMinProtocol();
		maxLoaderProtocol = CyanLoader.getModloader(CyanLoader.class).modloaderMaxProtocol();

		for (Modloader loader : Modloader.getAllModloaders()) {
			ModloaderData d = new ModloaderData();
			d.hasProtocols = loader instanceof ModKitModloader && loader instanceof ModKitModloader.ModKitProtocolRules;
			if (d.hasProtocols) {
				d.isRoot = ((ModKitModloader) loader).isRootModloader();
				d.modkit = ((ModKitModloader.ModKitProtocolRules) loader).modkitProtocolVersion();
				d.currentLoaderProtocol = ((ModKitModloader.ModKitProtocolRules) loader).modloaderProtocol();
				d.minLoaderProtocol = ((ModKitModloader.ModKitProtocolRules) loader).modloaderMinProtocol();
				d.maxLoaderProtocol = ((ModKitModloader.ModKitProtocolRules) loader).modloaderMaxProtocol();
			}

			d.name = loader.getName();
			d.simpleName = loader.getSimpleName();
			d.typeName = loader.getClass().getTypeName();
			d.version = loader.getVersion().toString();
			d.gameVersion = loader.getGameVersion();

			for (IModManifest mod : loader.getLoadedMods()) {
				d.mods.add(new Mod(mod.id(), mod.displayName(), mod.version().toString()));
			}
			for (IModManifest mod : loader.getLoadedCoremods()) {
				d.coremods.add(new Mod(mod.id(), mod.displayName(), mod.version().toString()));
			}

			Map<String, Version> entries = loader.getRuleEntries();
			entries.forEach((k, v) -> {
				d.entries.add(new Entry(k, v.toString()));
			});
			
			loaders.add(d);
		}
	}

	public ArrayList<ModloaderData> loaders = new ArrayList<ModloaderData>();

	@Override
	protected void readEntries(PacketReader packet) {
		currentLoaderProtocol = packet.readDouble();
		minLoaderProtocol = packet.readDouble();
		maxLoaderProtocol = packet.readDouble();

		int l = packet.readInt();
		for (int i = 0; i < l; i++) {
			ModloaderData d = new ModloaderData();
			d.hasProtocols = packet.readBoolean();
			if (d.hasProtocols) {
				d.isRoot = packet.readBoolean();
				d.modkit = packet.readDouble();
				d.currentLoaderProtocol = packet.readDouble();
				d.minLoaderProtocol = packet.readDouble();
				d.maxLoaderProtocol = packet.readDouble();
			}

			d.name = packet.readString();
			d.simpleName = packet.readString();
			d.typeName = packet.readString();
			d.version = packet.readString();

			if (packet.readBoolean())
				d.gameVersion = packet.readString();

			int c = packet.readInt();
			for (int i2 = 0; i2 < c; i2++) {
				Mod m = new Mod();
				m.id = packet.readString();
				m.displayName = packet.readString();
				m.version = packet.readString();
				d.mods.add(m);
			}

			c = packet.readInt();
			for (int i2 = 0; i2 < c; i2++) {
				Mod m = new Mod();
				m.id = packet.readString();
				m.displayName = packet.readString();
				m.version = packet.readString();
				d.coremods.add(m);
			}

			c = packet.readInt();
			for (int i2 = 0; i2 < c; i2++) {
				Entry e = new Entry();
				e.key = packet.readString();
				e.version = packet.readString();
				d.entries.add(e);
			}

			loaders.add(d);
		}
	}

	@Override
	protected void writeEntries(PacketWriter packet) {
		packet.writeDouble(currentLoaderProtocol);
		packet.writeDouble(minLoaderProtocol);
		packet.writeDouble(maxLoaderProtocol);

		packet.writeInt(loaders.size());
		for (ModloaderData loader : loaders) {
			packet.writeBoolean(loader.hasProtocols);
			if (loader.hasProtocols) {
				packet.writeBoolean(loader.isRoot);
				packet.writeDouble(loader.modkit);
				packet.writeDouble(loader.currentLoaderProtocol);
				packet.writeDouble(loader.minLoaderProtocol);
				packet.writeDouble(loader.maxLoaderProtocol);
			}

			packet.writeString(loader.name);
			packet.writeString(loader.simpleName);
			packet.writeString(loader.typeName);
			packet.writeString(loader.version);

			packet.writeBoolean(loader.gameVersion != null);
			if (loader.gameVersion != null)
				packet.writeString(loader.gameVersion);

			packet.writeInt(loader.mods.size());
			for (Mod m : loader.mods) {
				packet.writeString(m.id);
				packet.writeString(m.displayName);
				packet.writeString(m.version);
			}

			packet.writeInt(loader.coremods.size());
			for (Mod m : loader.coremods) {
				packet.writeString(m.id);
				packet.writeString(m.displayName);
				packet.writeString(m.version);
			}

			packet.writeInt(loader.entries.size());
			for (Entry e : loader.entries) {
				packet.writeString(e.key);
				packet.writeString(e.version);
			}
		}
	}

}
