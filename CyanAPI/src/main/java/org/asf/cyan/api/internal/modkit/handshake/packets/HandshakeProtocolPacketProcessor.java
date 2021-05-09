package org.asf.cyan.api.internal.modkit.handshake.packets;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.internal.modkit.handshake.CyanHandshakePacketChannel;
import org.asf.cyan.api.internal.modkit.handshake.packets.content.HandshakeFailedPacket;
import org.asf.cyan.api.internal.modkit.handshake.packets.content.HandshakeLoaderPacket;
import org.asf.cyan.api.internal.modkit.handshake.packets.content.HandshakeProtocolPacket;
import org.asf.cyan.api.internal.modkit.handshake.packets.content.HandshakeFailedPacket.FailureType;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.network.PacketReader;
import org.asf.cyan.api.network.channels.ServerPacketProcessor;

import net.minecraft.network.chat.TranslatableComponent;

public class HandshakeProtocolPacketProcessor extends ServerPacketProcessor {

	//
	// ModKit Protocol 1.0, support until 2.0
	// ModKit 1.0 LTS Version
	//
	// Protocol 2.0 will most likely be different
	//
	private final static double minimalProtocolVersion = 1.0d; // ModKit 1.0
	private final static double maximalProtocolVersion = 2.0d; // ModKit 2.0

	public static final double PROTOCOL = 1.0d; // Current

	//
	// Permanent (raw) format:
	// integer - error
	// double - version
	// string - language key
	// string - display version
	//
	// Error 0: outdated local protocol, remote newer
	// Error 1: outdated remote protocol, local newer
	// Error 2: outdated local loader, remote newer
	// Error 3: outdated remote loader, local newer
	//
	// Protocol Negotiation uses RawReader or plain reading to receive
	// Protocol Negotiation uses RawWriter or plain writing to write
	//
	// NIO ByteBuffer is used by the underlying RawWriter and RawReader
	// CyanUtil Packets are used by the packet writer. (flow format)
	//

	@Override
	public String id() {
		return "core";
	}

	@Override
	protected void process(PacketReader content) {
		CyanHandshakePacketChannel.startInfoHandler(getServer());
		HandshakeProtocolPacket packet = new HandshakeProtocolPacket().read(content);
		if (packet.protocolVersion < minimalProtocolVersion) {
			HandshakeFailedPacket response = new HandshakeFailedPacket();
			response.failure = FailureType.PROTOCOL_REMOTE;
			response.language = "modkit.protocol.outdated.remote";
			response.displayVersion = Double.toString(minimalProtocolVersion);
			response.version = minimalProtocolVersion;
			response.write(getChannel());
			getChannel().getConnection().tick();
			getPlayer().connection.disconnect(new TranslatableComponent(response.language,
					"§6" + packet.protocolVersion, "§6" + response.displayVersion, "§6" + response.version));
		} else if (packet.protocolVersion > maximalProtocolVersion) {
			HandshakeFailedPacket response = new HandshakeFailedPacket();
			response.failure = FailureType.PROTOCOL_LOCAL;
			response.language = "modkit.protocol.outdated.local";
			response.displayVersion = Double.toString(maximalProtocolVersion);
			response.version = maximalProtocolVersion;
			response.write(getChannel());
			getChannel().getConnection().tick();
			getPlayer().connection.disconnect(new TranslatableComponent(response.language,
					"§6" + packet.protocolVersion, "§6" + response.displayVersion, "§6" + response.version));
		} else {
			CyanHandshakePacketChannel.assignProtocol(getPlayer(), packet.protocolVersion);
			
			HandshakeLoaderPacket response = new HandshakeLoaderPacket();
			response.protocol = HandshakeLoaderPacketProcessor.PROTOCOL;
			response.version = Modloader.getModloader(CyanLoader.class).getVersion();
			response.write(getChannel());
			getChannel().getConnection().tick();
		}
	}

}
