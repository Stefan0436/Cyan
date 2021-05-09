package org.asf.cyan.api.internal.modkit.components._1_15_2.common.network.packets;

import java.util.ArrayList;
import java.util.function.Consumer;

import org.asf.cyan.api.events.network.EarlyCyanClientHandshakeEvent;
import org.asf.cyan.api.events.network.ServerSideConnectedEvent;
import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;
import org.asf.cyan.api.events.objects.network.ServerConnectionEventObject;
import org.asf.cyan.api.internal.modkit.components._1_15_2.common.network.packets.buffer.FriendlyByteBufInputFlow;
import org.asf.cyan.api.internal.modkit.handshake.CyanHandshakePacketChannel;
import org.asf.cyan.api.internal.modkit.transformers._1_15_2.common.network.ServerboundCustomPayloadPacketExtension;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.network.PacketReader;
import org.asf.cyan.api.network.channels.PacketChannel;
import org.asf.cyan.api.util.server.language.ClientLanguage;
import org.asf.cyan.mods.dependencies.HandshakeRule;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class NetworkHooks {
	public static boolean handlePayload(ClientboundCustomPayloadPacket packet, ResourceLocation type,
			FriendlyByteBuf buffer, Minecraft minecraft, Consumer<String> brandOutput, String serverBrand,
			Connection connection) {
		if (ClientboundCustomPayloadPacket.BRAND.equals(type)) {
			minecraft.player.setServerBrand(buffer.readUtf(32767));
			brandOutput.accept(minecraft.player.getServerBrand());
			return true;
		} else {
			if (type.getNamespace().equals("cyan") && type.getPath().equals("cyan.handshake.start")) {
				EarlyCyanClientHandshakeEvent.getInstance()
						.dispatch(new ClientConnectionEventObject(connection, minecraft, serverBrand)).getResult();
				return true;
			}
			PacketChannel ch = PacketChannel.getChannel(type.getNamespace(), () -> minecraft);
			if (ch != null) {
				if (serverBrand == null)
					brandOutput.accept("Cyan");

				if (ch.process(type.getPath(), new FriendlyByteBufInputFlow(buffer)))
					return true;
			}
		}
		return false;
	}

	public static boolean handlePayload(ServerboundCustomPayloadPacket packet, MinecraftServer server,
			Connection connection, ServerPlayer player, String clientBrand, Consumer<String> brandOutput) {

		FriendlyByteBuf data = ((ServerboundCustomPayloadPacketExtension) packet).readDataCyan();
		ResourceLocation id = ((ServerboundCustomPayloadPacketExtension) packet).getIdentifierCyan();
		if (id.getNamespace().equals("cyan") && id.getPath().equals("requestserverbrand")) {
			connection.send(new ClientboundCustomPayloadPacket(ClientboundCustomPayloadPacket.BRAND,
					new FriendlyByteBuf(Unpooled.buffer()).writeUtf(server.getServerModName())));
			return true;
		} else if (id.getNamespace().equals("cyan") && id.getPath().equals("client.language.knownkeys")) {
			PacketReader.RawReader reader = new PacketReader.RawReader(
					PacketReader.create(new FriendlyByteBufInputFlow(data)));
			ArrayList<String> keys = new ArrayList<String>();
			int count = reader.readInt();
			for (int i = 0; i < count; i++) {
				keys.add(reader.readString());
			}
			ClientLanguage.setLanguageKeys(player, keys);
			return true;
		}
		if (ServerboundCustomPayloadPacket.BRAND.equals(id)) {
			boolean first = clientBrand == null;
			clientBrand = data.readUtf(32767);
			brandOutput.accept(clientBrand);
			CyanHandshakePacketChannel.assignBrand(player, clientBrand);
			if (first) {
				ServerSideConnectedEvent.getInstance()
						.dispatch(new ServerConnectionEventObject(connection, server, player, clientBrand)).getResult();
				connection.send(new ClientboundCustomPayloadPacket(ClientboundCustomPayloadPacket.BRAND,
						new FriendlyByteBuf(Unpooled.buffer()).writeUtf(server.getServerModName())));
				if (clientBrand.startsWith("Cyan")) {
					connection.send(
							new ClientboundCustomPayloadPacket(new ResourceLocation("cyan", "cyan.handshake.start"),
									new FriendlyByteBuf(Unpooled.buffer(0))));
				} else if (HandshakeRule.getAllRules().stream().filter(t -> t.getSide() == GameSide.SERVER)
						.count() != 0) {
					player.connection.tick();
					player.connection.disconnect(new TextComponent(
							"§9This server runs a Cyan-like modloader and requires some client mods.\nPlease install a compatible modloader before trying again."));
					return true;
				}
			}

			return true;
		} else {
			PacketChannel ch = PacketChannel.getChannel(id.getNamespace(), player);
			if (ch != null) {
				if (ch.process(id.getPath(), new FriendlyByteBufInputFlow(data)))
					return true;
			}
		}

		return false;
	}
}
