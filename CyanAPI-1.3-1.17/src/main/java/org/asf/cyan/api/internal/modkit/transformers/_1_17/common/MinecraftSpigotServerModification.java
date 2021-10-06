package org.asf.cyan.api.internal.modkit.transformers._1_17.common;

import org.asf.cyan.api.fluid.annotations.PlatformOnly;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import modkit.events.core.ServerShutdownEvent;
import modkit.events.core.ServerStartupEvent;
import modkit.events.ingame.level.ServerLevelLoadEvent;
import modkit.events.objects.core.ServerEventObject;
import modkit.events.objects.ingame.level.ServerLevelLoadEventObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;

@FluidTransformer
@TargetClass(target = "net.minecraft.server.MinecraftServer")
@PlatformOnly(LaunchPlatform.SPIGOT)
public abstract class MinecraftSpigotServerModification {

	@InjectAt(location = InjectLocation.HEAD, targetCall = "getMillis()", targetOwner = "net.minecraft.Util", offset = 1)
	protected void runServer() {
		Object obj = this;
		MinecraftServer protocolHooksServer = (MinecraftServer) obj;
		ServerStartupEvent.getInstance().dispatch(new ServerEventObject(protocolHooksServer)).getResult();
	}

	@InjectAt(location = InjectLocation.TAIL)
	private void loadSpawn(ChunkProgressListener listener, ServerLevel world) {
		BlockPos spawn = world.getSharedSpawnPos();
		ResourceLocation path = world.dimension().location();
		ServerLevelLoadEvent.getInstance()
				.dispatch(new ServerLevelLoadEventObject(world.getServer(), world, spawn, path)).getResult();
	}

	@InjectAt(location = InjectLocation.HEAD)
	public void safeShutdown(boolean wait, boolean restart) {
		Object self = this;
		ServerShutdownEvent.getInstance().dispatch(new ServerEventObject((MinecraftServer) self)).getResult();
	}
}
