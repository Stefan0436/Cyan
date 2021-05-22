package org.asf.cyan.internal.modkitimpl.handshake.packets.processors;

import java.util.HashMap;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.network.PacketReader;
import org.asf.cyan.api.network.channels.ServerPacketProcessor;
import org.asf.cyan.api.util.Colors;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeFailedPacket;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeFailedPacket.FailureType;
import org.asf.cyan.internal.modkitimpl.util.ClientImpl;
import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeModPacket;
import org.asf.cyan.mods.dependencies.HandshakeRule;

public class HandshakeModPacketProcessor extends ServerPacketProcessor {

	@Override
	public String id() {
		return "mods";
	}

	@Override
	protected void process(PacketReader reader) {
		HandshakeModPacket packet = new HandshakeModPacket().read(reader);
		HandshakeRule.getAllRules().forEach(rule -> {
			if (!packet.remoteRules.stream().anyMatch(t -> t.getKey().equals(rule.getKey())
					&& t.getCheckString().equals(rule.getCheckString()) && t.getSide() == rule.getSide())) {
				packet.remoteRules.add(rule);
			}
		});

		HashMap<String, Version> localMods = new HashMap<String, Version>();
		localMods.put("game", Version.fromString(Modloader.getModloaderGameVersion()));
		localMods.put("modloader", Modloader.getModloaderVersion());
		for (IModManifest mod : Modloader.getAllMods()) {
			localMods.putIfAbsent(mod.id(), mod.version());
		}

		HashMap<String, String> output1 = new HashMap<String, String>();
		HashMap<String, String> output2 = new HashMap<String, String>();
		boolean failClient = !HandshakeRule.checkAll(packet.entries, GameSide.CLIENT, output1, packet.remoteRules);
		boolean failServer = !HandshakeRule.checkAll(localMods, GameSide.SERVER, output2, packet.remoteRules);

		String missingClient = "";
		String missingClientNonColor = "";
		String missingServer = "";
		String missingServerNonColor = "";
		if (failClient) {
			for (String key : output1.keySet()) {
				String val = output1.get(key);
				if (!missingClient.isEmpty())
					missingClient += "§7, ";
				missingClient += "§5";
				missingClient += key;
				if (!val.isEmpty()) {
					missingClient += "§7 (§6";
					missingClient += val;
					missingClient += "§7)";
				}
				missingClient += "§7";

				if (!missingClientNonColor.isEmpty())
					missingClientNonColor += ", ";
				missingClientNonColor += key;
			}
		}
		if (failServer) {
			for (String key : output2.keySet()) {
				String val = output2.get(key);
				if (!missingServer.isEmpty())
					missingServer += "§7, ";
				missingServer += "§5";
				missingServer += key;
				if (!val.isEmpty()) {
					missingServer += "§7 (§6";
					missingServer += val;
					missingServer += "§7)";
				}
				missingServer += "§7";

				if (!missingServerNonColor.isEmpty())
					missingServerNonColor += ", ";
				missingServerNonColor += key;
			}
		}

		if (failClient && !failServer) {
			HandshakeUtils.getImpl().logWarnModsClientOnly(this, output1, missingClientNonColor);

			HandshakeFailedPacket response = new HandshakeFailedPacket();
			response.failure = FailureType.PROTOCOL_REMOTE;
			response.language = "modkit.missingmods.clientonly";
			response.displayVersion = missingClient + "\n" + missingServer;
			response.version = 0d;
			response.write(getChannel());

			HandshakeUtils.getImpl().disconnectSimple(this, response.language, missingClient);
		} else if (!failClient && failServer) {
			HandshakeUtils.getImpl().logWarnModsServerOnly(this, output2, missingServerNonColor);

			HandshakeFailedPacket response = new HandshakeFailedPacket();
			response.failure = FailureType.PROTOCOL_REMOTE;
			response.language = "modkit.missingmods.serveronly";
			response.displayVersion = missingClient + "\n" + missingServer;
			response.version = 0d;
			response.write(getChannel());

			HandshakeUtils.getImpl().disconnectSimple(this, response.language, missingServer);
		} else if (failClient && failServer) {
			HandshakeUtils.getImpl().logWarnModsBothSides(this, output1, output2, missingClientNonColor,
					missingServerNonColor);

			HandshakeFailedPacket response = new HandshakeFailedPacket();
			response.failure = FailureType.PROTOCOL_REMOTE;
			response.language = "modkit.missingmods.both";
			response.displayVersion = missingClient + "\n" + missingServer;
			response.version = 0d;
			response.write(getChannel());

			HandshakeUtils.getImpl().disconnectSimple(this, response.language, missingClient, missingServer);
		} else {
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
			info(Colors.GOLD + "Player " + HandshakeUtils.getImpl().getPlayerName(this)
					+ " logged in with a CYAN client, " + mods + " mods installed.");
			HandshakeUtils.getImpl().switchStateConnected(this, true);
			getChannel().sendPacket("handshake.finish");
			HandshakeUtils.getImpl().dispatchFinishEvent(this);
		}
	}

}
