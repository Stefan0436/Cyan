package org.asf.cyan.internal.modkitimpl.handshake.packets.processors;

import org.asf.cyan.api.network.PacketReader;
import org.asf.cyan.api.network.channels.ClientPacketProcessor;
import org.asf.cyan.api.util.server.language.ClientLanguage;
import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;
import org.asf.cyan.internal.modkitimpl.util.ScreenUtil;

public class HandshakeCompletionPacketProcessor extends ClientPacketProcessor {

	@Override
	public String id() {
		return "handshake.finish";
	}

	@Override
	protected void process(PacketReader reader) {
		ScreenUtil.getImpl().setScreenToWorld(this);
		ClientLanguage.writeKnownKeys(getChannel());
		HandshakeUtils.getImpl().dispatchConnectionEvent(this); 
	}

}
