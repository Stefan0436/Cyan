package org.asf.cyan.api.internal.modkit.handshake.packets.content;

import org.asf.cyan.api.events.network.AbstractPacket;
import org.asf.cyan.api.network.PacketReader;
import org.asf.cyan.api.network.PacketWriter;
import org.asf.cyan.api.network.PacketReader.RawReader;
import org.asf.cyan.api.network.PacketWriter.RawWriter;

public class HandshakeProtocolPacket extends AbstractPacket<HandshakeProtocolPacket> {

	@Override
	protected String id() {
		return "core";
	}
	
	public double protocolVersion;

	@Override
	protected void readEntries(PacketReader reader) {
		RawReader raw = new RawReader(reader);
		protocolVersion = raw.readDouble();
	}

	@Override
	protected void writeEntries(PacketWriter writer) {
		RawWriter raw = new RawWriter(writer);
		raw.writeDouble(protocolVersion);
	}

}
