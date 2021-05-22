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
	public double protocolMax = -1;
	public double protocolMin = -1;
	public Version version;

	@Override
	protected void readEntries(PacketReader reader) {
		protocol = reader.readDouble();
		version = reader.readVersion();
		if (protocol >= 0.14) {
			protocolMin = reader.readDouble();
			protocolMax = reader.readDouble();
		}
	}

	@Override
	protected void writeEntries(PacketWriter writer) {
		writer.writeDouble(protocol);
		writer.writeVersion(version);
		writer.writeDouble(protocolMin);
		writer.writeDouble(protocolMax);
	}

}
