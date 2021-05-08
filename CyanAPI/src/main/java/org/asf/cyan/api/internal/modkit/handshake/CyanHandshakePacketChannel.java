package org.asf.cyan.api.internal.modkit.handshake;

import org.asf.cyan.api.internal.modkit.handshake.packets.HandshakeCompletionPacketProcessor;
import org.asf.cyan.api.internal.modkit.handshake.packets.HandshakeLoaderPacketProcessor;
import org.asf.cyan.api.internal.modkit.handshake.packets.HandshakeModPacketProcessor;
import org.asf.cyan.api.internal.modkit.handshake.packets.HandshakeProtocolPacketProcessor;
import org.asf.cyan.api.network.channels.PacketChannel;

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
