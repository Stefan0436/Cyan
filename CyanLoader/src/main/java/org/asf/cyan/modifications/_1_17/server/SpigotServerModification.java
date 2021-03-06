package org.asf.cyan.modifications._1_17.server;

import java.net.Proxy;

import org.asf.cyan.api.fluid.annotations.PlatformOnly;
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
@TargetClass(target = "net.minecraft.server.MinecraftServer")
@VersionRegex("^[0-9]\\.[0-9][^5](\\..*)?$")
@PlatformOnly(LaunchPlatform.SPIGOT)
public class SpigotServerModification {

	private static boolean firstLoad;

	@Constructor(clinit = true)
	@InjectAt(location = InjectLocation.TAIL)
	public static void clinit() {
		firstLoad = true;
	}

	@Constructor
	@InjectAt(location = InjectLocation.TAIL)
	public static void ctor1(@TargetType(target = "joptsimple.OptionSet") Object var1,
			@TargetType(target = "net.minecraft.world.level.DataPackConfig") Object var2, Thread var3,
			@TargetType(target = "net.minecraft.core.RegistryAccess$RegistryHolder") Object var4,
			@TargetType(target = "net.minecraft.world.level.storage.LevelStorageSource$LevelStorageAccess") Object var5,
			@TargetType(target = "net.minecraft.world.level.storage.WorldData") Object var6,
			@TargetType(target = "net.minecraft.server.packs.repository.PackRepository") Object var7, Proxy var8,
			@TargetType(target = "com.mojang.datafixers.DataFixer") Object var9,
			@TargetType(target = "net.minecraft.server.ServerResources") Object var10,
			@TargetType(target = "com.mojang.authlib.minecraft.MinecraftSessionService") Object var11,
			@TargetType(target = "com.mojang.authlib.GameProfileRepository") Object var12,
			@TargetType(target = "net.minecraft.server.players.GameProfileCache") Object var13,
			@TargetType(target = "net.minecraft.server.level.progress.ChunkProgressListenerFactory") Object var14) {
		if (firstLoad) {
			CyanCore.setPhase(LoadPhase.POSTINIT);
			Modloader.getModloader().dispatchEvent("mods.postinit");
			
			CyanCore.setPhase(LoadPhase.RUNTIME);
			Modloader.getModloader().dispatchEvent("mods.runtimestart");

			firstLoad = false;
		}
	}

	@Constructor
	@InjectAt(location = InjectLocation.HEAD)
	public static void ctor2(@TargetType(target = "joptsimple.OptionSet") Object var1,
			@TargetType(target = "net.minecraft.world.level.DataPackConfig") Object var2, Thread var3,
			@TargetType(target = "net.minecraft.core.RegistryAccess$RegistryHolder") Object var4,
			@TargetType(target = "net.minecraft.world.level.storage.LevelStorageSource$LevelStorageAccess") Object var5,
			@TargetType(target = "net.minecraft.world.level.storage.WorldData") Object var6,
			@TargetType(target = "net.minecraft.server.packs.repository.PackRepository") Object var7, Proxy var8,
			@TargetType(target = "com.mojang.datafixers.DataFixer") Object var9,
			@TargetType(target = "net.minecraft.server.ServerResources") Object var10,
			@TargetType(target = "com.mojang.authlib.minecraft.MinecraftSessionService") Object var11,
			@TargetType(target = "com.mojang.authlib.GameProfileRepository") Object var12,
			@TargetType(target = "net.minecraft.server.players.GameProfileCache") Object var13,
			@TargetType(target = "net.minecraft.server.level.progress.ChunkProgressListenerFactory") Object var14) {
		if (firstLoad) {
			Modloader.getModloader().dispatchEvent("mods.setuploader", SpigotServerModification.class.getClassLoader());
			CyanCore.setPhase(LoadPhase.PRELOAD);
			Modloader.getModloader().dispatchEvent("mods.preinit");
		}
	}

	@Constructor
	@InjectAt(location = InjectLocation.HEAD, targetCall = "getMillis()", targetOwner = "net.minecraft.Util")
	public static void ctor3(@TargetType(target = "joptsimple.OptionSet") Object var1,
			@TargetType(target = "net.minecraft.world.level.DataPackConfig") Object var2, Thread var3,
			@TargetType(target = "net.minecraft.core.RegistryAccess$RegistryHolder") Object var4,
			@TargetType(target = "net.minecraft.world.level.storage.LevelStorageSource$LevelStorageAccess") Object var5,
			@TargetType(target = "net.minecraft.world.level.storage.WorldData") Object var6,
			@TargetType(target = "net.minecraft.server.packs.repository.PackRepository") Object var7, Proxy var8,
			@TargetType(target = "com.mojang.datafixers.DataFixer") Object var9,
			@TargetType(target = "net.minecraft.server.ServerResources") Object var10,
			@TargetType(target = "com.mojang.authlib.minecraft.MinecraftSessionService") Object var11,
			@TargetType(target = "com.mojang.authlib.GameProfileRepository") Object var12,
			@TargetType(target = "net.minecraft.server.players.GameProfileCache") Object var13,
			@TargetType(target = "net.minecraft.server.level.progress.ChunkProgressListenerFactory") Object var14) {
		if (firstLoad) {
			CyanCore.setPhase(LoadPhase.INIT);
			Modloader.getModloader().dispatchEvent("mods.init");
		}
	}


}
