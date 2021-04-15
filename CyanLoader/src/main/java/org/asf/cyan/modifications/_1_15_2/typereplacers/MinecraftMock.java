package org.asf.cyan.modifications._1_15_2.typereplacers;

import java.io.File;

import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.modifications._1_15_2.client.WindowModification;

@TargetClass(target = "net.minecraft.client.Minecraft")
public class MinecraftMock {

	public final File gameDirectory = null;

	@TargetType(target = "net.minecraft.client.gui.Font")
	public Object font;

	@TargetType(target = "com.mojang.blaze3d.platform.Window")
	private final WindowModification window = null;

	@TargetType(target = "com.mojang.blaze3d.platform.Window")
	public WindowModification getWindow() {
		return this.window;
	}

}
