package org.asf.cyan.internal.modkitimpl.handshake.packets;

import modkit.events.network.AbstractPacket;
import modkit.network.PacketReader;
import modkit.network.PacketWriter;
import modkit.network.PacketReader.RawReader;
import modkit.network.PacketWriter.RawWriter;

public class HandshakeProtocolPacket extends AbstractPacket<HandshakeProtocolPacket> {

	@Override
	protected String id() {
		return "core";
	}

	public double currentProtocolVersion;

	@Override
	protected void readEntries(PacketReader reader) {
		RawReader raw = new RawReader(reader);
		currentProtocolVersion = raw.readDouble();
	}

	@Override
	protected void writeEntries(PacketWriter writer) {
		RawWriter raw = new RawWriter(writer);
		raw.writeDouble(currentProtocolVersion);
	}

}
