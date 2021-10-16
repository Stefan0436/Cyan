package org.asf.cyan.internal.modkitimpl.handshake.processors;

import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;
import org.asf.cyan.internal.modkitimpl.util.ServerSoftwareImpl;

import modkit.network.PacketReader;
import modkit.network.channels.ClientPacketProcessor;

public class HandshakeLevelScreenPacketProcessor extends ClientPacketProcessor {

	@Override
	public String id() {
		return "ldscn";
	}

	@Override
	protected void process(PacketReader reader) {
		if (!((ServerSoftwareImpl) HandshakeUtils.getImpl().getSoftware()).hasHandshaked()) {
			HandshakeUtils.getImpl().reopenLevelScreen();
		}
	}

}
