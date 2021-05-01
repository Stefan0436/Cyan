package org.asf.cyan.api.internal;

import java.lang.reflect.InvocationTargetException;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.fluid.annotations.PlatformExclude;
import org.asf.cyan.api.fluid.annotations.PlatformOnly;
import org.asf.cyan.api.fluid.annotations.SideOnly;
import org.asf.cyan.api.fluid.annotations.VersionRegex;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.core.CyanInfo;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.api.FluidTransformer;

@CYAN_COMPONENT
public class CyanAPIComponent extends CyanComponent {

	protected static void initComponent() {
		Version minecraft = Version.fromString(CyanInfo.getMinecraftVersion());
		Version last = Version.fromString("0.0.0");

		Class<?>[] classes = findClasses(getMainImplementation(), IModKitComponent.class);
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
				IModKitComponent inst = (IModKitComponent)cls.getConstructor().newInstance();
				inst.initializeComponent();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}
		info("Loading CyanAPI ModKit transformers...");
		minecraft = Version.fromString(CyanInfo.getMinecraftVersion());
		last = Version.fromString("0.0.0");

		selectedPackage = "";
		classes = findAnnotatedClasses(getMainImplementation(), FluidTransformer.class);
		for (Class<?> cls : classes) {
			String pkg = cls.getPackageName();
			if (!pkg.startsWith("org.asf.cyan.api.internal.modkit.transformers."))
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

		try {
			for (Class<?> transformer : classes) {
				String pkg = transformer.getPackageName();
				if (!pkg.startsWith("org.asf.cyan.api.internal.modkit.transformers.")
						|| (!pkg.equals(selectedPackage) && !pkg.startsWith(selectedPackage + ".")))
					continue;

				if (pkg.contains(".client") && CyanInfo.getSide() != GameSide.CLIENT) {
					continue;
				} else if (pkg.contains(".server") && CyanInfo.getSide() != GameSide.SERVER) {
					continue;
				}

				if (transformer.isAnnotationPresent(SideOnly.class)
						&& transformer.getAnnotation(SideOnly.class).value() != Modloader.getModloaderGameSide()) {
					continue;
				} else if (transformer.isAnnotationPresent(PlatformOnly.class) && transformer
						.getAnnotation(PlatformOnly.class).value() != Modloader.getModloaderLaunchPlatform()) {
					continue;
				} else if (transformer.isAnnotationPresent(PlatformExclude.class) && transformer
						.getAnnotation(PlatformExclude.class).value() == Modloader.getModloaderLaunchPlatform()) {
					continue;
				} else if (transformer.isAnnotationPresent(VersionRegex.class)
						&& !transformer.getAnnotation(VersionRegex.class).modloaderVersion()
						&& !Modloader.getModloaderGameVersion()
								.matches(transformer.getAnnotation(VersionRegex.class).value())) {
					continue;
				} else if (transformer.isAnnotationPresent(VersionRegex.class)
						&& transformer.getAnnotation(VersionRegex.class).modloaderVersion()
						&& !Modloader.getModloaderVersion().toString()
								.matches(transformer.getAnnotation(VersionRegex.class).value())) {
					continue;
				}

				Fluid.registerTransformer(transformer.getTypeName(),
						CyanAPIComponent.class.getProtectionDomain().getCodeSource().getLocation());
			}
		} catch (IllegalStateException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
