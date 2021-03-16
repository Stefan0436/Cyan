package org.asf.cyan.mixin.services;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import org.asf.cyan.api.classloading.DynamicClassLoader;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.core.CyanCore;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.asf.cyan.mixin.CyanMixinSupport;
import org.asf.cyan.mixin.providers.CyanMixinClassProvider;
import org.asf.cyan.mixin.providers.CyanMixinClassTracker;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MixinServiceAbstract;

public class CyanMixinService extends MixinServiceAbstract {
	boolean init = false;

	public void setClassPool(FluidClassPool pool) {
		if (this.pool != null)
			throw new IllegalStateException("Pool already set");

		this.pool = pool;
	}

	public FluidClassPool getClassPool() {
		return pool;
	}
	
	private FluidClassPool pool;
	private IMixinTransformer transformer;
	private CyanMixinClassProvider classProvider;
	private DynamicClassLoader loader = null;
	private IClassBytecodeProvider provider;
	private CyanMixinClassTracker tracker;
	private CyanMixinSupport trail;

	private IMixinTransformerFactory transformerFactory;

	public void initialize(IClassBytecodeProvider provider) {
		if (!init)
			init = true;
		else
			throw new UnsupportedOperationException("Already initialized the service!");

		// FIXME: change to the offer system as soon as possible
		try {
			Constructor<?> ctor = Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer$Factory")
					.getDeclaredConstructor();
			ctor.setAccessible(true);
			transformerFactory = (IMixinTransformerFactory) ctor.newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		this.provider = provider;
		MixinBootstrap.getPlatform().addContainer(getPrimaryContainer());
	}

	public void onStartup() {
	}

	@Override
	public String getName() {
		return Modloader.getModloaderName();
	}

	@Override
	public boolean isValid() {
		try {
			Class.forName("cpw.mods.modlauncher.Launcher");
			return false;
		} catch (Exception e) {			
		}
		try {
			Class.forName("net.minecraft.launchwrapper.Launch");
			return false;
		} catch (Exception e) {			
		}
		try {
			Class.forName("org.asf.cyan.CyanLoader");
			return true;
		} catch (Exception e) {			
		}
		return false;
	}

	@Override
	public IClassProvider getClassProvider() {
		if (loader == null) {
			loader = (DynamicClassLoader) CyanCore.getCoreClassLoader();
		}
		if (classProvider == null) {
			classProvider = new CyanMixinClassProvider();
		}

		return classProvider;
	}

	@Override
	public IClassBytecodeProvider getBytecodeProvider() {
		return provider;
	}

	@Override
	public IClassTracker getClassTracker() {
		if (tracker == null)
			tracker = new CyanMixinClassTracker();

		return tracker;
	}

	@Override
	public ITransformerProvider getTransformerProvider() {
		return null;
	}

	@Override
	public IMixinAuditTrail getAuditTrail() {
		if (trail == null)
			trail = new CyanMixinSupport();

		return trail;
	}

	@Override
	public Collection<String> getPlatformAgents() {
		return Arrays.asList("org.asf.cyan.mixin.CyanMixinPlatformServiceAgent");
	}

	@Override
	public IContainerHandle getPrimaryContainer() {
		return CyanMixinClassProvider.getPrimaryContainer();
	}

	@Override
    public CompatibilityLevel getMinCompatibilityLevel() {
        return CompatibilityLevel.JAVA_9;
    }

	@Override
	public InputStream getResourceAsStream(String name) {
		getClassProvider();
		URL res = loader.getResource(name);
		if (res == null)
			throw new RuntimeException("Unable to load resource " + name);
		try {
			return res.openStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private IMixinTransformerFactory getFactory() {
		return transformerFactory;
	}

	public IMixinTransformer getTransformer() {
		if (transformer == null && getFactory() != null) {
			if (MixinEnvironment.getCurrentEnvironment().getActiveTransformer() != null)
				transformer = (IMixinTransformer) MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
			else
				transformer = getFactory().createTransformer();
		}
		return transformer;
	}
}
