package org.asf.cyan.modifications._1_15_2.server;

import java.lang.reflect.Modifier;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.versioning.VersionStatus;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.Modifiers;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;
import org.asf.cyan.modifications._1_15_2.typereplacers.MinecraftServerMock;

/**
 * 
 * Modifies the title of the minecraft server window
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@FluidTransformer
@TargetClass(target = "net.minecraft.server.gui.StatsComponent")
public class StatsComponentModification {
	@Modifiers(modifiers = Modifier.PRIVATE)
	private String[] msgs = null;

	@TargetType(target = "net.minecraft.server.MinecraftServer")
	private final MinecraftServerMock server = null;

	@InjectAt(location = InjectLocation.HEAD)
	private void tick() {
		if (msgs.length == 11) {
			String modloaderStr = Modloader.getModloader().toString();
			if (!Modloader.getModloaderVersionStatus().equals(VersionStatus.UNKNOWN))
				modloaderStr = modloaderStr + " (" + Modloader.getModloaderVersionStatus().toString() + ")";

			msgs = new String[msgs.length + 1];
			msgs[4] = modloaderStr;

			if (Modloader.getModloader().getChildren().length != 0) {
				Modloader second = Modloader.getModloader().getChildren()[0];

				msgs[5] = "Running alongside " + (second.getName().equals("") ? "" : second.getName() + " ")
						+ second.getVersion();

				if (Modloader.getModloader().getChildren().length >= 2) {
					Modloader third = Modloader.getModloader().getChildren()[0];
					msgs[5] += " and " + (third.getName().equals("") ? "" : third.getName() + " ") + third.getVersion();
				}
			}
		}

		if (server.getPlayerList() != null)
			msgs[3] = "Online players: " + server.getPlayerCount() + " / " + server.getMaxPlayers();
		else
			msgs[3] = "Online players: ? / ? (loading)";

		msgs[6] = "Loaded " + Modloader.getModloader().getSimpleName() + " Mods: "
				+ (Modloader.getModloader().getLoadedMods().length
						+ Modloader.getModloader().getLoadedCoremods().length)
				+ " / " + Modloader.getModloader().getKnownModsCount();
	}
}
