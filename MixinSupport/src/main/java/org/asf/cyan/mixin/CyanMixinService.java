package org.asf.cyan.mixin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import org.asf.cyan.api.classloading.CyanClassTracker;
import org.asf.cyan.api.classloading.DynamicURLClassLoader;
import org.asf.cyan.core.CyanCore;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.transformer.MixinTransformationHandler;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MixinServiceAbstract;
import org.spongepowered.asm.util.IConsumer;

public class CyanMixinService extends MixinServiceAbstract {
	boolean init = false;

	IConsumer<Phase> phaseConsumer;
	DynamicURLClassLoader loader = null;
	IClassBytecodeProvider provider;
	MixinTransformationHandler handler;
	CyanClassTracker tracker;
	CyanMixinSupport trail;
	CyanContainerHandle container;

	public void onInit(IClassBytecodeProvider provider) {
		if (!init)
			init = true;
		else
			throw new UnsupportedOperationException("Already initialized the service!");
		this.provider = provider;
	}

	public void onStartup() {
		this.phaseConsumer.accept(Phase.DEFAULT);
	}

	@Override
	public String getName() {
		return "CyanMod";
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public IClassProvider getClassProvider() {
		if (loader == null) {
			loader = (DynamicURLClassLoader) CyanCore.getCoreClassLoader();
		}
		//return loader;
		return null; // FIXME: change when re-enabling mixin support!
	}

	@Override
	public IClassBytecodeProvider getBytecodeProvider() {
		return provider;
	}

	@Override
	public IClassTracker getClassTracker() {
		if (tracker == null)
			tracker = new CyanClassTracker();
		//return tracker;
		return null; // FIXME: change when re-enabling mixin support! (the class tracker no longer implements IClassTracker)
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
		if (container == null)
			container = new CyanContainerHandle();
		return container;
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		getClassProvider();
		URL res = loader.getResource(name);
		if (res == null) throw new RuntimeException("Unable to load resource "+name);
		try {
			return res.openStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
