package org.asf.cyan.internal.modkitimpl.handshake.packets.processors;

import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;

import modkit.network.PacketReader;
import modkit.network.channels.ClientPacketProcessor;
import modkit.util.server.language.ClientLanguage;

public class HandshakeCompletionPacketProcessor extends ClientPacketProcessor {

	@Override
	public String id() {
		return "handshake.finish";
	}

	@Override
	protected void process(PacketReader reader) {
		ClientLanguage.writeKnownKeys(getChannel());
		HandshakeUtils.getImpl().dispatchConnectionEvent(this); 
	}

}
