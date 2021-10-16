package org.asf.cyan.internal.modkitimpl.handshake.processors;

import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeProtocolPacket;
import org.asf.cyan.internal.modkitimpl.info.Protocols;
import org.asf.cyan.internal.modkitimpl.util.ClientSoftwareImpl;
import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;

import modkit.network.PacketReader;
import modkit.network.channels.ServerPacketProcessor;
import modkit.protocol.ModKitProtocol;
import modkit.protocol.handshake.Handshake;

public class HandshakeProtocolPacketProcessorServer extends ServerPacketProcessor {

	@Override
	public String id() {
		return "core";
	}

	@Override
	protected void process(PacketReader content) {
		ClientSoftwareImpl.startInfoHandler(this);
		if (((ClientSoftwareImpl) ClientSoftwareImpl.getForUUID(HandshakeUtils.getImpl().getUUID(this)))
				.hasHandshaked()) {
			return;
		}

		HandshakeProtocolPacket packet = new HandshakeProtocolPacket().read(content);

		// Validate the protocol version
		int status = Handshake.validateModKitProtocol(Protocols.MODKIT_PROTOCOL, ModKitProtocol.MIN_PROTOCOL,
				ModKitProtocol.MAX_PROTOCOL, packet.currentProtocolVersion, packet.minProtocolVersion,
				packet.maxProtocolVersion);

		if (status != 0) {
			final String failure;
			final Object[] args;

			if (status == 2) {
				// Disconnect the client, we are using an older protocol
				failure = "modkit.protocol.outdated.local";
				args = new Object[] { "\u00A76" + ModKitProtocol.CURRENT, "\u00A76" + packet.minProtocolVersion };
				info("Client connection failed: outdated server modkit protocol: " + ModKitProtocol.CURRENT
						+ ", client protocol: " + packet.currentProtocolVersion + " (min: " + packet.minProtocolVersion
						+ ", max: " + packet.maxProtocolVersion + ")");
			} else {
				// Disconnect the client, we are using an newer protocol
				failure = "modkit.protocol.outdated.remote";
				args = new Object[] { "\u00A76" + packet.currentProtocolVersion,
						"\u00A76" + ModKitProtocol.MIN_PROTOCOL };
				info("Client connection failed: outdated client modkit protocol: " + packet.currentProtocolVersion
						+ ", server protocol: " + ModKitProtocol.CURRENT + " (min: " + ModKitProtocol.MIN_PROTOCOL
						+ ", max: " + ModKitProtocol.MAX_PROTOCOL + ")");
			}

			HandshakeUtils.getImpl().disconnectSimple(this, failure, args);
			return;
		}

		// Protocols are compatible, assign the current version
		ClientSoftwareImpl.assignProtocol(this, packet.currentProtocolVersion, packet.minProtocolVersion,
				packet.maxProtocolVersion);

		// Send a protocol packet back to the client
		HandshakeProtocolPacket protocol = new HandshakeProtocolPacket();
		protocol.currentProtocolVersion = ModKitProtocol.CURRENT;
		protocol.minProtocolVersion = ModKitProtocol.MIN_PROTOCOL;
		protocol.maxProtocolVersion = ModKitProtocol.MAX_PROTOCOL;
		protocol.write(getChannel());
	}

}
