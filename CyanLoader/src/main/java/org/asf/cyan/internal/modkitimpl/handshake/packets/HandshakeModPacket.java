package org.asf.cyan.internal.modkitimpl.handshake.packets;

import java.util.ArrayList;
import java.util.HashMap;

import org.asf.cyan.api.events.network.AbstractPacket;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.network.PacketReader;
import org.asf.cyan.api.network.PacketWriter;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.api.protocol.handshake.HandshakeRule;

public class HandshakeModPacket extends AbstractPacket<HandshakeModPacket> {

	public HashMap<String, Version> entries = new HashMap<String, Version>();
	public ArrayList<HandshakeRule> remoteRules = new ArrayList<HandshakeRule>();
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
		count = reader.readInt();
		for (int i = 0; i < count; i++) {
			remoteRules.add(new HandshakeRule((reader.readInt() == 0 ? GameSide.SERVER : GameSide.CLIENT),
					reader.readString(), reader.readString()));
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
		writer.writeInt(remoteRules.size());
		remoteRules.forEach(rule -> {
			writer.writeInt(rule.getSide() == GameSide.SERVER ? 0 : 1);
			writer.writeString(rule.getKey());
			writer.writeString(rule.getCheckString());
		});
		writer.writeDouble(clientProtocol);
	}

}
