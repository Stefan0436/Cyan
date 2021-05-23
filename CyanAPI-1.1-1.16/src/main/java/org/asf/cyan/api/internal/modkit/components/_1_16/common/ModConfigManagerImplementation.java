package org.asf.cyan.api.internal.modkit.components._1_16.common;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;

import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.mods.IBaseMod;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;

import modkit.config.ConfigManager;
import modkit.config.ModConfiguration;

public class ModConfigManagerImplementation<T2 extends IBaseMod> extends ConfigManager<T2> implements IModKitComponent {

	private static HashMap<String, ModConfiguration<?, ?>> configs = new HashMap<String, ModConfiguration<?, ?>>();

	public ModConfigManagerImplementation() {
	}

	private T2 modInstance = null;

	@SuppressWarnings("unchecked")
	public ModConfigManagerImplementation(Class<T2> modClass) {
		this();
		for (Modloader loader : Modloader.getAllModloaders()) {
			IBaseMod md = loader.getModByClass(modClass);
			if (md != null)
				modInstance = (T2) md;
		}
		if (modInstance == null)
			throw new IllegalStateException("Mod not loaded!");
	}

	@Override
	public void initializeComponent() {
		implementation = this;
	}

	@Override
	public <T extends IBaseMod> ConfigManager<T> newInstance(Class<T> mod) {
		return new ModConfigManagerImplementation<T>(mod);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends ModConfiguration<T, T2>> T getConfiguration(Class<T> configClass) throws IOException {
		if (configs.containsKey(configClass.getTypeName()))
			return (T) configs.get(configClass.getTypeName());

		try {
			Constructor<T> ctor = configClass.getDeclaredConstructor(modInstance.getClass());
			ctor.setAccessible(true);
			T conf = ctor.newInstance(modInstance);
			conf.readAll();
			configs.put(configClass.getTypeName(), conf);
			return conf;
		} catch (Exception e) {
			throw new IOException("Cannot load configuration " + configClass.getTypeName(), e);
		}
	}

	@Override
	protected String getMainDir() {
		return MinecraftInstallationToolkit.getMinecraftDirectory().toString();
	}

}
