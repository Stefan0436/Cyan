package org.asf.cyan.modifications._1_17.server;

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
@VersionRegex("^[0-9]\\.[0-9][^5](\\..*)?$")
@TargetClass(target = "net.minecraft.server.MinecraftServer")
@PlatformExclude(LaunchPlatform.SPIGOT)
public class MinecraftServerModification {

	private static boolean firstLoad;

	@Constructor(clinit = true)
	@InjectAt(location = InjectLocation.TAIL)
	public static void clinit() {
		firstLoad = true;
	}

	@Constructor
	@InjectAt(location = InjectLocation.TAIL)
	public static void ctor1(Thread var1,
			@TargetType(target = "net.minecraft.core.RegistryAccess$RegistryHolder") Object var2,
			@TargetType(target = "net.minecraft.world.level.storage.LevelStorageSource$LevelStorageAccess") Object var3,
			@TargetType(target = "net.minecraft.world.level.storage.WorldData") Object var4,
			@TargetType(target = "net.minecraft.server.packs.repository.PackRepository") Object var5, Proxy var6,
			@TargetType(target = "com.mojang.datafixers.DataFixer") Object var7,
			@TargetType(target = "net.minecraft.server.ServerResources") Object var8,
			@TargetType(target = "com.mojang.authlib.minecraft.MinecraftSessionService") Object var9,
			@TargetType(target = "com.mojang.authlib.GameProfileRepository") Object var10,
			@TargetType(target = "net.minecraft.server.players.GameProfileCache") Object var11,
			@TargetType(target = "net.minecraft.server.level.progress.ChunkProgressListenerFactory") Object var12) {
		if (firstLoad) {
			CyanCore.setPhase(LoadPhase.RUNTIME);
			Modloader.getModloader().dispatchEvent("mods.runtimestart");

			firstLoad = false;
		}
	}

	@Constructor
	@InjectAt(location = InjectLocation.HEAD)
	public static void ctor2(Thread var1,
			@TargetType(target = "net.minecraft.core.RegistryAccess$RegistryHolder") Object var2,
			@TargetType(target = "net.minecraft.world.level.storage.LevelStorageSource$LevelStorageAccess") Object var3,
			@TargetType(target = "net.minecraft.world.level.storage.WorldData") Object var4,
			@TargetType(target = "net.minecraft.server.packs.repository.PackRepository") Object var5, Proxy var6,
			@TargetType(target = "com.mojang.datafixers.DataFixer") Object var7,
			@TargetType(target = "net.minecraft.server.ServerResources") Object var8,
			@TargetType(target = "com.mojang.authlib.minecraft.MinecraftSessionService") Object var9,
			@TargetType(target = "com.mojang.authlib.GameProfileRepository") Object var10,
			@TargetType(target = "net.minecraft.server.players.GameProfileCache") Object var11,
			@TargetType(target = "net.minecraft.server.level.progress.ChunkProgressListenerFactory") Object var12) {
		if (firstLoad)
			Modloader.getModloader().dispatchEvent("mods.setuploader", MinecraftServerModification.class.getClassLoader());
		CyanCore.setPhase(LoadPhase.PRELOAD);
		if (firstLoad) {
			Modloader.getModloader().dispatchEvent("mods.preinit");
		}
	}

	@Constructor
	@InjectAt(location = InjectLocation.HEAD, targetCall = "getMillis()", targetOwner = "net.minecraft.Util")
	public static void ctor3(Thread var1,
			@TargetType(target = "net.minecraft.core.RegistryAccess$RegistryHolder") Object var2,
			@TargetType(target = "net.minecraft.world.level.storage.LevelStorageSource$LevelStorageAccess") Object var3,
			@TargetType(target = "net.minecraft.world.level.storage.WorldData") Object var4,
			@TargetType(target = "net.minecraft.server.packs.repository.PackRepository") Object var5, Proxy var6,
			@TargetType(target = "com.mojang.datafixers.DataFixer") Object var7,
			@TargetType(target = "net.minecraft.server.ServerResources") Object var8,
			@TargetType(target = "com.mojang.authlib.minecraft.MinecraftSessionService") Object var9,
			@TargetType(target = "com.mojang.authlib.GameProfileRepository") Object var10,
			@TargetType(target = "net.minecraft.server.players.GameProfileCache") Object var11,
			@TargetType(target = "net.minecraft.server.level.progress.ChunkProgressListenerFactory") Object var12) {
		if (firstLoad) {
			CyanCore.setPhase(LoadPhase.INIT);
			Modloader.getModloader().dispatchEvent("mods.init");
		}
	}

	@Constructor
	@InjectAt(location = InjectLocation.HEAD, targetCall = "<init>(net.minecraft.server.MinecraftServer)", targetOwner = "net.minecraft.server.network.ServerConnectionListener")
	public static void ctor4(Thread var1,
			@TargetType(target = "net.minecraft.core.RegistryAccess$RegistryHolder") Object var2,
			@TargetType(target = "net.minecraft.world.level.storage.LevelStorageSource$LevelStorageAccess") Object var3,
			@TargetType(target = "net.minecraft.world.level.storage.WorldData") Object var4,
			@TargetType(target = "net.minecraft.server.packs.repository.PackRepository") Object var5, Proxy var6,
			@TargetType(target = "com.mojang.datafixers.DataFixer") Object var7,
			@TargetType(target = "net.minecraft.server.ServerResources") Object var8,
			@TargetType(target = "com.mojang.authlib.minecraft.MinecraftSessionService") Object var9,
			@TargetType(target = "com.mojang.authlib.GameProfileRepository") Object var10,
			@TargetType(target = "net.minecraft.server.players.GameProfileCache") Object var11,
			@TargetType(target = "net.minecraft.server.level.progress.ChunkProgressListenerFactory") Object var12) {
		if (firstLoad) {
			CyanCore.setPhase(LoadPhase.POSTINIT);
			Modloader.getModloader().dispatchEvent("mods.postinit");
		}
	}
}
