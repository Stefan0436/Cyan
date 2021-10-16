package org.asf.cyan.internal.modkitimpl.handshake.processors;

import java.util.ArrayList;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeLoaderListPacket;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeRulesPacket;
import org.asf.cyan.internal.modkitimpl.util.ServerSoftwareImpl;

import modkit.network.PacketReader;
import modkit.network.channels.ClientPacketProcessor;
import modkit.protocol.handshake.HandshakeRule;
import modkit.util.client.ServerSoftware;
import modkit.util.remotedata.RemoteMod;

public class HandshakeLoaderListPacketProcessorClient extends ClientPacketProcessor {

	@Override
	public String id() {
		return "mods";
	}

	@Override
	protected void process(PacketReader content) {
		HandshakeLoaderListPacket list = new HandshakeLoaderListPacket().read(content);
		ServerSoftwareImpl.getCurrent().assignMods(list);

		RemoteMod[] serverMods = ServerSoftware.getInstance().getAllMods();
		IModManifest[] clientMods = Modloader.getAllMods();

		ArrayList<IModManifest> missingMods = new ArrayList<IModManifest>();
		for (IModManifest mod : clientMods) {
			boolean found = false;
			for (RemoteMod m : serverMods) {
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
			info("Attempting to connect to server with " + missingMods.size()
					+ " missing mods on current client. Read the debug log for more information.");
			debug("List of missing mods:");
			missingMods.forEach(t -> debug(" - " + t.id() + " (" + t.displayName() + ")"));
		}

		HandshakeRulesPacket rules = new HandshakeRulesPacket();
		rules.rules.addAll(HandshakeRule.getAllRules());
		rules.write(getChannel());
	}

}
