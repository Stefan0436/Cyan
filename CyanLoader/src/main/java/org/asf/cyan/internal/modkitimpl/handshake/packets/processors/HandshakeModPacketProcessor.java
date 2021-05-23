package org.asf.cyan.internal.modkitimpl.handshake.packets.processors;

import org.asf.cyan.internal.modkitimpl.util.ClientImpl;
import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;

import modkit.network.PacketReader;
import modkit.network.channels.ServerPacketProcessor;
import modkit.util.Colors;

import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeModPacket;

public class HandshakeModPacketProcessor extends ServerPacketProcessor {

	@Override
	public String id() {
		return "mods";
	}

	@Override
	protected void process(PacketReader reader) {
		HandshakeModPacket packet = new HandshakeModPacket().read(reader);
		ClientImpl.assignModloader(this, packet.entries.get("game"), packet.clientProtocol,
				packet.entries.get("modloader"));

		int mods = 0;
		for (String id : packet.entries.keySet()) {
			if (!id.equals("modloader") && !id.equals("game")) {
				ClientImpl.assignMod(this, id, packet.entries.get(id));
				mods++;
			}
		}

		ClientImpl.assignPlayer(this);
		info(Colors.GOLD + "Player " + HandshakeUtils.getImpl().getPlayerName(this) + " logged in with a CYAN client, "
				+ mods + " mods installed.");
		HandshakeUtils.getImpl().switchStateConnected(this, true);

		getChannel().sendPacket("handshake.finish");
		HandshakeUtils.getImpl().dispatchFinishEvent(this);
	}

}
