package org.asf.cyan.internal.modkitimpl.handshake;

import org.asf.cyan.api.network.channels.PacketChannel;
import org.asf.cyan.internal.modkitimpl.handshake.packets.processors.HandshakeCompletionPacketProcessor;
import org.asf.cyan.internal.modkitimpl.handshake.packets.processors.HandshakeLoaderPacketProcessor;
import org.asf.cyan.internal.modkitimpl.handshake.packets.processors.HandshakeModPacketProcessor;
import org.asf.cyan.internal.modkitimpl.handshake.packets.processors.HandshakeProtocolPacketProcessor;

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

}
