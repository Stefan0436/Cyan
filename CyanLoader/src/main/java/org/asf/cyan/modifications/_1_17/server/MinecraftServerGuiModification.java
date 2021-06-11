package org.asf.cyan.modifications._1_17.server;

import javax.swing.JFrame;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.LocalVariable;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

/**
 * 
 * Modifies the title of the minecraft server window
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@FluidTransformer
@TargetClass(target = "net.minecraft.server.gui.MinecraftServerGui")
public class MinecraftServerGuiModification {

	@InjectAt(location = InjectLocation.HEAD, targetCall = "setDefaultCloseOperation(int)", targetOwner = "javax.swing.JFrame")
	@TargetType(target = "net.minecraft.server.gui.MinecraftServerGui")
	public static void showFrameFor(
			@TargetType(target = "net.minecraft.server.dedicated.DedicatedServer") final Object var1,
			@LocalVariable JFrame var2) {
		var2.setTitle("Minecraft Server " + Modloader.getModloaderGameVersion() + " - " + Modloader.getModloaderName()
				+ " - Version " + Modloader.getModloaderVersion());
	}
	
	@InjectAt(location = InjectLocation.HEAD)
	public void print(javax.swing.JTextArea a, javax.swing.JScrollPane p, java.lang.String s) {
		s = s.replaceAll("\\§[0-9a-fk-r]", "");
	}
}
