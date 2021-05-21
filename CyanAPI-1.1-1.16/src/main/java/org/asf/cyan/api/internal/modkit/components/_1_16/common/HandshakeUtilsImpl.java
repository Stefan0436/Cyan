package org.asf.cyan.api.internal.modkit.components._1_16.common;

import java.util.HashMap;
import java.util.UUID;

import org.asf.cyan.api.events.network.CyanClientHandshakeEvent;
import org.asf.cyan.api.events.network.CyanServerHandshakeEvent;
import org.asf.cyan.api.events.network.ServerSideConnectedEvent;
import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;
import org.asf.cyan.api.events.objects.network.ServerConnectionEventObject;
import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.internal.ServerGamePacketListenerExtension;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.network.channels.ClientPacketProcessor;
import org.asf.cyan.api.network.channels.PacketChannel;
import org.asf.cyan.api.network.channels.ServerPacketProcessor;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeFailedPacket;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeLoaderPacket;
import org.asf.cyan.internal.modkitimpl.info.Protocols;
import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class HandshakeUtilsImpl extends HandshakeUtils implements IModKitComponent {

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
		processor.getPlayer().connection.disconnect(new TranslatableComponent(response.language, "§6" + protocol,
				"§6" + response.displayVersion, "§6" + response.version));
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
		processor.getPlayer().connection.disconnect(new TranslatableComponent(lang, args));
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
		data.add("modkit", modkitData);
	}

}
