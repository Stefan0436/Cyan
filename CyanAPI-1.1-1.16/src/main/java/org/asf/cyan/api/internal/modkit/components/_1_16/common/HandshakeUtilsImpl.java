package org.asf.cyan.api.internal.modkit.components._1_16.common;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.events.network.CyanClientHandshakeEvent;
import org.asf.cyan.api.events.network.CyanServerHandshakeEvent;
import org.asf.cyan.api.events.network.ServerSideConnectedEvent;
import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;
import org.asf.cyan.api.events.objects.network.ServerConnectionEventObject;
import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.internal.ServerGamePacketListenerExtension;
import org.asf.cyan.api.internal.modkit.transformers._1_16.common.network.ServerStatusInterface;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.network.channels.ClientPacketProcessor;
import org.asf.cyan.api.network.channels.PacketChannel;
import org.asf.cyan.api.network.channels.ServerPacketProcessor;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeFailedPacket;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeLoaderPacket;
import org.asf.cyan.internal.modkitimpl.info.Protocols;
import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;
import org.asf.cyan.mods.dependencies.HandshakeRule;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.status.ClientStatusPacketListener;
import net.minecraft.network.protocol.status.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerboundPingRequestPacket;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class HandshakeUtilsImpl extends HandshakeUtils implements IModKitComponent {

	public static class ClientHandshakeUtils {

		private static class StatusResponseHandler implements ClientStatusPacketListener {

			private Connection conn;

			public StatusResponseHandler(Connection conn) {
				this.conn = conn;
			}

			@Override
			public Connection getConnection() {
				return conn;
			}

			@Override
			public void onDisconnect(Component arg0) {
				stop = true;
			}

			@Override
			public void handlePongResponse(ClientboundPongResponsePacket arg0) {
				conn.handleDisconnection();
				stop = true;
			}

			@Override
			public void handleStatusResponse(ClientboundStatusResponsePacket arg0) {
				ServerStatusInterface inter = (ServerStatusInterface) arg0.getStatus();
				response = inter.getJson();
				conn.send(new ServerboundPingRequestPacket(Util.getMillis()));
			}

			private JsonObject response = null;
			public int timeout = 5000;
			private boolean stop = false;

			public JsonObject getResponse() {
				int mili = 0;
				while (true) {
					if (mili == timeout / 10 || stop)
						return response;
					if (!conn.isConnected() && !conn.isConnecting()) {
						return response;
					}
					try {
						Thread.sleep(10);
						mili++;
					} catch (InterruptedException e) {
						break;
					}
				}
				return response;
			}

		}

		public static boolean begin(String ip, int port, Minecraft minecraft, Screen parent) {
			try {
				Connection conn = Connection.connectToServer(InetAddress.getByName(ip), port, false);
				StatusResponseHandler handler = new StatusResponseHandler(conn);
				conn.setListener(handler);
				conn.send(new ClientIntentionPacket(ip, port, ConnectionProtocol.STATUS));
				conn.send(new ServerboundStatusRequestPacket());
				JsonObject response = handler.getResponse();
				while (response == null) {
					conn.handleDisconnection();
					conn = Connection.connectToServer(InetAddress.getByName(ip), port, false);
					handler = new StatusResponseHandler(conn);
					conn.setListener(handler);
					conn.send(new ClientIntentionPacket(ip, port, ConnectionProtocol.STATUS));
					conn.send(new ServerboundStatusRequestPacket());
					response = handler.getResponse();
				}
				conn.handleDisconnection();
				if (response.has("modkit")) {
					JsonObject modkitData = response.get("modkit").getAsJsonObject();

					double serverProtocol = modkitData.get("protocol").getAsDouble();
					double serverMinProtocol = modkitData.get("protocol.min").getAsDouble();
					double serverMaxProtocol = modkitData.get("protocol.max").getAsDouble();

					int status = HandshakeUtils.getImpl().validateModKitProtocol(serverProtocol, serverMinProtocol,
							serverMaxProtocol, Protocols.MODKIT_PROTOCOL, Protocols.MIN_MODKIT, Protocols.MAX_MODKIT);
					if (status != 0) {
						final String failure;
						final Object[] args;
						if (status == 1) {
							failure = "modkit.protocol.outdated.remote";
							args = new Object[] { "§6" + Protocols.MODKIT_PROTOCOL, "§6" + serverMinProtocol,
									"§6" + serverMinProtocol };
							info("Connection failed: outdated server modkit protocol: " + serverProtocol
									+ ", client protocol: " + Protocols.MODKIT_PROTOCOL + " (min: "
									+ Protocols.MIN_MODKIT + ", max: " + Protocols.MAX_MODKIT + ")");
						} else {
							failure = "modkit.protocol.outdated.local";
							args = new Object[] { "§6" + Protocols.MODKIT_PROTOCOL, "§6" + serverMaxProtocol,
									"§6" + serverMaxProtocol };
							info("Connection failed: outdated client modkit protocol: " + Protocols.MODKIT_PROTOCOL
									+ ", server protocol: " + serverProtocol + " (min: " + serverMinProtocol + ", max: "
									+ serverMaxProtocol + ")");
						}
						new Thread(() -> {
							minecraft.execute(() -> {
								minecraft.setScreen(new DisconnectedScreen((Screen) parent,
										CommonComponents.CONNECT_FAILED, new TranslatableComponent(failure, args)));
							});
						}).start();
						return false;
					}

					JsonObject modloaderData = modkitData.get("modloader").getAsJsonObject();
					double loaderProtocol = modloaderData.get("protocol").getAsDouble();
					double loaderMinProtocol = modloaderData.get("protocol.min").getAsDouble();
					double loaderMaxProtocol = modloaderData.get("protocol.max").getAsDouble();
					String version = modloaderData.get("main").getAsJsonObject().get("version").getAsString();

					status = HandshakeUtils.getImpl().validateLoaderProtocol(loaderProtocol, loaderMinProtocol,
							loaderMaxProtocol, Protocols.LOADER_PROTOCOL, Protocols.MIN_LOADER, Protocols.MAX_LOADER);
					if (status != 0) {
						final String failure;
						final Object[] args;
						if (status == 2) {
							failure = "modkit.loader.outdated.local";
							args = new Object[] { version,
									Modloader.getModloader(CyanLoader.class).getVersion().toString(),
									Protocols.MIN_LOADER };
							info("Connection failed: outdated server modloader: " + serverProtocol + "(" + version + ")"
									+ ", client protocol: " + Protocols.LOADER_PROTOCOL + " ("
									+ Modloader.getModloaderVersion() + ", min: " + Protocols.MIN_LOADER + ", max: "
									+ Protocols.MAX_LOADER + ")");
						} else {
							failure = "modkit.loader.outdated.remote";
							args = new Object[] { version,
									Modloader.getModloader(CyanLoader.class).getVersion().toString(),
									Protocols.MAX_LOADER };
							info("Connection failed: outdated client modloader: " + Protocols.LOADER_PROTOCOL + "("
									+ Modloader.getModloaderVersion() + ")" + ", server protocol: " + serverProtocol
									+ " (" + version + ", min: " + serverMinProtocol + ", max: " + serverMaxProtocol
									+ ")");
						}
						new Thread(() -> {
							minecraft.execute(() -> {
								minecraft.setScreen(new DisconnectedScreen((Screen) parent,
										CommonComponents.CONNECT_FAILED, new TranslatableComponent(failure, args)));
							});
						}).start();
						return false;
					}

					HashMap<String, Version> remoteEntries = new HashMap<String, Version>();
					remoteEntries.put("game", Version
							.fromString(modloaderData.get("main").getAsJsonObject().get("game.version").getAsString()));
					remoteEntries.put("modloader", Version.fromString(version));

					JsonArray loaders = modloaderData.get("all").getAsJsonArray();
					for (JsonElement element : loaders) {
						JsonObject modloader = element.getAsJsonObject();
						JsonArray mods = modloader.get("mods").getAsJsonArray();
						JsonArray coremods = modloader.get("coremods").getAsJsonArray();

						for (JsonElement ele : mods) {
							JsonObject mod = ele.getAsJsonObject();
							remoteEntries.putIfAbsent(mod.get("id").getAsString(),
									Version.fromString(mod.get("version").getAsString()));
						}
						for (JsonElement ele : coremods) {
							JsonObject mod = ele.getAsJsonObject();
							remoteEntries.putIfAbsent(mod.get("id").getAsString(),
									Version.fromString(mod.get("version").getAsString()));
						}
					}

					HashMap<String, Version> localEntries = new HashMap<String, Version>();
					localEntries.put("game", Version.fromString(Modloader.getModloaderGameVersion()));
					localEntries.put("modloader", Modloader.getModloaderVersion());
					for (IModManifest mod : Modloader.getAllMods()) {
						localEntries.putIfAbsent(mod.id(), mod.version());
					}

					ArrayList<HandshakeRule> rules = new ArrayList<HandshakeRule>();
					JsonArray remoteRules = modkitData.get("rules").getAsJsonArray();
					for (JsonElement ele : remoteRules) {
						JsonObject ruleObject = ele.getAsJsonObject();
						rules.add(new HandshakeRule(GameSide.valueOf(ruleObject.get("side").getAsString()),
								ruleObject.get("key").getAsString(), ruleObject.get("checkstring").getAsString()));
					}
					HandshakeRule.getAllRules().forEach(rule -> {
						if (!rules.stream().anyMatch(t -> t.getKey().equals(rule.getKey())
								&& t.getCheckString().equals(rule.getCheckString()) && t.getSide() == rule.getSide())) {
							rules.add(rule);
						}
					});

					HashMap<String, String> output1 = new HashMap<String, String>();
					HashMap<String, String> output2 = new HashMap<String, String>();
					boolean failClient = !HandshakeRule.checkAll(localEntries, GameSide.CLIENT, output1, rules);
					boolean failServer = !HandshakeRule.checkAll(remoteEntries, GameSide.SERVER, output2, rules);

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

					if (failClient || failServer) {
						final String failure;
						final Object[] args;
						if (failClient && !failServer) {
							warn("Local client is missing " + output1.size() + " CYAN mods on the client. (mods: "
									+ missingClientNonColor + ")");

							failure = "modkit.missingmods.clientonly";
							args = new Object[] { missingClient };
						} else if (!failClient && failServer) {
							warn("Local client is missing " + output2.size() + " CYAN mods for the server. (mods: "
									+ missingServerNonColor + ")");

							failure = "modkit.missingmods.serveronly";
							args = new Object[] { missingServer };
						} else {
							warn("Local client is missing " + output2.size() + " CYAN mods for the server and "
									+ output1.size() + " CYAN mods on the client. (mods: " + missingClientNonColor
									+ ", server mods: " + missingServerNonColor + ")");

							failure = "modkit.missingmods.both";
							args = new Object[] { missingClient, missingServer };
						}
						new Thread(() -> {
							minecraft.execute(() -> {
								minecraft.setScreen(new DisconnectedScreen((Screen) parent,
										CommonComponents.CONNECT_FAILED, new TranslatableComponent(failure, args)));
							});
						}).start();
						return false;
					}
				} else if (HandshakeRule.getAllRules().stream().filter(t -> t.getSide() == GameSide.SERVER)
						.count() != 0) {
					new Thread(() -> {
						minecraft.execute(() -> {
							minecraft.setScreen(new DisconnectedScreen((Screen) parent, CommonComponents.CONNECT_FAILED,
									new TranslatableComponent("modkit.missingmodded.server")));
						});
					}).start();
					return false;
				}
			} catch (Exception e) {
				if (e instanceof UnknownHostException || e.getCause() instanceof ConnectException)
					return true;
			}
			return true;
		}

	}

	@Override
	public void initializeComponent() {
		impl = this;
	}

	@Override
	public UUID getUUID(Object player) {
		return ((ServerPlayer) player).getUUID();
	}

	@Override
	public boolean isServerRunning(ServerPacketProcessor processor) {
		return processor.getServer().isRunning();
	}

	@Override
	public boolean isUUIDPresentInPlayerList(ServerPacketProcessor processor, String key) {
		MinecraftServer srv = processor.getServer();
		return srv.getPlayerList().getPlayers().stream().anyMatch(t -> t.getUUID().toString().equals(key));
	}

	@Override
	public <T extends PacketChannel> T getChannel(Class<T> type, ClientConnectionEventObject event) {
		return PacketChannel.getChannel(type, event);
	}

	@Override
	public void dispatchConnectionEvent(ClientPacketProcessor processor) {
		CyanClientHandshakeEvent.getInstance().dispatch(new ClientConnectionEventObject(processor.getConnection(),
				processor.getClient(), processor.getServerBrand()));
	}

	@Override
	public void disconnect(ClientPacketProcessor processor, HandshakeFailedPacket response,
			HandshakeLoaderPacket packet) {
		processor.getChannel().getConnection().disconnect(new TranslatableComponent(response.language,
				packet.version.toString(), response.displayVersion, response.version));
	}

	@Override
	public UUID getUUID(ServerPacketProcessor processor) {
		return processor.getPlayer().getUUID();
	}

	@Override
	public void logWarnModsClientOnly(ServerPacketProcessor processor, HashMap<String, String> mods,
			String modsPretty) {
		warn("Player " + processor.getPlayer().getName().getString() + " is missing " + mods.size()
				+ " CYAN mods on the client. (mods: " + modsPretty + ")");
	}

	@Override
	public void disconnectColored1(ServerPacketProcessor processor, HandshakeFailedPacket response, double protocol) {
		synchronized (processor.getPlayer().connection) {
			processor.getPlayer().connection.disconnect(new TranslatableComponent(response.language, "§6" + protocol,
					"§6" + response.displayVersion, "§6" + response.version));
		}
	}

	@Override
	public void logWarnModsServerOnly(ServerPacketProcessor processor, HashMap<String, String> mods,
			String modsPretty) {
		warn("Player " + processor.getPlayer().getName().getString() + " is missing " + mods.size()
				+ " CYAN mods for the server. (mods: " + modsPretty + ")");
	}

	@Override
	public void logWarnModsBothSides(ServerPacketProcessor processor, HashMap<String, String> mods1,
			HashMap<String, String> mods2, String modsPretty1, String modsPretty2) {
		warn("Player " + processor.getPlayer().getName().getString() + " is missing " + mods2.size()
				+ " CYAN mods for the server and " + mods1.size() + " CYAN mods on the client. (mods: " + modsPretty1
				+ ", server mods: " + modsPretty2 + ")");
	}

	@Override
	public void disconnectSimple(ServerPacketProcessor processor, String lang, Object... args) {
		synchronized (processor.getPlayer().connection) {
			processor.getPlayer().connection.disconnect(new TranslatableComponent(lang, args));
		}
	}

	@Override
	public void switchStateConnected(ServerPacketProcessor processor, boolean state) {
		((ServerGamePacketListenerExtension) processor.getPlayer().connection).setConnectedCyan(true);
	}

	@Override
	public void dispatchFinishEvent(ServerPacketProcessor processor) {
		CyanServerHandshakeEvent.getInstance().dispatch(new ServerConnectionEventObject(processor.getConnection(),
				processor.getServer(), processor.getPlayer(), processor.getClientBrand())).getResult();
		ServerSideConnectedEvent.getInstance().dispatch(new ServerConnectionEventObject(processor.getConnection(),
				processor.getServer(), processor.getPlayer(), processor.getClientBrand()));
	}

	@Override
	public String getPlayerName(ServerPacketProcessor processor) {
		return processor.getPlayer().getName().getString();
	}

	@Override
	public Object getPlayerObject(ServerPacketProcessor processor) {
		return processor.getPlayer();
	}

	@Override
	public void onSerializeJson(JsonObject data) {
		JsonObject modkitData = new JsonObject();
		modkitData.addProperty("protocol", Protocols.MODKIT_PROTOCOL);
		modkitData.addProperty("protocol.min", Protocols.MIN_MODKIT);
		modkitData.addProperty("protocol.max", Protocols.MAX_MODKIT);

		JsonObject modloaderData = new JsonObject();
		modloaderData.addProperty("protocol", Protocols.LOADER_PROTOCOL);
		modloaderData.addProperty("protocol.min", Protocols.MIN_LOADER);
		modloaderData.addProperty("protocol.max", Protocols.MAX_LOADER);
		JsonObject main = new JsonObject();
		main.addProperty("name", Modloader.getModloader().getName());
		main.addProperty("version", Modloader.getModloader().getVersion().toString());
		main.addProperty("game.version", Modloader.getModloaderGameVersion());
		modloaderData.add("main", main);

		JsonArray loaders = new JsonArray();
		for (Modloader loader : Modloader.getAllModloaders()) {
			JsonObject modloader = new JsonObject();
			modloader.addProperty("name", loader.getName());
			modloader.addProperty("version", loader.getVersion().toString());
			modloader.addProperty("type", loader.getClass().getTypeName());

			modloader.addProperty("allmods.known.count", loader.getKnownModsCount());
			JsonArray mods = new JsonArray();
			for (IModManifest mod : loader.getLoadedMods()) {
				JsonObject modinfo = new JsonObject();
				modinfo.addProperty("id", mod.id());
				modinfo.addProperty("version", mod.version().toString());
				mods.add(modinfo);
			}
			modloader.add("mods", mods);
			JsonArray coremods = new JsonArray();
			for (IModManifest mod : loader.getLoadedCoremods()) {
				JsonObject modinfo = new JsonObject();
				modinfo.addProperty("id", mod.id());
				modinfo.addProperty("version", mod.version().toString());
				coremods.add(modinfo);
			}
			modloader.add("coremods", mods);

			loaders.add(modloader);
		}
		modloaderData.add("all", loaders);

		modkitData.add("modloader", modloaderData);

		JsonArray handshakeRules = new JsonArray();
		for (HandshakeRule rule : HandshakeRule.getAllRules()) {
			JsonObject ruleObject = new JsonObject();
			ruleObject.addProperty("key", rule.getKey());
			ruleObject.addProperty("checkstring", rule.getCheckString());
			ruleObject.addProperty("side", rule.getSide().toString());
			handshakeRules.add(ruleObject);
		}
		modkitData.add("rules", handshakeRules);

		data.add("modkit", modkitData);
	}

	@Override
	public boolean beginHandshake(String ip, int port, Object minecraft, Object parent) {
		return ClientHandshakeUtils.begin(ip, port, (Minecraft) minecraft, (Screen) parent);
	}

	@Override
	public int validateModKitProtocol(double serverProtocol, double serverMinProtocol, double serverMaxProtocol,
			double clientProtocol, double clientMinProtocol, double clientMaxProtocol) {
		if (clientProtocol < serverMinProtocol || (serverMaxProtocol != -1 && serverProtocol > clientMaxProtocol)) {
			return 1;
		} else if (clientProtocol > serverMaxProtocol
				|| (clientMinProtocol != -1 && serverProtocol < clientMinProtocol)) {
			return 2;
		}
		return 0;
	}

	@Override
	public int validateLoaderProtocol(double loaderProtocol, double loaderMinProtocol, double loaderMaxProtocol,
			double clientProtocol, double clientMinProtocol, double clientMaxProtocol) {
		if (loaderProtocol < clientMinProtocol || (loaderMinProtocol != -1 && clientProtocol < loaderMinProtocol)) {
			return 2;
		} else if (loaderProtocol > clientMaxProtocol
				|| (loaderMaxProtocol != -1 && clientProtocol > loaderMaxProtocol)) {
			return 1;
		}
		return 0;
	}

}
