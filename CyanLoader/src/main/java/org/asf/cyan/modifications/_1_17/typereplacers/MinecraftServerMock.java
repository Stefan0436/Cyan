package org.asf.cyan.modifications._1_17.typereplacers;

import java.util.List;

import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;

@TargetClass(target = "net.minecraft.server.MinecraftServer")
public abstract class MinecraftServerMock {
	public abstract int getMaxPlayers();
	public abstract int getPlayerCount();
	
	@TargetType(target = "net.minecraft.server.players.PlayerList")
	public abstract List<?> getPlayerList();
}
