package org.asf.cyan.api.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.core.CyanInfo;

public class ModKitController extends CyanComponent {
	static ModKitController instance;
	public void begin(ClassLoader loader) {
		CallTrace.setCallTraceClassLoader(loader);
		instance = this;
		Modloader.getModloader().dispatchEvent("mods.prestartgame");
		Modloader.getModloader().dispatchEvent("mods.prestartgame", loader);
		Version minecraft = Version.fromString(CyanInfo.getMinecraftVersion());
		Version last = Version.fromString("0.0.0");

		String[] classNames = findClassNames(getMainImplementation(), IModKitComponent.class);
		ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
		for (String name : classNames) {
			try {
				classes.add(loader.loadClass(name));
			} catch (ClassNotFoundException e) {
				try {
					classes.add(ClassLoader.getSystemClassLoader().loadClass(name));
				} catch (ClassNotFoundException e1) {
				}
			}
		}
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

		Modloader.getModloader().dispatchEvent("mods.load.regular.start");
		Modloader.getModloader().dispatchEvent("mods.load.regular.start", loader);
	}
}
