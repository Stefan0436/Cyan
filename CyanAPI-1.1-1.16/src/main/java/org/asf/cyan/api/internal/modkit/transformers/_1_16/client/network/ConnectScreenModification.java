package org.asf.cyan.api.internal.modkit.transformers._1_16.client.network;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;
import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

@FluidTransformer
@TargetClass(target = "net.minecraft.client.gui.screens.ConnectScreen")
public class ConnectScreenModification {

	private Minecraft minecraft;
	private Screen parent;

	@InjectAt(location = InjectLocation.HEAD)
	public void connect(String ip, int port) {
		if (!HandshakeUtils.getImpl().beginHandshake(minecraft, parent, ip, port))
			return;
		return;
	}

}
