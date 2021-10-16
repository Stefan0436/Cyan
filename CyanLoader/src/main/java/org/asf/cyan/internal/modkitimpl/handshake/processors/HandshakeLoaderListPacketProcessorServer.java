package org.asf.cyan.internal.modkitimpl.handshake.processors;

import java.util.ArrayList;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeLoaderListPacket;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeResetPacket;
import org.asf.cyan.internal.modkitimpl.util.ClientSoftwareImpl;
import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;

import modkit.network.PacketReader;
import modkit.network.channels.ServerPacketProcessor;
import modkit.protocol.handshake.Handshake;
import modkit.protocol.ModKitModloader.ModKitProtocolRules;
import modkit.util.remotedata.RemoteMod;
import modkit.util.remotedata.RemoteModloader;
import modkit.util.server.ClientSoftware;

public class HandshakeLoaderListPacketProcessorServer extends ServerPacketProcessor {

	@Override
	public String id() {
		return "mods";
	}

	@Override
	protected void process(PacketReader content) {
		HandshakeLoaderListPacket list = new HandshakeLoaderListPacket().read(content);
		ClientSoftwareImpl.setModInfo(this, list);

		for (RemoteModloader loader : ClientSoftware.getForUUID(HandshakeUtils.getImpl().getUUID(this))
				.getClientModloaders()) {
			if (loader.getModloaderProtocolVersion() != -1) {
				Modloader ld = loader.findInstance();

				if (ld == null || !(ld instanceof ModKitProtocolRules)) {
					new HandshakeResetPacket().write(getChannel());
					info("Disconnecting player " + HandshakeUtils.getImpl().getPlayerName(this)
							+ " as they are missing a required '" + loader.getName() + "' modloader");
					HandshakeUtils.getImpl().disconnectSimple(this, "modkit.missingmodded.client.loader",
							"\u00A76" + loader.getName());
					return;
				}

				ModKitProtocolRules rules = (ModKitProtocolRules) ld;
				int status = Handshake.validateLoaderProtocol(rules.modloaderProtocol(), rules.modloaderMinProtocol(),
						rules.modloaderMaxProtocol(), loader.getModloaderProtocolVersion(),
						loader.getModloaderMinProtocolVersion(), loader.getModloaderMaxProtocolVersion());

				String failure = null;
				Object[] args = null;
				if (status == 2) {
					failure = "modkit.loader.outdated.local";
					args = new Object[] { ld.getVersion().toString(), loader.getVersion().toString(),
							loader.getModloaderMinProtocolVersion() };
					info("Player connection failed: " + HandshakeUtils.getImpl().getPlayerName(this)
							+ " needs an older client modloader: " + rules.modloaderProtocol() + " ("
							+ ld.getVersion().toString() + ")" + ", client protocol: "
							+ loader.getModloaderProtocolVersion() + " (" + loader.getVersion().toString() + ", min: "
							+ loader.getModloaderMinProtocolVersion() + ", max: "
							+ loader.getModloaderMaxProtocolVersion() + ")");
				} else if (status == 1) {
					failure = "modkit.loader.outdated.remote";
					args = new Object[] { ld.getVersion().toString(), loader.getVersion().toString(),
							loader.getModloaderMaxProtocolVersion() };

					info("Player connection failed: " + HandshakeUtils.getImpl().getPlayerName(this)
							+ " uses outdated client modloader: " + loader.getModloaderProtocolVersion() + " ("
							+ ld.getVersion().toString() + ")" + ", server protocol: " + rules.modloaderProtocol()
							+ " (" + loader.getVersion().toString() + ", min: " + rules.modloaderMinProtocol()
							+ ", max: " + rules.modloaderMaxProtocol() + ")");
				}

				if (failure != null) {
					new HandshakeResetPacket().write(getChannel());
					HandshakeUtils.getImpl().disconnectSimple(this, failure, args);
					return;
				}
			}
		}

		RemoteMod[] clientMods = ClientSoftware.getForUUID(HandshakeUtils.getImpl().getUUID(this)).getAllMods();
		IModManifest[] serverMods = Modloader.getAllMods();

		ArrayList<IModManifest> missingMods = new ArrayList<IModManifest>();
		for (IModManifest mod : serverMods) {
			boolean found = false;
			for (RemoteMod m : clientMods) {
				if (m.getModID().equals(mod.id())) {
					found = true;
					break;
				}
			}
			if (!found) {
				missingMods.add(mod);
			}
		}

		if (missingMods.size() != 0) {
			info("Attempting to accept connection of " + HandshakeUtils.getImpl().getPlayerName(this) + " with "
					+ missingMods.size() + " missing mods on the server. Read the debug log for more information.");
			debug("List of missing mods:");
			missingMods.forEach(t -> debug(" - " + t.id() + " (" + t.displayName() + ")"));
		}

		HandshakeLoaderListPacket mods = new HandshakeLoaderListPacket();
		mods.fill();
		mods.write(getChannel());
	}

}
