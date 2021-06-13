package org.asf.cyan.api.internal.modkit.components._1_16.common.network;

import java.util.ArrayList;
import java.util.function.Consumer;

import org.asf.cyan.api.events.network.EarlyCyanClientHandshakeEvent;
import org.asf.cyan.api.events.network.ServerSideConnectedEvent;
import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;
import org.asf.cyan.api.events.objects.network.ServerConnectionEventObject;
import org.asf.cyan.api.internal.modkit.components._1_16.common.network.buffer.FriendlyByteBufInputFlow;
import org.asf.cyan.api.internal.modkit.transformers._1_16.common.network.ServerboundCustomPayloadPacketExtension;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.network.PacketReader;
import org.asf.cyan.api.network.channels.PacketChannel;
import org.asf.cyan.api.util.server.language.ClientLanguage;
import org.asf.cyan.internal.modkitimpl.util.ClientImpl;
import org.asf.cyan.mods.dependencies.HandshakeRule;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
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
			String brand = buffer.readUtf(32767);

			minecraft.player.setServerBrand(brand);
			brandOutput.accept(brand);

			if (!brand.startsWith("Cyan")
					&& HandshakeRule.getAllRules().stream().filter(t -> t.getSide() == GameSide.SERVER).count() != 0) {
				minecraft.getConnection().getConnection()
						.disconnect(new TranslatableComponent("modkit.missingmodded.server"));
			}
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
			Connection connection, ServerPlayer player, String clientBrand, Consumer<String> brandOutput,
			Consumer<Boolean> connectedOutput) {

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
			ClientImpl.assignBrand(player.getUUID(), clientBrand);
			ClientImpl.assignPlayerObjects(player.getName().getString(), player.getUUID(), player);
			
			if (first) {
				connection.send(new ClientboundCustomPayloadPacket(ClientboundCustomPayloadPacket.BRAND,
						new FriendlyByteBuf(Unpooled.buffer()).writeUtf(server.getServerModName())));
				if (clientBrand.startsWith("Cyan")) {
					connection.send(
							new ClientboundCustomPayloadPacket(new ResourceLocation("cyan", "cyan.handshake.start"),
									new FriendlyByteBuf(Unpooled.buffer(0))));
				} else if (HandshakeRule.getAllRules().stream().filter(t -> t.getSide() == GameSide.CLIENT)
						.count() != 0) {
					player.connection.tick();
					player.connection.disconnect(new TextComponent(
							"\u00A79This server runs a Cyan-like modloader and requires some client mods.\nPlease install a compatible modloader before trying again."));
					return true;
				} else {
					ServerSideConnectedEvent.getInstance()
							.dispatch(new ServerConnectionEventObject(connection, server, player, clientBrand))
							.getResult();
					connectedOutput.accept(true);
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
