package org.asf.cyan.internal.modkitimpl.handshake.packets;

import org.asf.cyan.api.events.network.AbstractPacket;
import org.asf.cyan.api.network.PacketReader;
import org.asf.cyan.api.network.PacketWriter;
import org.asf.cyan.api.versioning.Version;

public class HandshakeLoaderPacket extends AbstractPacket<HandshakeLoaderPacket> {

	@Override
	protected String id() {
		return "loader";
	}

	public double protocol;
	public Version version;

	@Override
	protected void readEntries(PacketReader reader) {
		protocol = reader.readDouble();
		version = reader.readVersion();
	}

	@Override
	protected void writeEntries(PacketWriter writer) {
		writer.writeDouble(protocol);
		writer.writeVersion(version);
	}

}
