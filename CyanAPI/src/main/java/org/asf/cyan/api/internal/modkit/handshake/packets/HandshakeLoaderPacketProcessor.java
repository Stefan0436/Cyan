package org.asf.cyan.api.internal.modkit.handshake.packets;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.internal.modkit.handshake.packets.content.HandshakeFailedPacket;
import org.asf.cyan.api.internal.modkit.handshake.packets.content.HandshakeLoaderPacket;
import org.asf.cyan.api.internal.modkit.handshake.packets.content.HandshakeModPacket;
import org.asf.cyan.api.internal.modkit.handshake.packets.content.HandshakeFailedPacket.FailureType;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.network.PacketReader;
import org.asf.cyan.api.network.channels.ClientPacketProcessor;
import org.asf.cyan.api.versioning.Version;

import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.network.chat.TranslatableComponent;

public class HandshakeLoaderPacketProcessor extends ClientPacketProcessor {

	private final static double minimalLoader = 0.13d; // Cyan 1.0.0.A13
	private final static double maximalLoader = 1.0d; // Cyan 1.0.0 Release LTS

	public static final double PROTOCOL = 0.13d; // Current

	@Override
	public String id() {
		return "loader";
	}

	@Override
	@SuppressWarnings("resource")
	protected void process(PacketReader reader) {
		if (getClient().screen == null || !(getClient().screen instanceof ReceivingLevelScreen))
			getClient().setScreen(new ReceivingLevelScreen());

		HandshakeLoaderPacket packet = new HandshakeLoaderPacket().read(reader);
		Version version = Modloader.getModloader(CyanLoader.class).getVersion();

		if (packet.protocol < minimalLoader) {
			HandshakeFailedPacket response = new HandshakeFailedPacket();
			response.failure = FailureType.LOADER_LOCAL;
			response.language = "modkit.loader.outdated.local";
			response.displayVersion = version.toString();
			response.version = minimalLoader;
			response.write(getChannel());
			getChannel().getConnection().disconnect(new TranslatableComponent(response.language,
					packet.version.toString(), response.displayVersion, response.version));
		} else if (packet.protocol > maximalLoader) {
			HandshakeFailedPacket response = new HandshakeFailedPacket();
			response.failure = FailureType.LOADER_REMOTE;
			response.language = "modkit.loader.outdated.remote";
			response.displayVersion = version.toString();
			response.version = maximalLoader;
			response.write(getChannel());
			getChannel().getConnection().disconnect(new TranslatableComponent(response.language,
					packet.version.toString(), response.displayVersion, response.version));
		} else {
			HandshakeModPacket response = new HandshakeModPacket();
			response.clientProtocol = PROTOCOL;
			
			response.entries.put("game", Version.fromString(Modloader.getModloaderGameVersion()));
			response.entries.put("modloader", Modloader.getModloader(CyanLoader.class).getVersion());
			for (IModManifest mod : Modloader.getAllMods()) {
				response.entries.putIfAbsent(mod.id(), mod.version());
			}
			
			response.write(getChannel());
		}
	}

}
