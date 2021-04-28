package org.asf.cyan.modifications._1_15_2.client;

import org.asf.cyan.api.fluid.annotations.VersionRegex;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.LocalVariable;
import org.asf.cyan.fluid.api.transforming.Reflect;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;
import org.asf.cyan.modifications._1_15_2.typereplacers.MinecraftMock;

/**
 * 
 * Modifies the main client window to display Cyan version information
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@FluidTransformer
@VersionRegex("^[0-9]\\.[0-9][^5](\\..*)?$")
@TargetClass(target = "net.minecraft.client.gui.screens.LoadingOverlay")
public class LoadingOverlayModification {

	@Reflect
	public static void drawString(@TargetType(target = "com.mojang.blaze3d.vertex.PoseStack") Object var0, @TargetType(target = "net.minecraft.client.gui.Font") Object var1, String var2, int var3, int var4, int var5) {}

	@Reflect
	public static void drawCenteredString(@TargetType(target = "com.mojang.blaze3d.vertex.PoseStack") Object var0, @TargetType(target = "net.minecraft.client.gui.Font") Object var1, String var2, int var3, int var4, int var5) {}

	@Reflect
	public static void drawProgressBar(@TargetType(target = "com.mojang.blaze3d.vertex.PoseStack") Object var1, int var2, int var3, int var4, int var5, float var6) {}

	@TargetType(target = "net.minecraft.client.Minecraft")
	private final MinecraftMock minecraft = null;

	@InjectAt(location = InjectLocation.HEAD, targetCall = "fill(com.mojang.blaze3d.vertex.PoseStack, int, int, int, int, int)")
	public void drawProgressBar(@TargetType(target = "com.mojang.blaze3d.vertex.PoseStack") Object var1, int var2, int var3, int var4, int var5, float var6,
		@LocalVariable int var7,
		@LocalVariable int var8,
		@LocalVariable int var9
	) {
		if (var6 < 1.0 && var6 > 0.1)
			drawCenteredString(var1, minecraft.font, "Cyan Mod Loader - Loading mods...", var4 - ((var4 - var2) / 2), var3 - 12, 65535);
	}

}
