package modkit.protocol.transformers.handshake;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.mods.IModManifest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import modkit.protocol.ModKitProtocol;
import modkit.protocol.ModkitModloader;
import modkit.protocol.ModkitModloader.ModkitProtocolRules;
import modkit.protocol.handshake.HandshakeRule;

/**
 * 
 * ModKit Version Status Transformer Interface - Static interface for modifying
 * the minecraft server status serialization process.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 * @since 1.1
 *
 */
public class VersionStatusTransformer {

	/**
	 * Transforms the data before sending it over the network (call from a
	 * transformer)
	 * 
	 * @param input Input JsonObject
	 */
	public static void applySerializeMethodTransformer(JsonObject input) {
		ModkitModloader root = ModkitModloader.getModKitRootModloader();

		JsonObject modkitData = new JsonObject();
		modkitData.addProperty("protocol", ModKitProtocol.CURRENT);
		modkitData.addProperty("protocol.min", ModKitProtocol.MIN_PROTOCOL);
		modkitData.addProperty("protocol.max", ModKitProtocol.MAX_PROTOCOL);

		JsonObject modloaderData = new JsonObject();
		modloaderData.add("root", modloader(root, false));

		JsonArray loaders = new JsonArray();
		for (Modloader loader : Modloader.getAllModloaders()) {
			loaders.add(modloader(loader, true));
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

		modkitData.add("handshake", handshakeRules);
		input.add("modkit", modkitData);
	}

	private static JsonObject modloader(Modloader modloader, boolean addMods) {
		JsonObject loader = new JsonObject();
		loader.addProperty("name", modloader.getName());
		loader.addProperty("version", modloader.getVersion().toString());
		loader.addProperty("game.version", modloader.getGameVersion());
		loader.addProperty("type", modloader.getClass().getTypeName());
		if (modloader instanceof ModkitProtocolRules) {
			ModkitProtocolRules rules = (ModkitProtocolRules) modloader;
			JsonObject protocol = new JsonObject();
			protocol.addProperty("version", rules.modloaderProtocol());
			protocol.addProperty("min", rules.modloaderMinProtocol());
			protocol.addProperty("max", rules.modloaderMaxProtocol());
			loader.add("protocol", protocol);
		}
		if (addMods) {
			loader.addProperty("allmods.known.count", modloader.getKnownModsCount());
			JsonArray mods = new JsonArray();
			for (IModManifest mod : modloader.getLoadedMods()) {
				JsonObject modinfo = new JsonObject();
				modinfo.addProperty("id", mod.id());
				modinfo.addProperty("version", mod.version().toString());
				mods.add(modinfo);
			}
			loader.add("mods", mods);
			JsonArray coremods = new JsonArray();
			for (IModManifest mod : modloader.getLoadedCoremods()) {
				JsonObject modinfo = new JsonObject();
				modinfo.addProperty("id", mod.id());
				modinfo.addProperty("version", mod.version().toString());
				coremods.add(modinfo);
			}
			loader.add("coremods", coremods);
		}
		return loader;
	}

}
