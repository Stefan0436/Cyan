package org.asf.cyan.internal.modkitimpl.handshake.packets;

import org.asf.cyan.api.versioning.Version;

import modkit.events.network.AbstractPacket;
import modkit.network.PacketReader;
import modkit.network.PacketWriter;

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
