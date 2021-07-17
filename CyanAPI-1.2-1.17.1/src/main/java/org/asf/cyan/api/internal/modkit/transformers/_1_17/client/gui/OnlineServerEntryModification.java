package org.asf.cyan.api.internal.modkit.transformers._1_17.client.gui;

import java.util.List;

import org.asf.cyan.api.internal.CyanAPIComponent;
import org.asf.cyan.api.internal.modkit.transformers._1_17.client.network.ServerDataAccessor;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.LocalVariable;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

@FluidTransformer
@TargetClass(target = "net.minecraft.client.gui.screens.multiplayer.ServerSelectionList$OnlineServerEntry")
public class OnlineServerEntryModification {

	private final ServerData serverData = null;
	private final Minecraft minecraft = null;

	@InjectAt(location = InjectLocation.HEAD, targetCall = "width(net.minecraft.network.chat.FormattedText)", targetOwner = "net.minecraft.client.gui.Font")
	public void render(@TargetType(target = "com.mojang.blaze3d.vertex.PoseStack") PoseStack var1, int var2, int var3,
			int var4, int var5, int var6, int var7, int var8, boolean var9, float var10, @LocalVariable boolean var11,
			@SuppressWarnings("rawtypes") @LocalVariable List var13, @LocalVariable Component var14,
			@LocalVariable int var15) {

		if (var11) { // FIXME
			Component newMsg = cyanProcessRender(var14);
			if (newMsg != null) {
				var14 = newMsg;
				var11 = true;
			}
		}
	}

	private Component cyanProcessRender(Component var14) {
		ServerDataAccessor acc = (ServerDataAccessor) serverData;

		if (acc.cyanGetServerData() != null) {
			CyanAPIComponent.test(acc);
		}

		return null;
	}

}
