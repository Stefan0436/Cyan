package org.asf.cyan.internal.modkitimpl.handshake.packets;

import modkit.events.network.AbstractPacket;
import modkit.network.PacketReader;
import modkit.network.PacketWriter;

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
