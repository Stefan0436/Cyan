package org.asf.cyan.api.util.server.language;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;

import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import org.asf.cyan.api.internal.ServerGamePacketListenerExtension;
import org.asf.cyan.api.network.PacketWriter;
import org.asf.cyan.api.network.channels.PacketChannel;

/**
 * 
 * A utility that records client language information, so the server can provide
 * language components that have a fallback message for non-cyan clients.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ClientLanguage {
	private static class ClientLanguageNode {
		public ClientLanguageNode(String key, String fallback) {
			this.key = key;
			this.fallback = fallback;
		}

		public String key;
		public String fallback;
	}

	private static ArrayList<ClientLanguageNode> language = new ArrayList<ClientLanguageNode>();
	private static HashMap<String, ArrayList<String>> knownNodes = new HashMap<String, ArrayList<String>>();

	private static boolean watcherOnline = false;

	/**
	 * Registers a known langauge key
	 * 
	 * @param key      Language key
	 * @param fallback Fallback string
	 */
	public static void registerLanguageKey(String key, String fallback) {
		language.add(new ClientLanguageNode(key, fallback));
	}

	/**
	 * Creates a language or text component depending on the client presented keys
	 * 
	 * @param player      The player to retrieve the key of
	 * @param languageKey Language key to use, uses fallback if not present on
	 *                    client
	 * @param arguments   Language arguments
	 */
	public static BaseComponent createComponent(ServerPlayer player, String languageKey, Object... arguments) {
		if (player == null) {
			String message = language.stream().filter(t -> t.key.equals(languageKey)).findFirst().get().fallback;
			String newMessage = "";
			boolean last = false;
			int i = 0;
			for (char ch : message.toCharArray()) {
				if (ch == '%' && !last)
					last = true;
				else {
					if (last && (ch != 's' || i == arguments.length)) {
						newMessage += "%" + ch;
						last = false;
					} else if (last) {
						newMessage += arguments[i++].toString();
						last = false;
					} else {
						newMessage += ch;
					}
				}
			}
			return new TextComponent(newMessage);
		}
		String brand = ((ServerGamePacketListenerExtension) player.connection).getClientBrand();

		if (!language.stream().anyMatch(t -> t.key.equals(languageKey)))
			return new TextComponent("Missing server language node: " + languageKey);

		if (brand == null || !knownNodes.containsKey(player.getUUID().toString())
				|| !knownNodes.get(player.getUUID().toString()).contains(languageKey)) {
			String message = language.stream().filter(t -> t.key.equals(languageKey)).findFirst().get().fallback;
			String newMessage = "";
			boolean last = false;
			int i = 0;
			for (char ch : message.toCharArray()) {
				if (ch == '%' && !last)
					last = true;
				else {
					if (last && (ch != 's' || i == arguments.length)) {
						newMessage += "%" + ch;
						last = false;
					} else if (last) {
						newMessage += arguments[i++];
						last = false;
					} else {
						newMessage += ch;
					}
				}
			}
			return new TextComponent(newMessage);
		} else {
			return new TranslatableComponent(languageKey, arguments);
		}
	}

	/**
	 * Transmits the client keys to the server
	 */
	public static void writeKnownKeys(PacketChannel packetChannel) {
		PacketWriter.RawWriter writer = new PacketWriter.RawWriter(packetChannel.newPacket());
		writer.writeInt(language.size());
		for (ClientLanguageNode node : language) {
			writer.writeString(node.key);
		}
		packetChannel.sendPacket("cyan", "client.language.knownkeys", writer.getWriter());
	}

	/**
	 * Starts the player watcher thread that removes the keys of logged out players
	 */
	public static void startPlayerWatcher(MinecraftServer server) {
		if (watcherOnline)
			return;
		watcherOnline = true;
		new Thread(() -> {
			while (server.isRunning()) {
				while (true) {
					try {
						String[] keys = knownNodes.keySet().toArray(t -> new String[t]);
						for (String key : keys) {
							if (!server.getPlayerList().getPlayers().stream()
									.anyMatch(t -> t.getUUID().toString().equals(key))) {
								knownNodes.remove(key);
							}
						}
						break;
					} catch (ConcurrentModificationException | IndexOutOfBoundsException e) {
					}
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					break;
				}
				watcherOnline = false;
			}
		}, "ModKit Langauge Watcher Server Thread").start();
	}

	/**
	 * Assigns the known language keys of a player
	 * 
	 * @param player Player to assign the keys to
	 * @param keys   Language keys to use
	 */
	public static void setLanguageKeys(ServerPlayer player, List<String> keys) {
		knownNodes.put(player.getUUID().toString(), new ArrayList<String>(keys));
	}

	/**
	 * Removes a player from the server storage
	 * 
	 * @param player Server player to remove the language information of
	 */
	public static void removePlayer(ServerPlayer player) {
		if (knownNodes.containsKey(player.getUUID().toString()))
			knownNodes.remove(player.getUUID().toString());
	}
}
