package modkit.protocol.handshake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.versioning.Version;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import modkit.protocol.ModKitProtocol;
import modkit.protocol.ModkitModloader;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

/**
 * 
 * ModKit Handshake Controller - Interface for the ModKit handshake process
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 * @since 1.1
 *
 */
public class Handshake extends CyanComponent {

	/**
	 * Runs the early handshake process
	 * 
	 * @param response       Response server status JSON.
	 * @param disconnectCall Method to call to disconnect the client
	 * @return True if the client can try to log in, false otherwise
	 */
	public static boolean earlyClientHandshake(JsonObject response, Consumer<Component> disconnectCall) {
		ModkitModloader.ModkitProtocolRules root = (ModkitModloader.ModkitProtocolRules) ModkitModloader
				.getModKitRootModloader();

		if (response.has("modkit")) {
			JsonObject modkitData = response.get("modkit").getAsJsonObject();

			double serverProtocol = modkitData.get("protocol").getAsDouble();
			double serverMinProtocol = modkitData.get("protocol.min").getAsDouble();
			double serverMaxProtocol = modkitData.get("protocol.max").getAsDouble();

			int status = validateModKitProtocol(serverProtocol, serverMinProtocol, serverMaxProtocol,
					ModKitProtocol.CURRENT, ModKitProtocol.MIN_PROTOCOL, ModKitProtocol.MAX_PROTOCOL);
			if (status != 0) {
				final String failure;
				final Object[] args;
				if (status == 2) {
					failure = "modkit.protocol.outdated.local";
					args = new Object[] { "\u00A76" + serverProtocol, "\u00A76" + ModKitProtocol.MIN_PROTOCOL };
					info("Connection failed: outdated server modkit protocol: " + serverProtocol + ", client protocol: "
							+ ModKitProtocol.CURRENT + " (min: " + ModKitProtocol.MIN_PROTOCOL + ", max: "
							+ ModKitProtocol.MAX_PROTOCOL + ")");
				} else {
					failure = "modkit.protocol.outdated.remote";
					args = new Object[] { "\u00A76" + ModKitProtocol.CURRENT, "\u00A76" + serverMinProtocol };
					info("Connection failed: outdated client modkit protocol: " + ModKitProtocol.CURRENT
							+ ", server protocol: " + serverProtocol + " (min: " + serverMinProtocol + ", max: "
							+ serverMaxProtocol + ")");
				}
				disconnectCall.accept(new TranslatableComponent(failure, args));
				return false;
			}

			JsonObject modloaderData = modkitData.get("modloader").getAsJsonObject();
			HashMap<String, Version> remoteEntries = new HashMap<String, Version>();
			remoteEntries.put("game",
					Version.fromString(modloaderData.get("root").getAsJsonObject().get("game.version").getAsString()));

			ArrayList<String> presentLoaders = new ArrayList<String>();
			JsonArray loaders = modloaderData.get("all").getAsJsonArray();
			for (JsonElement element : loaders) {
				JsonObject modloader = element.getAsJsonObject();

				String name = modloader.get("name").getAsString();
				String version = modloader.get("version").getAsString();

				if (modloader.has("protocol")) {
					Modloader loader = Modloader.getModloader(name);
					if (loader == null) {
						disconnectCall.accept(
								new TranslatableComponent("modkit.missingmodded.client.loader", "\u00A76" + name));
						return false;
					}

					presentLoaders.add(name);

					JsonObject protocol = modloader.get("protocol").getAsJsonObject();
					double loaderProtocol = protocol.get("version").getAsDouble();
					double loaderMinProtocol = protocol.get("min").getAsDouble();
					double loaderMaxProtocol = protocol.get("max").getAsDouble();

					String loaderVersion = loader.getVersion().toString();

					status = validateLoaderProtocol(loaderProtocol, loaderMinProtocol, loaderMaxProtocol,
							root.modloaderProtocol(), root.modloaderMinProtocol(), root.modloaderMaxProtocol());
					if (status != 0) {
						final String failure;
						final Object[] args;
						if (status == 2) {
							failure = "modkit.loader.outdated.local";
							args = new Object[] { version, loaderVersion, root.modloaderMinProtocol() };
							info("Connection failed: outdated server modloader: " + serverProtocol + "(" + version + ")"
									+ ", client protocol: " + root.modloaderProtocol() + " (" + loaderVersion
									+ ", min: " + root.modloaderMinProtocol() + ", max: " + root.modloaderMaxProtocol()
									+ ")");
						} else {
							failure = "modkit.loader.outdated.remote";
							args = new Object[] { version, loaderVersion, root.modloaderMaxProtocol() };
							info("Connection failed: outdated client modloader: " + root.modloaderProtocol() + "("
									+ loaderVersion + ")" + ", server protocol: " + serverProtocol + " (" + version
									+ ", min: " + serverMinProtocol + ", max: " + serverMaxProtocol + ")");
						}
						disconnectCall.accept(new TranslatableComponent(failure, args));
						return false;
					}
				}

				remoteEntries.put(name.toLowerCase(), Version.fromString(version));
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

			if (!presentLoaders.contains(ModkitModloader.getModKitRootModloader().getName())) {
				disconnectCall.accept(new TranslatableComponent("modkit.missingmodded.server.loader",
						"\u00A76" + ModkitModloader.getModKitRootModloader().getName()));
				return false;
			}

			HashMap<String, Version> localEntries = new HashMap<String, Version>();
			localEntries.put("game", Version.fromString(Modloader.getModloaderGameVersion()));
			for (Modloader loader : Modloader.getAllModloaders()) {
				localEntries.put(loader.getName().toLowerCase(), loader.getVersion());
			}
			for (IModManifest mod : Modloader.getAllMods()) {
				localEntries.putIfAbsent(mod.id(), mod.version());
			}

			ArrayList<HandshakeRule> rules = new ArrayList<HandshakeRule>();
			JsonArray remoteRules = modkitData.get("handshake").getAsJsonArray();
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
						missingClient += "\u00A77, ";
					missingClient += "\u00A75";
					missingClient += key;
					if (!val.isEmpty()) {
						missingClient += "\u00A77 (\u00A76";
						missingClient += val;
						missingClient += "\u00A77)";
					}
					missingClient += "\u00A77";

					if (!missingClientNonColor.isEmpty())
						missingClientNonColor += ", ";
					missingClientNonColor += key;
				}
			}
			if (failServer) {
				for (String key : output2.keySet()) {
					String val = output2.get(key);
					if (!missingServer.isEmpty())
						missingServer += "\u00A77, ";
					missingServer += "\u00A75";
					missingServer += key;
					if (!val.isEmpty()) {
						missingServer += "\u00A77 (\u00A76";
						missingServer += val;
						missingServer += "\u00A77)";
					}
					missingServer += "\u00A77";

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
					warn("Local client is missing " + output2.size() + " CYAN mods for the server and " + output1.size()
							+ " CYAN mods on the client. (mods: " + missingClientNonColor + ", server mods: "
							+ missingServerNonColor + ")");

					failure = "modkit.missingmods.both";
					args = new Object[] { missingClient, missingServer };
				}
				disconnectCall.accept(new TranslatableComponent(failure, args));
				return false;
			}
		} else if (HandshakeRule.getAllRules().stream().filter(t -> t.getSide() == GameSide.SERVER).count() != 0) {
			disconnectCall.accept(new TranslatableComponent("modkit.missingmodded.server"));
			return false;
		}

		return true;
	}

	/**
	 * Validates the given server
	 * 
	 * @param response       Response server status JSON.
	 * @return True if the client is compatible, false otherwise
	 */
	public static boolean serverListHandshake(JsonObject response) {
		ModkitModloader.ModkitProtocolRules root = (ModkitModloader.ModkitProtocolRules) ModkitModloader
				.getModKitRootModloader();

		if (response.has("modkit")) {
			JsonObject modkitData = response.get("modkit").getAsJsonObject();

			double serverProtocol = modkitData.get("protocol").getAsDouble();
			double serverMinProtocol = modkitData.get("protocol.min").getAsDouble();
			double serverMaxProtocol = modkitData.get("protocol.max").getAsDouble();

			int status = validateModKitProtocol(serverProtocol, serverMinProtocol, serverMaxProtocol,
					ModKitProtocol.CURRENT, ModKitProtocol.MIN_PROTOCOL, ModKitProtocol.MAX_PROTOCOL);
			if (status != 0)
				return false;

			JsonObject modloaderData = modkitData.get("modloader").getAsJsonObject();
			HashMap<String, Version> remoteEntries = new HashMap<String, Version>();
			remoteEntries.put("game",
					Version.fromString(modloaderData.get("root").getAsJsonObject().get("game.version").getAsString()));

			ArrayList<String> presentLoaders = new ArrayList<String>();
			JsonArray loaders = modloaderData.get("all").getAsJsonArray();
			for (JsonElement element : loaders) {
				JsonObject modloader = element.getAsJsonObject();

				String name = modloader.get("name").getAsString();
				String version = modloader.get("version").getAsString();

				if (modloader.has("protocol")) {
					Modloader loader = Modloader.getModloader(name);
					if (loader == null)
						return false;

					presentLoaders.add(name);

					JsonObject protocol = modloader.get("protocol").getAsJsonObject();
					double loaderProtocol = protocol.get("version").getAsDouble();
					double loaderMinProtocol = protocol.get("min").getAsDouble();
					double loaderMaxProtocol = protocol.get("max").getAsDouble();

					status = validateLoaderProtocol(loaderProtocol, loaderMinProtocol, loaderMaxProtocol,
							root.modloaderProtocol(), root.modloaderMinProtocol(), root.modloaderMaxProtocol());
					if (status != 0)
						return false;
				}

				remoteEntries.put(name.toLowerCase(), Version.fromString(version));
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

			if (!presentLoaders.contains(ModkitModloader.getModKitRootModloader().getName()))
				return false;

			HashMap<String, Version> localEntries = new HashMap<String, Version>();
			localEntries.put("game", Version.fromString(Modloader.getModloaderGameVersion()));
			for (Modloader loader : Modloader.getAllModloaders()) {
				localEntries.put(loader.getName().toLowerCase(), loader.getVersion());
			}
			for (IModManifest mod : Modloader.getAllMods()) {
				localEntries.putIfAbsent(mod.id(), mod.version());
			}

			ArrayList<HandshakeRule> rules = new ArrayList<HandshakeRule>();
			JsonArray remoteRules = modkitData.get("handshake").getAsJsonArray();
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

			boolean failClient = !HandshakeRule.checkAll(localEntries, GameSide.CLIENT, null, rules);
			boolean failServer = !HandshakeRule.checkAll(remoteEntries, GameSide.SERVER, null, rules);

			if (failClient || failServer)
				return false;
		} else if (HandshakeRule.getAllRules().stream().filter(t -> t.getSide() == GameSide.SERVER).count() != 0)
			return false;

		return true;
	}

	public static int validateModKitProtocol(double serverProtocol, double serverMinProtocol, double serverMaxProtocol,
			double clientProtocol, double clientMinProtocol, double clientMaxProtocol) {
		if (clientProtocol < serverMinProtocol || (clientMaxProtocol != -1.0d && serverProtocol > clientMaxProtocol)) {
			return 1;
		} else if (clientProtocol > serverMaxProtocol
				|| (clientMinProtocol != -1 && serverProtocol < clientMinProtocol)) {
			return 2;
		}
		return 0;
	}

	public static int validateLoaderProtocol(double loaderProtocol, double loaderMinProtocol, double loaderMaxProtocol,
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
