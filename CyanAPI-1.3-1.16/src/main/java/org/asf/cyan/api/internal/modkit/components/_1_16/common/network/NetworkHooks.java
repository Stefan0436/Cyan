package org.asf.cyan.api.internal.modkit.components._1_16.common.network;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.ArrayList;
import java.util.function.Consumer;

import org.asf.cyan.api.internal.modkit.components._1_16.common.network.buffer.FriendlyByteBufInputFlow;
import org.asf.cyan.api.internal.modkit.components._1_16.common.network.buffer.FriendlyByteBufOutputFlow;
import org.asf.cyan.api.internal.modkit.transformers._1_16.common.network.ServerboundCustomPayloadPacketExtension;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.internal.modkitimpl.HandshakeComponent;
import org.asf.cyan.internal.modkitimpl.util.ClientImpl;

import io.netty.buffer.Unpooled;
import modkit.events.network.ServerSideConnectedEvent;
import modkit.events.objects.network.ServerConnectionEventObject;
import modkit.network.PacketReader;
import modkit.network.PacketWriter;
import modkit.network.channels.PacketChannel;
import modkit.protocol.handshake.HandshakeRule;
import modkit.util.server.language.ClientLanguage;
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
	private static ArrayList<SplitPacket> splitPackets = new ArrayList<SplitPacket>();
	
	private static class SplitPacket {
		public static SplitPacket getPacket(long id, int tick) {
			while (true) {
				try {
					for (SplitPacket pk : splitPackets) {
						if (pk.id == id && pk.tick == tick)
							return pk;
					}
					break;
				} catch (ConcurrentModificationException e) {
				}
			}
			return null;
		}

		public long age = System.currentTimeMillis();
		public ArrayList<byte[]> payloads = new ArrayList<byte[]>();

		public String channel;
		public long id;
		public int tick;
		public int count;
		public int received = 0;
		public int size;

		public void start(MinecraftServer server) {
			splitPackets.add(this);
			Thread th = new Thread(() -> {
				while (server == null || server.isRunning()) {
					while (true) {
						try {
							if (!splitPackets.contains(this))
								break;
							long age = System.currentTimeMillis() - this.age;
							if (age > 5000) {
								while (true) {
									try {
										if (splitPackets.contains(this))
											splitPackets.remove(this);
										break;
									} catch (ConcurrentModificationException e) {
									}
								}
							}
							try {
								Thread.sleep(1);
							} catch (InterruptedException e) {
							}
							break;
						} catch (ConcurrentModificationException e) {
						}
					}
				}
			}, "Split Packet Timeout Thread");
			th.setDaemon(true);
			th.start();
		}
	}

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
			if (type.getNamespace().equals("modkit") && type.getPath().equals("splitpacket")) {
				PacketReader.RawReader reader = new PacketReader.RawReader(
						PacketReader.create(new FriendlyByteBufInputFlow(buffer)));
				byte typeB = reader.readSingleByte();
				switch (typeB) {
				case 0:

					SplitPacket header = new SplitPacket();
					header.channel = reader.readVarString();
					header.id = reader.readLong();
					header.tick = reader.readInt();
					header.count = reader.readVarInt();
					header.size = reader.readVarInt();
					for (int i = 0; i < header.count; i++)
						header.payloads.add(null);

					if (SplitPacket.getPacket(header.id, header.tick) != null)
						connection.disconnect(new TextComponent("Duplicate packet received"));
					else
						header.start(null);

					break;
				case 1:

					long id = reader.readLong();
					int tick = reader.readInt();
					int index = reader.readInt();
					byte[] payload = reader.readAllBytes();

					SplitPacket pk = SplitPacket.getPacket(id, tick);
					if (pk == null)
						connection.disconnect(
								new TextComponent("Unrecognized split packet received. ID: " + id + ", tick: " + tick));
					else {
						if (pk.count <= index) {
							connection.disconnect(new TextComponent("Index out of bounds: " + index + " of " + pk.count
									+ ", split packet ID: " + id + ", tick: " + tick));
						} else {
							if (index < pk.payloads.size() && index >= 0 && pk.payloads.get(index) == null) {
								pk.payloads.set(index, payload);
								pk.received++;
								pk.age = System.currentTimeMillis();
								if (pk.received == pk.count) {
									while (true) {
										try {
											if (splitPackets.contains(pk))
												splitPackets.remove(pk);
											break;
										} catch (ConcurrentModificationException e) {
										}
									}

									int i = 0;
									byte[] data = new byte[pk.size];
									for (byte[] payloadD : pk.payloads) {
										for (byte b : payloadD)
											data[i++] = b;
									}
									if (i != pk.size) {
										connection.disconnect(new TextComponent(
												"Malformed split packet received, size incorrect: expected: " + pk.size
														+ ", got: " + i));
									} else {
										ClientboundCustomPayloadPacket payloadPK = new ClientboundCustomPayloadPacket(
												ResourceLocation.tryParse(pk.channel),
												new FriendlyByteBuf(Unpooled.copiedBuffer(data)));
										minecraft.getConnection().handleCustomPayload(payloadPK);
									}
								}
							} else if (index > pk.payloads.size() || index < 0)
								connection.disconnect(new TextComponent("Malformed packet received"));
							else
								connection.disconnect(new TextComponent("Duplicate packet received"));
						}
					}

					break;
				default:
					connection.disconnect(new TextComponent("Malformed packet received"));
					break;
				}
				return true;
			} else if (type.getNamespace().equals("cyan") && type.getPath().equals("cyan.handshake.start")) {
				HandshakeComponent.handshakeStartClient(minecraft);
				return true;
			}
			for (PacketChannel ch : PacketChannel.getChannels(type.getNamespace(), () -> minecraft)) {
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

		FriendlyByteBuf buffer = ((ServerboundCustomPayloadPacketExtension) packet).readDataCyan();
		ResourceLocation type = ((ServerboundCustomPayloadPacketExtension) packet).getIdentifierCyan();
		if (type.getNamespace().equals("modkit") && type.getPath().equals("splitpacket")) {
			PacketReader.RawReader reader = new PacketReader.RawReader(
					PacketReader.create(new FriendlyByteBufInputFlow(buffer)));
			byte typeB = reader.readSingleByte();
			switch (typeB) {
			case 0:

				SplitPacket header = new SplitPacket();
				header.channel = reader.readVarString();
				header.id = reader.readLong();
				header.tick = reader.readInt();
				header.count = reader.readVarInt();
				header.size = reader.readVarInt();
				for (int i = 0; i < header.count; i++)
					header.payloads.add(null);

				if (SplitPacket.getPacket(header.id, header.tick) != null)
					connection.disconnect(new TextComponent("Duplicate packet received"));
				else
					header.start(null);

				break;
			case 1:

				long id = reader.readLong();
				int tick = reader.readInt();
				int index = reader.readInt();
				byte[] payload = reader.readAllBytes();

				SplitPacket pk = SplitPacket.getPacket(id, tick);
				if (pk == null)
					connection.disconnect(
							new TextComponent("Unrecognized split packet received. ID: " + id + ", tick: " + tick));
				else {
					if (pk.count <= index) {
						connection.disconnect(new TextComponent("Index out of bounds: " + index + " of " + pk.count
								+ ", split packet ID: " + id + ", tick: " + tick));
					} else {
						if (index < pk.payloads.size() && index >= 0 && pk.payloads.get(index) == null) {
							pk.payloads.set(index, payload);
							pk.received++;
							pk.age = System.currentTimeMillis();
							if (pk.received == pk.count) {
								while (true) {
									try {
										if (splitPackets.contains(pk))
											splitPackets.remove(pk);
										break;
									} catch (ConcurrentModificationException e) {
									}
								}

								int i = 0;
								byte[] data = new byte[pk.size];
								for (byte[] payloadD : pk.payloads) {
									for (byte b : payloadD)
										data[i++] = b;
								}
								if (i != pk.size) {
									connection.disconnect(new TextComponent(
											"Malformed split packet received, size incorrect: expected: " + pk.size
													+ ", got: " + i));
								} else {
									ServerboundCustomPayloadPacket payloadPK = new ServerboundCustomPayloadPacket(
											ResourceLocation.tryParse(pk.channel),
											new FriendlyByteBuf(Unpooled.copiedBuffer(data)));
									player.connection.handleCustomPayload(payloadPK);
								}
							}
						} else if (index > pk.payloads.size() || index < 0)
							connection.disconnect(new TextComponent("Malformed packet received"));
						else
							connection.disconnect(new TextComponent("Duplicate packet received"));
					}
				}

				break;
			default:
				connection.disconnect(new TextComponent("Malformed packet received"));
				break;
			}
			return true;
		} else if (type.getNamespace().equals("cyan") && type.getPath().equals("requestserverbrand")) {
			connection.send(new ClientboundCustomPayloadPacket(ClientboundCustomPayloadPacket.BRAND,
					new FriendlyByteBuf(Unpooled.buffer()).writeUtf(server.getServerModName())));
			return true;
		} else if (type.getNamespace().equals("cyan") && type.getPath().equals("client.language.knownkeys")) {
			PacketReader.RawReader reader = new PacketReader.RawReader(
					PacketReader.create(new FriendlyByteBufInputFlow(buffer)));
			ArrayList<String> keys = new ArrayList<String>();
			int count = reader.readInt();
			for (int i = 0; i < count; i++) {
				keys.add(reader.readString());
			}
			ClientLanguage.setLanguageKeys(player, keys);
			return true;
		}
		if (ServerboundCustomPayloadPacket.BRAND.equals(type)) {
			boolean first = clientBrand == null;
			clientBrand = buffer.readUtf(32767);

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
			for (PacketChannel ch : PacketChannel.getChannels(type.getNamespace(), player)) {
				if (ch.process(type.getPath(), new FriendlyByteBufInputFlow(buffer)))
					return true;
			}
		}

		return false;
	}

	public static void splitSendServer(String channel, byte[] data, Connection connection, long id, int tick) {
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
		}

		SplitPacket pk = new SplitPacket();
		pk.tick = tick;
		pk.id = id;
		pk.channel = channel;
		pk.size = data.length;

		int i = 0;
		while (i != data.length) {
			byte[] payload = new byte[(data.length - i) > 32750 ? 32750 : (data.length - i)];
			payload = Arrays.copyOfRange(data, i, i + payload.length);
			i += payload.length;
			pk.payloads.add(payload);
		}

		pk.count = pk.payloads.size();

		FriendlyByteBufOutputFlow flow = new FriendlyByteBufOutputFlow();
		PacketWriter.RawWriter wr = new PacketWriter.RawWriter(PacketWriter.create(flow));
		wr.writeByte((byte) 0);
		wr.writeVarString(pk.channel);
		wr.writeLong(pk.id);
		wr.writeInt(pk.tick);
		wr.writeVarInt(pk.count);
		wr.writeVarInt(pk.size);

		ServerboundCustomPayloadPacket packet = new ServerboundCustomPayloadPacket(
				new ResourceLocation("modkit", "splitpacket"), flow.toBuffer());
		connection.send(packet);

		i = 0;
		for (byte[] payload : pk.payloads) {
			flow = new FriendlyByteBufOutputFlow();

			wr = new PacketWriter.RawWriter(PacketWriter.create(flow));
			wr.writeByte((byte) 1);
			wr.writeLong(pk.id);
			wr.writeInt(pk.tick);
			wr.writeInt(i++);
			wr.writeBytes(payload);

			packet = new ServerboundCustomPayloadPacket(new ResourceLocation("modkit", "splitpacket"), flow.toBuffer());
			connection.send(packet);
		}

		flow.close();
		pk.payloads.clear();
	}

	public static void splitSendClient(String channel, byte[] data, Connection connection, long id, int tick) {
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
		}

		SplitPacket pk = new SplitPacket();
		pk.tick = tick;
		pk.id = id;
		pk.channel = channel;
		pk.size = data.length;

		int i = 0;
		while (i != data.length) {
			byte[] payload = new byte[(data.length - i) > 32750 ? 32750 : (data.length - i)];
			payload = Arrays.copyOfRange(data, i, i + payload.length);
			i += payload.length;
			pk.payloads.add(payload);
		}

		pk.count = pk.payloads.size();

		FriendlyByteBufOutputFlow flow = new FriendlyByteBufOutputFlow();
		PacketWriter.RawWriter wr = new PacketWriter.RawWriter(PacketWriter.create(flow));
		wr.writeByte((byte) 0);
		wr.writeVarString(pk.channel);
		wr.writeLong(pk.id);
		wr.writeInt(pk.tick);
		wr.writeVarInt(pk.count);
		wr.writeVarInt(pk.size);

		ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(
				new ResourceLocation("modkit", "splitpacket"), flow.toBuffer());
		connection.send(packet);

		i = 0;
		for (byte[] payload : pk.payloads) {
			flow.close();
			flow = new FriendlyByteBufOutputFlow();

			wr = new PacketWriter.RawWriter(PacketWriter.create(flow));
			wr.writeByte((byte) 1);
			wr.writeLong(pk.id);
			wr.writeInt(pk.tick);
			wr.writeInt(i++);
			wr.writeBytes(payload);

			packet = new ClientboundCustomPayloadPacket(new ResourceLocation("modkit", "splitpacket"), flow.toBuffer());
			connection.send(packet);
		}

		flow.close();
		pk.payloads.clear();
	}
}
