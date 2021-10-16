package org.asf.cyan.internal.modkitimpl.handshake.processors;

import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeRulesPacket;

import java.util.HashMap;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeFinishedPacket;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeResetPacket;
import org.asf.cyan.internal.modkitimpl.util.ClientSoftwareImpl;
import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;

import modkit.network.PacketReader;
import modkit.network.channels.ServerPacketProcessor;
import modkit.protocol.handshake.Handshake;
import modkit.protocol.handshake.Handshake.HandshakeFailure;
import modkit.protocol.handshake.HandshakeRule;
import modkit.util.remotedata.RemoteModloader;
import modkit.util.server.ClientSoftware;

public class HandshakeRulePacketProcessor extends ServerPacketProcessor {

	@Override
	public String id() {
		return "rules";
	}

	@Override
	@SuppressWarnings("static-access")
	protected void process(PacketReader content) {
		HandshakeRulesPacket rules = new HandshakeRulesPacket().read(content);
		HandshakeRule.getAllRules().forEach(rule -> {
			if (!rules.rules.stream().anyMatch(t -> t.getKey().equals(rule.getKey())
					&& t.getCheckString().equals(rule.getCheckString()) && t.getSide() == rule.getSide())) {
				rules.rules.add(rule);
			}
		});

		ClientSoftware client = ClientSoftware.getForUUID(HandshakeUtils.getImpl().getUUID(this));

		HashMap<String, Version> localEntries = new HashMap<String, Version>();
		localEntries.put("game", Version.fromString(Modloader.getModloaderGameVersion()));
		for (Modloader loader : Modloader.getAllModloaders()) {
			localEntries.put(loader.getName().toLowerCase(), loader.getVersion());
		}
		for (IModManifest mod : Modloader.getAllMods()) {
			localEntries.putIfAbsent(mod.id(), mod.version());
		}

		HashMap<String, Version> remoteEntries = new HashMap<String, Version>();
		if (client.getGameVersion() != null) {
			remoteEntries.put("game", client.getGameVersion());
		}
		for (RemoteModloader loader : client.getClientModloaders()) {
			remoteEntries.put(loader.getName().toLowerCase(), client.getGameVersion());
			remoteEntries.putAll(loader.getRuleEntries());
		}

		HandshakeFailure failure = Handshake.validateRules(remoteEntries, localEntries, rules.rules);
		if (failure != null) {
			final String msg;
			final Object[] args;

			if (failure.hasClientFailed && !failure.hasServerFailed) {
				info("Disconnecting player " + HandshakeUtils.getImpl().getPlayerName(this) + ", they are missing "
						+ failure.missingModsClient.size() + " mods on their client. (mods: "
						+ failure.missingModsClientMessageNonColor + ")");

				msg = "modkit.missingmods.clientonly";
				args = new Object[] { failure.missingModsClientMessage };
			} else if (!failure.hasClientFailed && failure.hasServerFailed) {
				info("Disconnecting player " + HandshakeUtils.getImpl().getPlayerName(this) + ", the server is missing "
						+ failure.missingModsServer.size() + " mods required by the client. (mods: "
						+ failure.missingModsServerMessageNonColor + ")");

				msg = "modkit.missingmods.serveronly";
				args = new Object[] { failure.missingModsServerMessage };
			} else {
				info("Disconnecting player " + HandshakeUtils.getImpl().getPlayerName(this) + ", missing "
						+ failure.missingModsServer.size() + " mods required by the client, missing "
						+ failure.missingModsClient.size() + " mods required by the server.");

				msg = "modkit.missingmods.both";
				args = new Object[] { failure.missingModsClientMessage, failure.missingModsServerMessage };
			}

			new HandshakeResetPacket().write(getChannel());
			HandshakeUtils.getImpl().disconnectSimple(this, msg, args);
		} else {
			((ClientSoftwareImpl) ClientSoftwareImpl.getForUUID(HandshakeUtils.getImpl().getUUID(this)))
					.completeHandshake(this);
			new HandshakeFinishedPacket().write(getChannel());
		}
	}

}
