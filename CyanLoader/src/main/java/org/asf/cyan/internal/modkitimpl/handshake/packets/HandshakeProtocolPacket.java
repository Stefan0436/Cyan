package org.asf.cyan.internal.modkitimpl.handshake.packets;

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

	public double currentProtocolVersion;
	public double minProtocolVersion = -1;
	public double maxProtocolVersion = -1;

	@Override
	protected void readEntries(PacketReader reader) {
		RawReader raw = new RawReader(reader);
		currentProtocolVersion = raw.readDouble();
		if (currentProtocolVersion >= 1.1) {
			minProtocolVersion = raw.readDouble();
			maxProtocolVersion = raw.readDouble();
		}
	}

	@Override
	protected void writeEntries(PacketWriter writer) {
		RawWriter raw = new RawWriter(writer);
		raw.writeDouble(currentProtocolVersion);
		raw.writeDouble(minProtocolVersion);
		raw.writeDouble(maxProtocolVersion);
	}

}
