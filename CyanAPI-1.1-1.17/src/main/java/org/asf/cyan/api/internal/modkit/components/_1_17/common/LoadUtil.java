package org.asf.cyan.api.internal.modkit.components._1_17.common;

import java.util.HashMap;
import java.util.function.Consumer;

import org.asf.cyan.api.internal.modkit.transformers._1_17.common.world.storage.LevelModDataReader;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.versioning.Version;

import modkit.util.Colors;
import net.minecraft.locale.Language;

public class LoadUtil {

	public static boolean checkWorldJoin(LevelModDataReader reader, String gameVersion, boolean addGame,
			Consumer<String> callback) {
		HashMap<String, String> entries = new HashMap<String, String>();
		HashMap<String, String> localEntries = new HashMap<String, String>();
		for (String loader : reader.cyanGetAllLoaders()) {
			entries.put(loader, reader.cyanGetLoader(loader).version);
			entries.putAll(reader.cyanGetLoader(loader).mods);
			entries.putAll(reader.cyanGetLoader(loader).coremods);
		}

		for (Modloader loader : Modloader.getAllModloaders()) {
			localEntries.put(loader.getName(), loader.getVersion().toString());
			for (IModManifest manifest : loader.getLoadedCoremods()) {
				localEntries.put(manifest.id(), manifest.version().toString());
			}
			for (IModManifest manifest : loader.getLoadedMods()) {
				localEntries.put(manifest.id(), manifest.version().toString());
			}
		}

		String newMsg = Language.getInstance().getOrDefault("modkit.new");
		if (newMsg.equals("modkit.new"))
			newMsg = "New";

		String removedMsg = Language.getInstance().getOrDefault("modkit.removed");
		if (removedMsg.equals("modkit.removed"))
			removedMsg = "Removed";

		String upgradedMsg = Language.getInstance().getOrDefault("modkit.upgraded");
		if (upgradedMsg.equals("modkit.upgraded"))
			upgradedMsg = "Upgraded";

		String downgradedMsg = Language.getInstance().getOrDefault("modkit.downgraded");
		if (downgradedMsg.equals("modkit.downgraded"))
			downgradedMsg = "Downgraded";

		HashMap<String, String> statuses = new HashMap<String, String>();
		for (String key : localEntries.keySet()) {
			if (!entries.containsKey(key))
				statuses.put(key,
						Colors.GOLD + newMsg + Colors.LIGHT_GREY + ", " + Colors.DARK_PURPLE + localEntries.get(key));
		}
		for (String key : entries.keySet()) {
			if (!localEntries.containsKey(key))
				statuses.put(key, Colors.DARK_RED + removedMsg);
			else {
				Version oldVer = Version.fromString(entries.get(key));
				Version newVer = Version.fromString(localEntries.get(key));
				if (!oldVer.isEqualTo(newVer)) {
					if (oldVer.isGreaterThan(newVer))
						statuses.put(key,
								Colors.GOLD + downgradedMsg + Colors.LIGHT_GREY + ", " + Colors.DARK_PURPLE + newVer);
					else
						statuses.put(key, Colors.DARK_GREEN + upgradedMsg + Colors.LIGHT_GREY + ", "
								+ Colors.DARK_PURPLE + newVer);
				}
			}
		}
		if (statuses.size() != 0 || addGame) {
			Version game = Version.fromString(gameVersion);
			Version current = Version.fromString(Modloader.getModloaderGameVersion());
			if (!game.isEqualTo(current)) {
				if (game.isGreaterThan(current))
					statuses.put("Minecraft", Colors.GOLD + Language.getInstance().getOrDefault("modkit.downgraded")
							+ Colors.LIGHT_GREY + ", " + Colors.DARK_PURPLE + current);
				else
					statuses.put("Minecraft", Colors.DARK_GREEN + Language.getInstance().getOrDefault("modkit.upgraded")
							+ Colors.LIGHT_GREY + ", " + Colors.DARK_PURPLE + current);
			}
		}
		if (statuses.size() != 0) {
			StringBuilder statusChange = new StringBuilder();
			statusChange.append(Colors.LIGHT_GREY);
			boolean first = true;
			if (statuses.containsKey("Minecraft")) {
				String key = "Minecraft";
				String value = statuses.get("Minecraft");
				statuses.remove(key);
				statusChange.append(Colors.LIGHT_GREEN).append(key).append(Colors.LIGHT_PURPLE).append(" (")
						.append(value).append(Colors.LIGHT_PURPLE).append(")");
				first = false;
			}
			for (String key : statuses.keySet()) {
				String value = statuses.get(key);
				if (!first)
					statusChange.append(Colors.LIGHT_GREY + ", ");
				statusChange.append(Colors.LIGHT_GREEN).append(key).append(Colors.LIGHT_PURPLE).append(" (")
						.append(value).append(Colors.LIGHT_PURPLE).append(")");
				first = false;
			}

			callback.accept(statusChange.toString());
			return true;
		}

		return false;
	}

}
