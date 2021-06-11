package org.asf.cyan.internal.modkitimpl.handshake.packets.processors;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeModPacket;
import org.asf.cyan.internal.modkitimpl.info.Protocols;

import modkit.network.PacketReader;
import modkit.network.channels.ClientPacketProcessor;
import modkit.protocol.handshake.HandshakeRule;

public class HandshakeLoaderPacketProcessor extends ClientPacketProcessor {

	@Override
	public String id() {
		return "loader";
	}

	@Override
	protected void process(PacketReader reader) {
		HandshakeModPacket response = new HandshakeModPacket();
		response.clientProtocol = Protocols.LOADER_PROTOCOL;

		response.entries.put("game", Version.fromString(Modloader.getModloaderGameVersion()));
		response.entries.put("modloader", Modloader.getModloader(CyanLoader.class).getVersion());

		for (IModManifest mod : Modloader.getAllMods()) {
			response.entries.putIfAbsent(mod.id(), mod.version());
		}			
		for (HandshakeRule rule : HandshakeRule.getAllRules()) {
			response.remoteRules.add(rule);
		}
		
		response.write(getChannel());
	}

}
