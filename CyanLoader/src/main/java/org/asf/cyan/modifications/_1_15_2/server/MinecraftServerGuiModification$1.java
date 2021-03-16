package org.asf.cyan.modifications._1_15_2.server;

import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

/**
 * 
 * Modifies the title of the minecraft server window
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@FluidTransformer
@TargetClass(target = "net.minecraft.server.gui.MinecraftServerGui$1")
public class MinecraftServerGuiModification$1 {

	private JFrame val$frame;

	@InjectAt(location = InjectLocation.HEAD, targetCall = "halt(boolean)", targetOwner = "net.minecraft.server.dedicated.DedicatedServer")
	public void windowClosing(WindowEvent event) {
		val$frame.setTitle(
				"Minecraft Server " + Modloader.getModloaderGameVersion() + " - " + Modloader.getModloaderName()
						+ " - Version " + Modloader.getModloaderVersion() + " - Shutting down...");
	}

}
