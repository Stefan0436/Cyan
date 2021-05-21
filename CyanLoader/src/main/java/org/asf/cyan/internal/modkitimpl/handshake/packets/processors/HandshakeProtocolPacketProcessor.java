package org.asf.cyan.internal.modkitimpl.handshake.packets.processors;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.network.PacketReader;
import org.asf.cyan.api.network.channels.ServerPacketProcessor;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeFailedPacket;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeFailedPacket.FailureType;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeLoaderPacket;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeProtocolPacket;
import org.asf.cyan.internal.modkitimpl.info.Protocols;
import org.asf.cyan.internal.modkitimpl.util.ClientImpl;
import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;

public class HandshakeProtocolPacketProcessor extends ServerPacketProcessor {

	//
	// Permanent (raw) format:
	// integer - error
	// double - version
	// string - language key
	// string - display version
	//
	// Error 0: outdated local protocol, remote newer
	// Error 1: outdated remote protocol, local newer
	// Error 2: outdated local loader, remote newer
	// Error 3: outdated remote loader, local newer
	//
	// Protocol Negotiation uses RawReader or plain reading to receive
	// Protocol Negotiation uses RawWriter or plain writing to write
	//
	// NIO ByteBuffer is used by the underlying RawWriter and RawReader
	// CyanUtil Packets are used by the packet writer. (flow format)
	//

	@Override
	public String id() {
		return "core";
	}

	@Override
	protected void process(PacketReader content) {
		ClientImpl.startInfoHandler(this);
		HandshakeProtocolPacket packet = new HandshakeProtocolPacket().read(content);
		if (packet.currentProtocolVersion < Protocols.MIN_MODKIT
				|| (packet.maxProtocolVersion != -1 && Protocols.MODKIT_PROTOCOL > packet.maxProtocolVersion)) {
			HandshakeFailedPacket response = new HandshakeFailedPacket();
			response.failure = FailureType.PROTOCOL_REMOTE;
			response.language = "modkit.protocol.outdated.remote";
			response.displayVersion = Double.toString(Protocols.MIN_MODKIT);
			response.version = Protocols.MIN_MODKIT;
			response.write(getChannel());
			HandshakeUtils.getImpl().disconnectColored1(this, response, packet.currentProtocolVersion);
		} else if (packet.currentProtocolVersion > Protocols.MAX_MODKIT
				|| (packet.minProtocolVersion != -1 && Protocols.MODKIT_PROTOCOL < packet.minProtocolVersion)) {
			HandshakeFailedPacket response = new HandshakeFailedPacket();
			response.failure = FailureType.PROTOCOL_LOCAL;
			response.language = "modkit.protocol.outdated.local";
			response.displayVersion = Double.toString(Protocols.MAX_MODKIT);
			response.version = Protocols.MAX_MODKIT;
			response.write(getChannel());
			HandshakeUtils.getImpl().disconnectColored1(this, response, packet.currentProtocolVersion);
		} else {
			ClientImpl.assignProtocol(this, packet.currentProtocolVersion);
			HandshakeLoaderPacket response = new HandshakeLoaderPacket();
			response.protocol = Protocols.LOADER_PROTOCOL;
			response.version = Modloader.getModloader(CyanLoader.class).getVersion();
			response.protocolMin = Protocols.MIN_LOADER;
			response.protocolMax = Protocols.MAX_LOADER;
			response.write(getChannel());
		}
	}

}
