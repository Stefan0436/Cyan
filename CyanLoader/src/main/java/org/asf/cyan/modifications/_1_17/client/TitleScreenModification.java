package org.asf.cyan.modifications._1_17.client;

import org.asf.cyan.api.fluid.annotations.VersionRegex;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.versioning.VersionStatus;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.LocalVariable;
import org.asf.cyan.fluid.api.transforming.Reflect;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetName;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

/**
 * 
 * Modifies the main client window to display Cyan version information
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@FluidTransformer
@VersionRegex("^[0-9]\\.[0-9][^5](\\..*)?$")
@TargetClass(target = "net.minecraft.client.gui.screens.TitleScreen")
public class TitleScreenModification {

	@Reflect
	public static void drawString(@TargetType(target = "com.mojang.blaze3d.vertex.PoseStack") Object var0, @TargetType(target = "net.minecraft.client.gui.Font") Object var1, String var2, int var3, int var4, int var5) {}
	
	@Reflect
	public static void drawCenteredString(@TargetType(target = "com.mojang.blaze3d.vertex.PoseStack") Object var0, @TargetType(target = "net.minecraft.client.gui.Font") Object var1, String var2, int var3, int var4, int var5) {}
	
	@TargetType(target = "net.minecraft.client.gui.Font")
	public Object font;
	public int height;
	public int width;

	@InjectAt(location = InjectLocation.HEAD, targetCall = "getCurrentVersion()", targetOwner = "net.minecraft.SharedConstants")
	@TargetName(target = "render")
	public void render1(@TargetType(target = "com.mojang.blaze3d.vertex.PoseStack") Object var1, int var2, int var3, float var4, 
		@LocalVariable float var5,
		@LocalVariable boolean var6,
		@LocalVariable int var7,
		@LocalVariable boolean var8,
		@LocalVariable float var9,
		@LocalVariable int var10
	) {
		String modloaderStr = Modloader.getModloader().toString();
		if (!Modloader.getModloaderVersionStatus().equals(VersionStatus.UNKNOWN))
			modloaderStr = modloaderStr + " (" + Modloader.getModloaderVersionStatus().toString() + ")";

		drawString(var1, font, modloaderStr, 2, height - 10, 16777215 | var10);
		height = height - 10;
	}
	
	@InjectAt(location = InjectLocation.TAIL, targetCall = "drawString(com.mojang.blaze3d.vertex.PoseStack, net.minecraft.client.gui.Font, java.lang.String, int, int, int)")
	@TargetName(target = "render")
	public void render2(@TargetType(target = "com.mojang.blaze3d.vertex.PoseStack") Object var1, int var2, int var3, float var4) {
		height = height + 10;
	}

}
