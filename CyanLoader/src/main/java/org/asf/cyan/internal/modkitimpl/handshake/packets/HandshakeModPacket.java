package org.asf.cyan.internal.modkitimpl.handshake.packets;

import java.util.HashMap;

import org.asf.cyan.api.versioning.Version;

import modkit.events.network.AbstractPacket;
import modkit.network.PacketReader;
import modkit.network.PacketWriter;

public class HandshakeModPacket extends AbstractPacket<HandshakeModPacket> {

	public HashMap<String, Version> entries = new HashMap<String, Version>();
	public double clientProtocol = 0d;

	@Override
	protected String id() {
		return "mods";
	}

	@Override
	protected void readEntries(PacketReader reader) {
		int count = reader.readInt();
		for (int i = 0; i < count; i++) {
			entries.put(reader.readString(), reader.readVersion());
		}
		clientProtocol = reader.readDouble();
	}

	@Override
	protected void writeEntries(PacketWriter writer) {
		writer.writeInt(entries.size());
		entries.forEach((key, val) -> {
			writer.writeString(key);
			writer.writeVersion(val);
		});
		writer.writeDouble(clientProtocol);
	}

}
