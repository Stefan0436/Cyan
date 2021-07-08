package org.asf.cyan.api.internal.modkit.transformers._1_17.client.network;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;
import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

@FluidTransformer
@TargetClass(target = "net.minecraft.client.gui.screens.ConnectScreen")
public class ConnectScreenModification {

	private Screen parent;

	@InjectAt(location = InjectLocation.HEAD)
	public void connect(@TargetType(target = "net.minecraft.client.Minecraft") final Minecraft minecraft,
			@TargetType(target = "net.minecraft.client.multiplayer.resolver.ServerAddress") final ServerAddress addr) {
		if (!HandshakeUtils.getImpl().beginHandshake(minecraft, parent, addr.getHost(), addr.getPort()))
			return;
		return;
	}

}
