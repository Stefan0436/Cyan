package org.asf.cyan.internal.modkitimpl;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.internal.modkitimpl.handshake.CyanHandshakePacketChannel;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeProtocolPacket;
import org.asf.cyan.internal.modkitimpl.info.Protocols;
import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;

import modkit.protocol.ModKitProtocol;

public class HandshakeComponent {

	public static void handshakeStartClient(Object client) {
		if (CyanLoader.getModloader(CyanLoader.class).isRootModloader()) {
			CyanHandshakePacketChannel channel = HandshakeUtils.getImpl().getChannel(CyanHandshakePacketChannel.class,
					client);
			HandshakeProtocolPacket packet = new HandshakeProtocolPacket();
			packet.currentProtocolVersion = Protocols.MODKIT_PROTOCOL;
			packet.maxProtocolVersion = ModKitProtocol.MAX_PROTOCOL;
			packet.minProtocolVersion = ModKitProtocol.MIN_PROTOCOL;
			packet.write(channel);
		}
	}

}
