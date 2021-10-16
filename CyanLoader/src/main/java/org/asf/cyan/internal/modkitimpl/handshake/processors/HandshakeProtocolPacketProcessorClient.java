package org.asf.cyan.internal.modkitimpl.handshake.processors;

import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeLoaderListPacket;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeProtocolPacket;
import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;
import org.asf.cyan.internal.modkitimpl.util.ServerSoftwareImpl;

import modkit.network.PacketReader;
import modkit.network.channels.ClientPacketProcessor;

public class HandshakeProtocolPacketProcessorClient extends ClientPacketProcessor {

	@Override
	public String id() {
		return "core";
	}

	@Override
	protected void process(PacketReader content) {
		if (((ServerSoftwareImpl) HandshakeUtils.getImpl().getSoftware()).hasHandshaked())
			return;

		HandshakeProtocolPacket packet = new HandshakeProtocolPacket().read(content);
		ServerSoftwareImpl software = (ServerSoftwareImpl) HandshakeUtils.getImpl().getSoftware();
		software.assignProtocols(packet);

		HandshakeLoaderListPacket list = new HandshakeLoaderListPacket();
		list.fill();
		list.write(getChannel());
	}

}
