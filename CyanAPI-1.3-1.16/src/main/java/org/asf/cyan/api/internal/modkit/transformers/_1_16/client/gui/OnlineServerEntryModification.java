package org.asf.cyan.api.internal.modkit.transformers._1_16.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.asf.cyan.api.internal.modkit.transformers._1_16.client.network.ServerDataAccessor;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.LocalVariable;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetName;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;
import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;

import modkit.protocol.ModKitModloader;
import modkit.protocol.ModKitProtocol;
import modkit.protocol.handshake.Handshake;
import modkit.protocol.handshake.HandshakeRule;
import modkit.util.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

@FluidTransformer
@TargetClass(target = "net.minecraft.client.gui.screens.multiplayer.ServerSelectionList$OnlineServerEntry")
public class OnlineServerEntryModification {

	private final ServerData serverData = null;

	@InjectAt(location = InjectLocation.HEAD, targetCall = "width(net.minecraft.network.chat.FormattedText)", targetOwner = "net.minecraft.client.gui.Font")
	public void render(@TargetType(target = "com.mojang.blaze3d.vertex.PoseStack") PoseStack var1, int var2, int var3,
			int var4, int var5, int var6, int var7, int var8, boolean var9, float var10, @LocalVariable boolean var11,
			@SuppressWarnings("rawtypes") @LocalVariable List var13, @LocalVariable Component var14,
			@LocalVariable int var15) {

		if (!var11) {
			Component newMsg = cyanProcessRender(var14);
			if (newMsg != null) {
				var14 = newMsg;
				var11 = true;
			}
		}
	}

	@SuppressWarnings("rawtypes")
	@TargetName(target = "render")
	@InjectAt(location = InjectLocation.HEAD, targetCall = "color4f(float, float, float, float)", targetOwner = "com.mojang.blaze3d.systems.RenderSystem")
	public void render2(@TargetType(target = "com.mojang.blaze3d.vertex.PoseStack") PoseStack var1, int var2, int var3,
			int var4, int var5, int var6, int var7, int var8, boolean var9, float var10, @LocalVariable boolean var11,
			@LocalVariable List var12, @LocalVariable Component var13, @LocalVariable int var14,
			@LocalVariable int var15, @LocalVariable int var16, @LocalVariable int var17, @LocalVariable List var18,
			@LocalVariable boolean var19, @LocalVariable Object var20) {

		if (var11) {
			Object[] info = cyanTooltip(var13);
			if (info != null) {
				var18 = (List) info[0];
				var20 = (Component) info[1];
			}
		}
	}

	private Object[] cyanTooltip(Component var14) {
		ServerDataAccessor acc = (ServerDataAccessor) serverData;
		if (acc.cyanGetServerData() != null && var14 instanceof TranslatableComponent) {
			TranslatableComponent comp = (TranslatableComponent) var14;
			if (comp.getKey().equals("modkit.incompatible.server.serverlist")) {
				JsonArray loaders = acc.cyanGetServerData().get("modkit").getAsJsonObject().get("modloader")
						.getAsJsonObject().get("all").getAsJsonArray();
				JsonObject srv = acc.cyanGetServerData().get("modkit").getAsJsonObject().get("modloader")
						.getAsJsonObject().get("root").getAsJsonObject();

				List<HandshakeRule> rules = new ArrayList<HandshakeRule>();
				for (JsonElement r : acc.cyanGetServerData().get("modkit").getAsJsonObject().get("handshake")
						.getAsJsonArray()) {
					JsonObject rule = r.getAsJsonObject();
					rules.add(new HandshakeRule(GameSide.valueOf(rule.get("side").getAsString()),
							rule.get("key").getAsString(), rule.get("checkstring").getAsString()));
				}

				Component tooltip = new TranslatableComponent("modkit.incompatible.server.serverlist.tooltip",
						(srv.has("simplename") ? srv.get("simplename").getAsString()
								: (Modloader.getModloader(srv.get("name").getAsString()) == null
										? srv.get("name").getAsString()
										: Modloader.getModloader(srv.get("name").getAsString()).getSimpleName())),
						acc.cyanGetServerData().get("modkit").getAsJsonObject().get("modloader").getAsJsonObject()
								.get("root").getAsJsonObject().get("game.version").getAsString(),
						serverData.name, serverData.protocol).withStyle(ChatFormatting.RED);

				List<Component> lst = new ArrayList<Component>();
				lst.add(tooltip);

				ArrayList<String> mods = new ArrayList<String>();
				HashMap<String, Version> localEntries = new HashMap<String, Version>();
				localEntries.put("game", Version.fromString(Modloader.getModloaderGameVersion()));
				for (Modloader loader : Modloader.getAllModloaders()) {
					localEntries.put(loader.getName().toLowerCase(), loader.getVersion());
					Map<String, Version> entries = loader.getRuleEntries();
					for (String key : entries.keySet()) {
						localEntries.putIfAbsent(key, entries.get(key));
					}
				}

				boolean modIssue = false;
				for (HandshakeRule rule : rules) {
					if (!rule.validate(localEntries, GameSide.CLIENT)) {
						mods.add(rule.getKey());
						modIssue = true;
					}
				}

				boolean protocolIssue = !Handshake.validateProtocols(acc.cyanGetServerData());
				boolean incompatibleClientRules = false;

				HashMap<String, Version> remoteEntries = new HashMap<String, Version>();
				remoteEntries.put("game", Version.fromString(srv.get("game.version").getAsString()));
				for (JsonElement element : loaders) {
					JsonObject modloader = element.getAsJsonObject();

					String name = modloader.get("name").getAsString();
					String version = modloader.get("version").getAsString();

					remoteEntries.put(name.toLowerCase(), Version.fromString(version));
					JsonArray srvmods = modloader.get("mods").getAsJsonArray();
					JsonArray coremods = modloader.get("coremods").getAsJsonArray();

					for (JsonElement ele : srvmods) {
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

				for (HandshakeRule rule : HandshakeRule.getAllRules()) {
					if (!rule.validate(remoteEntries, GameSide.SERVER)) {
						incompatibleClientRules = true;
						break;
					}
				}

				if (modIssue) {
					for (JsonElement ld : loaders) {
						JsonObject loader = ld.getAsJsonObject();
						for (JsonElement md : loader.get("mods").getAsJsonArray()) {
							JsonObject mod = md.getAsJsonObject();

							String name;
							if (mod.has("name")) {
								name = mod.get("name").getAsString();
							} else {
								name = mod.get("id").getAsString();
							}

							if (lst.size() == 1) {
								lst.add(new TextComponent(""));
								lst.add(new TranslatableComponent(
										"modkit.incompatible.server.serverlist.tooltip.listmessage")
												.withStyle(ChatFormatting.GREEN));
							}
							if (!mods.contains(mod.get("id").getAsString()))
								lst.add(new TextComponent(name).withStyle(ChatFormatting.DARK_GREEN));
							else
								lst.add(new TextComponent(name).withStyle(ChatFormatting.DARK_RED));
						}
						for (JsonElement md : loader.get("coremods").getAsJsonArray()) {
							JsonObject mod = md.getAsJsonObject();

							String name;
							if (mod.has("name")) {
								name = mod.get("name").getAsString();
							} else {
								name = mod.get("id").getAsString();
							}

							if (lst.size() == 1) {
								lst.add(new TextComponent(""));
								lst.add(new TranslatableComponent(
										"modkit.incompatible.server.serverlist.tooltip.listmessage")
												.withStyle(ChatFormatting.GREEN));
							}
							if (!mods.contains(mod.get("id").getAsString()))
								lst.add(new TextComponent(name).withStyle(ChatFormatting.DARK_GREEN));
							else
								lst.add(new TextComponent(name).withStyle(ChatFormatting.DARK_RED));
						}
					}
				} else if (!protocolIssue && incompatibleClientRules) {
					for (JsonElement ld : loaders) {
						JsonObject loader = ld.getAsJsonObject();
						for (JsonElement md : loader.get("mods").getAsJsonArray()) {
							JsonObject mod = md.getAsJsonObject();

							String name;
							if (mod.has("name")) {
								name = mod.get("name").getAsString();
							} else {
								name = mod.get("id").getAsString();
							}

							if (lst.size() == 1) {
								lst.add(new TextComponent(""));
								lst.add(new TranslatableComponent(
										"modkit.incompatible.server.serverlist.tooltip.listmessage")
												.withStyle(ChatFormatting.GREEN));
							}
							if (!mods.contains(mod.get("id").getAsString()))
								lst.add(new TextComponent(name).withStyle(ChatFormatting.DARK_GREEN));
							else
								lst.add(new TextComponent(name).withStyle(ChatFormatting.DARK_RED));
						}
						for (JsonElement md : loader.get("coremods").getAsJsonArray()) {
							JsonObject mod = md.getAsJsonObject();

							String name;
							if (mod.has("name")) {
								name = mod.get("name").getAsString();
							} else {
								name = mod.get("id").getAsString();
							}

							if (lst.size() == 1) {
								lst.add(new TextComponent(""));
								lst.add(new TranslatableComponent(
										"modkit.incompatible.server.serverlist.tooltip.listmessage")
												.withStyle(ChatFormatting.GREEN));
							}
							if (!mods.contains(mod.get("id").getAsString()))
								lst.add(new TextComponent(name).withStyle(ChatFormatting.DARK_GREEN));
							else
								lst.add(new TextComponent(name).withStyle(ChatFormatting.DARK_RED));
						}
					}

					boolean missingServerMods = false;
					for (IModManifest mod : Modloader.getAllMods()) {
						boolean found = false;
						for (JsonElement ld : loaders) {
							JsonObject ldr = ld.getAsJsonObject();
							for (JsonElement md : ldr.get("mods").getAsJsonArray()) {
								JsonObject md2 = md.getAsJsonObject();
								if (md2.get("id").getAsString().equalsIgnoreCase(mod.id())) {
									found = true;
									break;
								}
							}
							for (JsonElement md : ldr.get("coremods").getAsJsonArray()) {
								JsonObject md2 = md.getAsJsonObject();
								if (md2.get("id").getAsString().equalsIgnoreCase(mod.id())) {
									found = true;
									break;
								}
							}
						}
						if (!found) {
							if (!missingServerMods) {
								missingServerMods = true;
								lst.add(new TextComponent(""));
								lst.add(new TranslatableComponent(
										"modkit.incompatible.server.serverlist.tooltip.missingservermods")
												.withStyle(ChatFormatting.DARK_PURPLE));
							}
							lst.add(new TextComponent(mod.displayName()).withStyle(ChatFormatting.DARK_RED));
						}
					}
				} else {
					if (protocolIssue) {
						lst.clear();
						tooltip = new TranslatableComponent(
								"modkit.incompatible.server.serverlist.tooltip.protocolfailure")
										.withStyle(ChatFormatting.RED);
						lst.add(tooltip);

						double serverProtocol = acc.cyanGetServerData().get("modkit").getAsJsonObject().get("protocol")
								.getAsDouble();
						double serverMinProtocol = acc.cyanGetServerData().get("modkit").getAsJsonObject()
								.get("protocol.min").getAsDouble();
						double serverMaxProtocol = acc.cyanGetServerData().get("modkit").getAsJsonObject()
								.get("protocol.max").getAsDouble();

						int status = Handshake.validateModKitProtocol(serverProtocol, serverMinProtocol,
								serverMaxProtocol, ModKitProtocol.CURRENT, ModKitProtocol.MIN_PROTOCOL,
								ModKitProtocol.MAX_PROTOCOL);
						if (status != 0) {
							lst.add(new TextComponent(""));
							lst.add(new TranslatableComponent(
									"modkit.incompatible.server.serverlist.tooltip.protocol.modkit")
											.withStyle(ChatFormatting.DARK_RED));
							lst.add(new TranslatableComponent(
									"modkit.incompatible.server.serverlist.tooltip.protocolcurrent",
									Colors.GOLD + serverProtocol));
							lst.add(new TranslatableComponent(
									"modkit.incompatible.server.serverlist.tooltip.protocolrequired",
									Colors.GOLD + ModKitProtocol.MIN_PROTOCOL
											+ (ModKitProtocol.MIN_PROTOCOL != ModKitProtocol.MAX_PROTOCOL ? "+" : "")));
							lst.add(new TranslatableComponent(
									"modkit.incompatible.server.serverlist.tooltip.protocolmax",
									Colors.GOLD + ModKitProtocol.MAX_PROTOCOL));
						}

						boolean incompatibleServerModloaders = false;
						for (Modloader loader : Modloader.getAllModloaders()) {
							if (loader instanceof ModKitModloader
									&& loader instanceof ModKitModloader.ModKitProtocolRules) {
								ModKitModloader.ModKitProtocolRules protocol = (ModKitModloader.ModKitProtocolRules) loader;

								boolean found = false;
								for (JsonElement ld : loaders) {
									JsonObject ldr = ld.getAsJsonObject();
									if (ldr.get("name").getAsString().equals(loader.getName())) {
										found = true;
										if (ldr.has("protocol")) {
											JsonObject lprotocol = ldr.get("protocol").getAsJsonObject();
											double loaderProtocol = lprotocol.get("version").getAsDouble();
											double loaderMinProtocol = lprotocol.get("min").getAsDouble();
											double loaderMaxProtocol = lprotocol.get("max").getAsDouble();

											int statusModloader = Handshake.validateLoaderProtocol(loaderProtocol,
													loaderMinProtocol, loaderMaxProtocol, protocol.modloaderProtocol(),
													protocol.modloaderMinProtocol(), protocol.modloaderMaxProtocol());
											if (statusModloader != 0) {
												if (!incompatibleServerModloaders) {
													incompatibleServerModloaders = false;
													lst.add(new TextComponent(""));
													lst.add(new TranslatableComponent(
															"modkit.incompatible.server.serverlist.tooltip.incompatibleloaders.server")
																	.withStyle(ChatFormatting.DARK_RED));
												}

												if (statusModloader == 2) {
													lst.add(new TranslatableComponent(
															"modkit.incompatible.server.serverlist.tooltip.incompatibleloader.server",
															Colors.YELLOW + loader.getName(),
															new TranslatableComponent(
																	"modkit.incompatible.server.serverlist.tooltip.incompatibleloader.newer"),
															Colors.GOLD + ldr.get("version").getAsString()));
												} else {
													lst.add(new TranslatableComponent(
															"modkit.incompatible.server.serverlist.tooltip.incompatibleloader.server",
															Colors.GOLD + loader.getName(),
															new TranslatableComponent(
																	"modkit.incompatible.server.serverlist.tooltip.incompatibleloader.outdated"),
															Colors.GOLD + ldr.get("version").getAsString()));
												}
											}
										} else {
											if (!incompatibleServerModloaders) {
												incompatibleServerModloaders = false;
												lst.add(new TextComponent(""));
												lst.add(new TranslatableComponent(
														"modkit.incompatible.server.serverlist.tooltip.incompatibleloaders.server")
																.withStyle(ChatFormatting.DARK_RED));
											}
											lst.add(new TranslatableComponent(
													"modkit.incompatible.server.serverlist.tooltip.incompatibleloader.server",
													Colors.DARK_GREEN + loader.getName(),
													new TranslatableComponent(
															"modkit.incompatible.server.serverlist.tooltip.incompatibleloader.incompatible"),
													Colors.GOLD + ldr.get("version").getAsString()));
										}
										break;
									}
								}

								if (!found) {
									if (!incompatibleServerModloaders) {
										incompatibleServerModloaders = false;
										lst.add(new TextComponent(""));
										lst.add(new TranslatableComponent(
												"modkit.incompatible.server.serverlist.tooltip.incompatibleloaders.server")
														.withStyle(ChatFormatting.DARK_RED));
									}
									lst.add(new TranslatableComponent(
											"modkit.incompatible.server.serverlist.tooltip.incompatibleloader.client",
											Colors.DARK_RED + loader.getName(),
											new TranslatableComponent(
													"modkit.incompatible.server.serverlist.tooltip.incompatibleloader.missing"),
											Colors.GOLD + loader.getVersion()));
								}
							}
						}

						boolean missingClientModloaders = false;
						for (JsonElement ld : loaders) {
							JsonObject ldr = ld.getAsJsonObject();
							if (ldr.has("protocol")) {
								if (Modloader.getModloader(ldr.get("name").getAsString()) == null) {
									if (!missingClientModloaders) {
										missingClientModloaders = false;
										lst.add(new TextComponent(""));
										lst.add(new TranslatableComponent(
												"modkit.incompatible.server.serverlist.tooltip.incompatibleloaders.client")
														.withStyle(ChatFormatting.DARK_RED));
									}
									lst.add(new TextComponent(ldr.get("simplename").getAsString())
											.withStyle(ChatFormatting.DARK_RED));
								} else {

								}
							}
						}

					} else {
						lst.clear();
						tooltip = new TranslatableComponent("modkit.incompatible.server.serverlist.tooltip.loaderissue",
								(srv.has("simplename") ? srv.get("simplename").getAsString()
										: (Modloader.getModloader(srv.get("name").getAsString()) == null
												? srv.get("name").getAsString()
												: Modloader.getModloader(srv.get("name").getAsString())
														.getSimpleName())),
								acc.cyanGetServerData().get("modkit").getAsJsonObject().get("modloader")
										.getAsJsonObject().get("root").getAsJsonObject().get("game.version")
										.getAsString(),
								serverData.name, serverData.protocol).withStyle(ChatFormatting.RED);
						lst.add(tooltip);
					}
				}

				return new Object[] { lst, tooltip };
			} else if (!HandshakeUtils.getImpl().handhakeCheck(acc.cyanGetServerData())) {
				Component tooltip = new TranslatableComponent("modkit.incompatible.server.serverlist.noloaders")
						.withStyle(ChatFormatting.RED);
				List<Component> lst = new ArrayList<Component>();
				lst.add(new TranslatableComponent("modkit.incompatible.server.serverlist.noloaders.tooltip")
						.withStyle(ChatFormatting.RED));
				boolean missingServerMods = false;
				for (IModManifest mod : Modloader.getAllMods()) {
					if (!missingServerMods) {
						missingServerMods = true;
						lst.add(new TextComponent(""));
						lst.add(new TranslatableComponent(
								"modkit.incompatible.server.serverlist.tooltip.requiredservermods")
										.withStyle(ChatFormatting.DARK_PURPLE));
					}
					lst.add(new TextComponent(mod.displayName()).withStyle(ChatFormatting.DARK_RED));
				}

				return new Object[] { lst, tooltip };
			}
		}
		return null;
	}

	private Component cyanProcessRender(Component var14) {
		ServerDataAccessor acc = (ServerDataAccessor) serverData;

		if (acc.cyanGetServerData() != null) {
			if (!HandshakeUtils.getImpl().handhakeCheck(acc.cyanGetServerData())
					&& acc.cyanGetServerData().has("modkit")) {
				JsonObject srv = acc.cyanGetServerData().get("modkit").getAsJsonObject().get("modloader")
						.getAsJsonObject().get("root").getAsJsonObject();
				return new TranslatableComponent("modkit.incompatible.server.serverlist", (srv.has("simplename")
						? srv.get("simplename").getAsString()
						: (Modloader.getModloader(srv.get("name").getAsString()) == null ? srv.get("name").getAsString()
								: Modloader.getModloader(srv.get("name").getAsString()).getSimpleName())),
						acc.cyanGetServerData().get("modkit").getAsJsonObject().get("modloader").getAsJsonObject()
								.get("root").getAsJsonObject().get("game.version").getAsString(),
						serverData.name, serverData.protocol).withStyle(ChatFormatting.RED);
			} else if (!HandshakeUtils.getImpl().handhakeCheck(acc.cyanGetServerData())) {
				return new TranslatableComponent("modkit.incompatible.server.serverlist.noloaders")
						.withStyle(ChatFormatting.RED);
			}
		}

		return null;
	}

}
