package org.asf.cyan.internal.modkitimpl.handshake;

import org.asf.cyan.internal.modkitimpl.handshake.processors.HandshakeCompletionPacketProcessor;
import org.asf.cyan.internal.modkitimpl.handshake.processors.HandshakeLevelScreenPacketProcessor;
import org.asf.cyan.internal.modkitimpl.handshake.processors.HandshakeLoaderListPacketProcessorClient;
import org.asf.cyan.internal.modkitimpl.handshake.processors.HandshakeLoaderListPacketProcessorServer;
import org.asf.cyan.internal.modkitimpl.handshake.processors.HandshakeProtocolPacketProcessorServer;
import org.asf.cyan.internal.modkitimpl.handshake.processors.HandshakeResetPacketProcessor;
import org.asf.cyan.internal.modkitimpl.handshake.processors.HandshakeRulePacketProcessor;
import org.asf.cyan.internal.modkitimpl.handshake.processors.HandshakeProtocolPacketProcessorClient;

import modkit.network.channels.PacketChannel;

public class CyanHandshakePacketChannel extends PacketChannel {

	@Override
	public String id() {
		return "cyan.handshake";
	}

	@Override
	public void setup() {
		register(HandshakeProtocolPacketProcessorServer.class);
		register(HandshakeProtocolPacketProcessorClient.class);

		register(HandshakeLoaderListPacketProcessorServer.class);
		register(HandshakeLoaderListPacketProcessorClient.class);

		register(HandshakeRulePacketProcessor.class);

		register(HandshakeCompletionPacketProcessor.class);
		register(HandshakeResetPacketProcessor.class);
		register(HandshakeLevelScreenPacketProcessor.class);
	}

	@Override
	protected PacketChannel newInstance() {
		return new CyanHandshakePacketChannel();
	}

	@Override
	public boolean supportSplitPackets() {
		return true;
	}

}
