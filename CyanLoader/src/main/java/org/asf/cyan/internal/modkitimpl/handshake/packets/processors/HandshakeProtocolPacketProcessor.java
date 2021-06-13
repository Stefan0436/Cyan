package org.asf.cyan.internal.modkitimpl.handshake.packets.processors;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeLoaderPacket;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeProtocolPacket;
import org.asf.cyan.internal.modkitimpl.info.Protocols;
import org.asf.cyan.internal.modkitimpl.util.ClientImpl;
import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;

import modkit.network.PacketReader;
import modkit.network.channels.ServerPacketProcessor;
import modkit.protocol.ModKitProtocol;
import modkit.protocol.handshake.Handshake;

public class HandshakeProtocolPacketProcessor extends ServerPacketProcessor {

	@Override
	public String id() {
		return "core";
	}

	@Override
	protected void process(PacketReader content) {
		ClientImpl.startInfoHandler(this);
		HandshakeProtocolPacket packet = new HandshakeProtocolPacket().read(content);
		int status = Handshake.validateModKitProtocol(Protocols.MODKIT_PROTOCOL, ModKitProtocol.MIN_PROTOCOL,
				ModKitProtocol.MAX_PROTOCOL, packet.currentProtocolVersion, packet.minProtocolVersion,
				packet.maxProtocolVersion);
		if (status != 0) {
			final String failure;
			final Object[] args;
			if (status == 2) {
				failure = "modkit.protocol.outdated.local";
				args = new Object[] { "\u00A76" + ModKitProtocol.CURRENT, "\u00A76" + packet.minProtocolVersion };
				info("Client connection failed: outdated server modkit protocol: " + ModKitProtocol.CURRENT
						+ ", client protocol: " + packet.currentProtocolVersion + " (min: " + packet.minProtocolVersion
						+ ", max: " + packet.maxProtocolVersion + ")");
			} else {
				failure = "modkit.protocol.outdated.remote";
				args = new Object[] { "\u00A76" + packet.currentProtocolVersion, "\u00A76" + ModKitProtocol.MIN_PROTOCOL };
				info("Client connection failed: outdated client modkit protocol: " + packet.currentProtocolVersion
						+ ", server protocol: " + ModKitProtocol.CURRENT + " (min: " + ModKitProtocol.MIN_PROTOCOL
						+ ", max: " + ModKitProtocol.MAX_PROTOCOL + ")");
			}
			HandshakeUtils.getImpl().disconnectSimple(this, failure, args);
			return;
		}
		ClientImpl.assignProtocol(this, packet.currentProtocolVersion);
		HandshakeLoaderPacket response = new HandshakeLoaderPacket();
		response.protocol = Protocols.LOADER_PROTOCOL;
		response.version = Modloader.getModloader(CyanLoader.class).getVersion();
		response.write(getChannel());
	}

}
