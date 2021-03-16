package org.asf.cyan.mixin;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.events.core.ISynchronizedEventListener;
import org.asf.cyan.api.modloader.Modloader;

import org.asf.cyan.fluid.Fluid;

import org.asf.cyan.mixin.processors.CyanMixinClassHook;
import org.asf.cyan.mixin.providers.FluidBytecodeProvider;
import org.asf.cyan.mixin.services.CyanMixinPhaseHandler;
import org.asf.cyan.mixin.services.CyanMixinService;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;

@CYAN_COMPONENT
public class CyanMixinSupport extends CyanComponent implements IMixinAuditTrail {

	static boolean componentInitialized = false;
	private static FluidBytecodeProvider provider = new FluidBytecodeProvider();

	@Override
	public void onApply(String className, String mixinName) {
		info("Applying mixin: " + className + "." + mixinName);
	}

	@Override
	public void onPostProcess(String className) {
		debug("Post-processing class: " + className);
	}

	@Override
	public void onGenerate(String className, String generatorName) {
		debug("Generating: " + generatorName + ", class: " + className);
	}

	/**
	 * Initialize the component, gets called from initializeComponents()
	 */
	public static void initComponent() {
		trace("INITIALIZE Main CYAN Mixin Component, caller: " + CallTrace.traceCallName());

		Fluid.registerHook(new CyanMixinClassHook());

		MixinBootstrap.init();
		IMixinService service = MixinService.getService();
		if (!(service instanceof CyanMixinService)) {
			error(service.getName() + " is not a CYAN mixin service! Cannot load it!");
			return;
		}

		CyanMixinService cyanMixin = (CyanMixinService) service;
		cyanMixin.initialize(provider);
		MixinBootstrap.getPlatform().inject();

		Modloader.getModloader().attachEventListener("phase.changed", new CyanMixinPhaseHandler());
		Modloader.getModloader().attachEventListener("game.beforestart", new ISynchronizedEventListener() {

			@Override
			public String getListenerName() {
				return "Cyan Mixin Support Startup";
			}

			@Override
			public void received(Object... params) {
				info("Activating mixin support...");
				CyanMixinClassHook.activate();
			}

		});
	}

	/**
	 * Check if the component is already initialized
	 * 
	 * @return True if the component is initialized, false otherwise
	 */
	public static boolean isInitialized() {
		return componentInitialized;
	}

	protected static String getMarker() {
		return "CyanMixin";
	}
}
