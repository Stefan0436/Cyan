package org.asf.cyan.internal.modkitimpl.handshake.packets.processors;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.network.PacketReader;
import org.asf.cyan.api.network.channels.ClientPacketProcessor;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeFailedPacket;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeFailedPacket.FailureType;
import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;
import org.asf.cyan.internal.modkitimpl.util.ScreenUtil;
import org.asf.cyan.mods.dependencies.HandshakeRule;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeLoaderPacket;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeModPacket;
import org.asf.cyan.internal.modkitimpl.info.Protocols;

public class HandshakeLoaderPacketProcessor extends ClientPacketProcessor {

	@Override
	public String id() {
		return "loader";
	}

	@Override
	protected void process(PacketReader reader) {
		ScreenUtil.getImpl().setScreenToReceiveLevel(this);
		HandshakeLoaderPacket packet = new HandshakeLoaderPacket().read(reader);
		Version version = Modloader.getModloader(CyanLoader.class).getVersion();

		int status = HandshakeUtils.getImpl().validateLoaderProtocol(
				packet.protocol, packet.protocolMin,
				packet.protocolMax, Protocols.LOADER_PROTOCOL, Protocols.MIN_LOADER, Protocols.MAX_LOADER);
		if (status == 2) {
			HandshakeFailedPacket response = new HandshakeFailedPacket();
			response.failure = FailureType.LOADER_LOCAL;
			response.language = "modkit.loader.outdated.local";
			response.displayVersion = version.toString();
			response.version = Protocols.MIN_LOADER;
			response.write(getChannel());
			HandshakeUtils.getImpl().disconnect(this, response, packet);
		} else if (status == 1) {
			HandshakeFailedPacket response = new HandshakeFailedPacket();
			response.failure = FailureType.LOADER_REMOTE;
			response.language = "modkit.loader.outdated.remote";
			response.displayVersion = version.toString();
			response.version = Protocols.MAX_LOADER;
			response.write(getChannel());
			HandshakeUtils.getImpl().disconnect(this, response, packet);
		} else {
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

}
