package org.asf.cyan.api.internal.modkit.components._1_16.common;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.UUID;

import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.internal.ServerGamePacketListenerExtension;
import org.asf.cyan.api.internal.modkit.transformers._1_16.common.network.ServerStatusInterface;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeFailedPacket;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeLoaderPacket;
import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;

import com.google.gson.JsonObject;

import modkit.events.network.ModKitClientHandshakeEvent;
import modkit.events.network.ModKitServerHandshakeEvent;
import modkit.events.network.ServerSideConnectedEvent;
import modkit.events.objects.network.ClientConnectionEventObject;
import modkit.events.objects.network.ServerConnectionEventObject;
import modkit.network.channels.ClientPacketProcessor;
import modkit.network.channels.PacketChannel;
import modkit.network.channels.ServerPacketProcessor;
import modkit.protocol.handshake.Handshake;
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
				stop = true;
				conn.disconnect(new TranslatableComponent("multiplayer.status.finished"));
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
						conn.tick();
					} catch (InterruptedException e) {
						break;
					}
				}
				return response;
			}

		}

		public static boolean begin(Minecraft minecraft, Screen parent, String ip, int port) {
			try {
				Connection conn = Connection.connectToServer(InetAddress.getByName(ip), port, false);
				StatusResponseHandler handler = new StatusResponseHandler(conn);
				conn.setListener(handler);
				conn.send(new ClientIntentionPacket(ip, port, ConnectionProtocol.STATUS));
				conn.send(new ServerboundStatusRequestPacket());
				JsonObject response = handler.getResponse();
				return Handshake.earlyClientHandshake(response, minecraft, c -> {
					new Thread(() -> {
						minecraft.execute(() -> {
							minecraft.setScreen(new DisconnectedScreen(parent, CommonComponents.CONNECT_FAILED, c));
						});
					}).start();
				});
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
	public <T extends PacketChannel> T getChannel(Class<T> type, Object client) {
		return PacketChannel.getChannel(type, () -> (Minecraft) client);
	}

	@Override
	public void dispatchConnectionEvent(ClientPacketProcessor processor) {
		ModKitClientHandshakeEvent.getInstance().dispatch(new ClientConnectionEventObject(processor.getConnection(),
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
		ModKitServerHandshakeEvent.getInstance().dispatch(new ServerConnectionEventObject(processor.getConnection(),
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
	public boolean beginHandshake(Object client, Object parent, String ip, int port) {
		return ClientHandshakeUtils.begin((Minecraft) client, (Screen) parent, ip, port);
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
