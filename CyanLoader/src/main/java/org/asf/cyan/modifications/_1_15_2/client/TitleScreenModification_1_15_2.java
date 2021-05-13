package org.asf.cyan.modifications._1_15_2.client;

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
@VersionRegex("^1\\.15\\.2$")
@TargetClass(target = "net.minecraft.client.gui.screens.TitleScreen")
public class TitleScreenModification_1_15_2 {

	@Reflect
	public void drawString(@TargetType(target = "net.minecraft.client.gui.Font") Object var1, String var2, int var3, int var4, int var5) {}
	
	@Reflect
	public void drawCenteredString(@TargetType(target = "net.minecraft.client.gui.Font") Object var1, String var2, int var3, int var4, int var5) {}
	
	@TargetType(target = "net.minecraft.client.gui.Font")
	public Object font;
	public int height;
	public int width;

	@InjectAt(location = InjectLocation.HEAD, targetCall = "getCurrentVersion()", targetOwner = "net.minecraft.SharedConstants")
	@TargetName(target = "render")
	public void render1(int var1, int var2, float var3,
		@LocalVariable float var4,
		@LocalVariable boolean var5,
		@LocalVariable int var6,
		@LocalVariable boolean var7,
		@LocalVariable float var8,
		@LocalVariable int var9
	) {	
		String modloaderStr = Modloader.getModloader().toString();
		if (!Modloader.getModloaderVersionStatus().equals(VersionStatus.UNKNOWN))
			modloaderStr = modloaderStr + " (" + Modloader.getModloaderVersionStatus().toString() + ")";

		drawString(font, modloaderStr, 2, height - 10, 16777215 | var9);
		height = height - 10;
	}
	
	@InjectAt(location = InjectLocation.TAIL, targetCall = "drawString(net.minecraft.client.gui.Font, java.lang.String, int, int, int)")
	@TargetName(target = "render")
	public void render2(int var1, int var2, float var3) {
		height = height + 10;
	}

}
