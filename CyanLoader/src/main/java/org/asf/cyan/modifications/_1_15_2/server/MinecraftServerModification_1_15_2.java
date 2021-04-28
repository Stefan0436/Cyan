package org.asf.cyan.modifications._1_15_2.server;

import java.io.File;
import java.net.Proxy;

import org.asf.cyan.api.fluid.annotations.PlatformExclude;
import org.asf.cyan.api.fluid.annotations.VersionRegex;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.modloader.LoadPhase;
import org.asf.cyan.core.CyanCore;

import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

@FluidTransformer
@VersionRegex("^1\\.15\\.2$")
@TargetClass(target = "net.minecraft.server.MinecraftServer")
@PlatformExclude(LaunchPlatform.SPIGOT)
public class MinecraftServerModification_1_15_2 {

	private static boolean firstLoad;

	@Constructor(clinit = true)
	@InjectAt(location = InjectLocation.TAIL)
	public static void clinit() {
		firstLoad = true;
	}

	@Constructor
	@InjectAt(location = InjectLocation.TAIL)
	public static void ctor(File var1, Proxy var2, @TargetType(target = "com.mojang.datafixers.DataFixer") Object var3,
			@TargetType(target = "net.minecraft.commands.Commands") Object var4,
			@TargetType(target = "com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService") Object var5,
			@TargetType(target = "com.mojang.authlib.minecraft.MinecraftSessionService") Object var6,
			@TargetType(target = "com.mojang.authlib.GameProfileRepository") Object var7,
			@TargetType(target = "net.minecraft.server.players.GameProfileCache") Object var8,
			@TargetType(target = "net.minecraft.server.level.progress.ChunkProgressListenerFactory") Object var9,
			String var10) {
		if (firstLoad) {
			CyanCore.setPhase(LoadPhase.RUNTIME);
			Modloader.getModloader().dispatchEvent("mods.runtimestart");

			firstLoad = false;
		}
	}

}
