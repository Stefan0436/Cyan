package org.asf.cyan.internal.modkitimpl.handshake.packets;

import java.util.ArrayList;

import org.asf.cyan.api.modloader.information.game.GameSide;

import modkit.events.network.AbstractPacket;
import modkit.network.PacketReader;
import modkit.network.PacketWriter;
import modkit.protocol.handshake.HandshakeRule;

public class HandshakeRulesPacket extends AbstractPacket<HandshakeRulesPacket> {

	@Override
	protected String id() {
		return "rules";
	}

	public ArrayList<HandshakeRule> rules = new ArrayList<HandshakeRule>();

	@Override
	protected void readEntries(PacketReader packet) {
		rules.clear();
		int l = packet.readInt();
		for (int i = 0; i < l; i++) {
			rules.add(new HandshakeRule(packet.readByte() == 0 ? GameSide.CLIENT : GameSide.SERVER, packet.readString(),
					packet.readString()));
		}
	}

	@Override
	protected void writeEntries(PacketWriter packet) {
		packet.writeInt(rules.size());
		for (HandshakeRule rule : rules) {
			packet.writeRawByte((byte) (rule.getSide() == GameSide.CLIENT ? 0 : 1));
			packet.writeString(rule.getKey());
			packet.writeString(rule.getCheckString());
		}
	}

}
