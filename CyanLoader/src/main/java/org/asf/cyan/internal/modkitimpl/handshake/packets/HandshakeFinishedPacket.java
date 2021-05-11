package org.asf.cyan.internal.modkitimpl.handshake.packets;

import org.asf.cyan.api.events.network.AbstractPacket;
import org.asf.cyan.api.network.PacketReader;
import org.asf.cyan.api.network.PacketWriter;

public class HandshakeFinishedPacket extends AbstractPacket<HandshakeFinishedPacket> {

	@Override
	protected String id() {
		return "handshake.finish";
	}

	@Override
	protected void readEntries(PacketReader packet) {
	}

	@Override
	protected void writeEntries(PacketWriter packet) {
	}

}
