package org.asf.cyan.internal.modkitimpl.handshake.packets.processors;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeLoaderPacket;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeProtocolPacket;
import org.asf.cyan.internal.modkitimpl.info.Protocols;
import org.asf.cyan.internal.modkitimpl.util.ClientImpl;

import modkit.network.PacketReader;
import modkit.network.channels.ServerPacketProcessor;

public class HandshakeProtocolPacketProcessor extends ServerPacketProcessor {

	@Override
	public String id() {
		return "core";
	}

	@Override
	protected void process(PacketReader content) {
		ClientImpl.startInfoHandler(this);
		HandshakeProtocolPacket packet = new HandshakeProtocolPacket().read(content);

		ClientImpl.assignProtocol(this, packet.currentProtocolVersion);
		HandshakeLoaderPacket response = new HandshakeLoaderPacket();
		response.protocol = Protocols.LOADER_PROTOCOL;
		response.version = Modloader.getModloader(CyanLoader.class).getVersion();
		response.write(getChannel());
	}

}
