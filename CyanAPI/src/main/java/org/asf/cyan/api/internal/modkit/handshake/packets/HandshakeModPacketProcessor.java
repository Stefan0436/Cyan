package org.asf.cyan.api.internal.modkit.handshake.packets;

import java.util.HashMap;

import org.asf.cyan.api.events.network.CyanServerHandshakeEvent;
import org.asf.cyan.api.events.objects.network.ServerConnectionEventObject;
import org.asf.cyan.api.internal.modkit.handshake.CyanHandshakePacketChannel;
import org.asf.cyan.api.internal.modkit.handshake.packets.content.HandshakeFailedPacket;
import org.asf.cyan.api.internal.modkit.handshake.packets.content.HandshakeModPacket;
import org.asf.cyan.api.internal.modkit.handshake.packets.content.HandshakeFailedPacket.FailureType;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.network.PacketReader;
import org.asf.cyan.api.network.channels.ServerPacketProcessor;
import org.asf.cyan.mods.dependencies.HandshakeRule;

import net.minecraft.network.chat.TranslatableComponent;

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

		HashMap<String, String> output1 = new HashMap<String, String>();
		HashMap<String, String> output2 = new HashMap<String, String>();
		boolean failClient = !HandshakeRule.checkAll(packet.entries, GameSide.CLIENT, output1, packet.remoteRules);
		boolean failServer = !HandshakeRule.checkAll(packet.entries, GameSide.SERVER, output2, packet.remoteRules);

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
			info("Player " + getPlayer().getName().getString() + " is missing " + output1.size()
					+ " Cyan mods on the client. (mods: " + missingClientNonColor + ")");

			HandshakeFailedPacket response = new HandshakeFailedPacket();
			response.failure = FailureType.PROTOCOL_REMOTE;
			response.language = "modkit.missingmods.clientonly";
			response.displayVersion = missingClient + "\n" + missingServer;
			response.version = 0d;
			response.write(getChannel());
			getPlayer().connection.tick();
			getPlayer().connection.disconnect(new TranslatableComponent(response.language, missingClient));
		} else if (!failClient && failServer) {
			info("Player " + getPlayer().getName().getString() + " is missing " + output2.size()
					+ " Cyan mods for the server. (mods: " + missingServerNonColor + ")");

			HandshakeFailedPacket response = new HandshakeFailedPacket();
			response.failure = FailureType.PROTOCOL_REMOTE;
			response.language = "modkit.missingmods.serveronly";
			response.displayVersion = missingClient + "\n" + missingServer;
			response.version = 0d;
			response.write(getChannel());
			getPlayer().connection.tick();
			getPlayer().connection.disconnect(new TranslatableComponent(response.language, missingServer));
		} else if (failClient && failServer) {
			info("Player " + getPlayer().getName().getString() + " is missing " + output2.size()
					+ " Cyan mods for the server and " + output1.size() + " Cyan mods on the client. (mods: "
					+ missingClientNonColor + ", server mods: " + missingServerNonColor + ")");

			HandshakeFailedPacket response = new HandshakeFailedPacket();
			response.failure = FailureType.PROTOCOL_REMOTE;
			response.language = "modkit.missingmods.both";
			response.displayVersion = missingClient + "\n" + missingServer;
			response.version = 0d;
			response.write(getChannel());
			getPlayer().connection.tick();
			getPlayer().connection
					.disconnect(new TranslatableComponent(response.language, missingClient, missingServer));
		} else {
			CyanHandshakePacketChannel.assignModloader(getPlayer(), packet.entries.get("game"),
					packet.clientProtocol, packet.entries.get("modloader"));
			
			for (String id : packet.entries.keySet()) {
				if (!id.equals("modloader") && !id.equals("game")) {
					CyanHandshakePacketChannel.assignMod(getPlayer(), id, packet.entries.get(id));
				}
			}

			getChannel().sendPacket("handshake.finish");
			CyanServerHandshakeEvent.getInstance().dispatch(
					new ServerConnectionEventObject(getConnection(), getServer(), getPlayer(), getClientBrand()))
					.getResult();
		}
	}

}
