package org.asf.cyan.api.internal.modkit.transformers._1_16.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

import modkit.protocol.handshake.HandshakeRule;
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
								: (Modloader.getModloader(srv.get("name") == null ? srv.get("name").getAsString()
										: Modloader.getModloader(srv.get("name").getAsString()).getSimpleName()))),
						acc.cyanGetServerData().get("modkit").getAsJsonObject().get("modloader").getAsJsonObject()
								.get("root").getAsJsonObject().get("game.version").getAsString(),
						serverData.name, serverData.protocol).withStyle(ChatFormatting.RED);

				List<Component> lst = new ArrayList<Component>();
				lst.add(tooltip);

				ArrayList<String> mods = new ArrayList<String>();
				HashMap<String, Version> localMods = new HashMap<String, Version>();
				for (IModManifest mod : Modloader.getAllMods()) {
					localMods.put(mod.id(), mod.version());
				}
				boolean modIssue = false;
				for (HandshakeRule rule : rules) {
					if (!rule.validate(localMods, GameSide.CLIENT)) {
						mods.add(rule.getKey());
						modIssue = true;
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
				} else {
					lst.clear();
					tooltip = new TranslatableComponent("modkit.incompatible.server.serverlist.tooltip.loaderissue",
							(srv.has("simplename") ? srv.get("simplename").getAsString()
									: (Modloader.getModloader(srv.get("name") == null ? srv.get("name").getAsString()
											: Modloader.getModloader(srv.get("name").getAsString()).getSimpleName()))),
							acc.cyanGetServerData().get("modkit").getAsJsonObject().get("modloader").getAsJsonObject()
									.get("root").getAsJsonObject().get("game.version").getAsString(),
							serverData.name, serverData.protocol).withStyle(ChatFormatting.RED);
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
				return new TranslatableComponent("modkit.incompatible.server.serverlist",
						(srv.has("simplename") ? srv.get("simplename").getAsString() : srv.get("name").getAsString()),
						acc.cyanGetServerData().get("modkit").getAsJsonObject().get("modloader").getAsJsonObject()
								.get("root").getAsJsonObject().get("game.version").getAsString(),
						serverData.name, serverData.protocol).withStyle(ChatFormatting.RED);
			}
		}

		return null;
	}

}
