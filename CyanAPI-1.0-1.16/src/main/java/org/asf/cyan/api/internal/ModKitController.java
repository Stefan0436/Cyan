package org.asf.cyan.api.internal;

import java.lang.reflect.InvocationTargetException;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.core.CyanCore;
import org.asf.cyan.core.CyanInfo;

public class ModKitController extends CyanComponent {
	public void begin(ClassLoader loader) {
		Modloader.getModloader().dispatchEvent("mods.prestartgame");
		Modloader.getModloader().dispatchEvent("mods.prestartgame", loader);
		Version minecraft = Version.fromString(CyanInfo.getMinecraftVersion());
		Version last = Version.fromString("0.0.0");

		CyanCore core = (CyanCore) getMainImplementation();
		Class<?>[] classes = core.findClasses(IModKitComponent.class, loader);
		String selectedPackage = "";

		for (Class<?> cls : classes) {
			String pkg = cls.getPackageName();
			if (!pkg.startsWith("org.asf.cyan.api.internal.modkit.components."))
				continue;

			if (pkg.contains(".common")) {
				pkg = pkg.substring(0, pkg.lastIndexOf(".common"));
			} else if (pkg.contains(".client")) {
				pkg = pkg.substring(0, pkg.lastIndexOf(".client"));
			} else if (pkg.contains(".server")) {
				pkg = pkg.substring(0, pkg.lastIndexOf(".server"));
			}

			String version = pkg.substring(pkg.lastIndexOf(".") + 1).substring(1).replace("_", ".");
			if (Version.fromString(version).isGreaterThan(minecraft) || Version.fromString(version).isLessThan(last))
				continue;

			selectedPackage = pkg;
			last = Version.fromString(version);
		}

		for (Class<?> cls : classes) {
			String pkg = cls.getPackageName();
			if (!pkg.startsWith("org.asf.cyan.api.internal.modkit.components.")
					|| (!pkg.equals(selectedPackage) && !pkg.startsWith(selectedPackage + ".")))
				continue;

			if (pkg.contains(".client") && CyanInfo.getSide() != GameSide.CLIENT) {
				continue;
			} else if (pkg.contains(".server") && CyanInfo.getSide() != GameSide.SERVER) {
				continue;
			}

			try {
				IModKitComponent inst = (IModKitComponent) cls.getConstructor().newInstance();
				inst.initializeComponent();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
