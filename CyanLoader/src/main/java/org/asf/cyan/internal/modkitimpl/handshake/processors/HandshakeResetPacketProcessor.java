package org.asf.cyan.internal.modkitimpl.handshake.processors;

import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;
import org.asf.cyan.internal.modkitimpl.util.ServerSoftwareImpl;

import modkit.network.PacketReader;
import modkit.network.channels.ClientPacketProcessor;

public class HandshakeResetPacketProcessor extends ClientPacketProcessor {

	@Override
	public String id() {
		return "reset";
	}

	@Override
	protected void process(PacketReader reader) {
		((ServerSoftwareImpl) HandshakeUtils.getImpl().getSoftware()).reset();
	}

}
