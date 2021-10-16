package org.asf.cyan.internal.modkitimpl.handshake.packets;

import modkit.events.network.AbstractPacket;
import modkit.network.PacketReader;
import modkit.network.PacketWriter;

public class HandshakeResetPacket extends AbstractPacket<HandshakeResetPacket> {

	@Override
	protected String id() {
		return "reset";
	}

	@Override
	protected void readEntries(PacketReader packet) {
	}

	@Override
	protected void writeEntries(PacketWriter packet) {
	}

}
