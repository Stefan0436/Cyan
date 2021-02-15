package org.asf.cyan.mixin;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.service.IMixinAuditTrail;

@CYAN_COMPONENT
public class CyanMixinSupport extends CyanComponent implements IMixinAuditTrail {

	final static String APPLY = "MIXIN_APPLY";
	final static String POSTPROC = "MIXIN_POSTPROC";
	final static String GENERATE = "MIXIN_GENERATE";
	
	@Override
	public void onApply(String className, String mixinName) {
		info("Applying mixin: "+className+"."+mixinName);
	}

	@Override
	public void onPostProcess(String className) {
		debug("Post-processing class: "+className);
	}

	@Override
	public void onGenerate(String className, String generatorName) {
		debug("Generating: "+generatorName+", class: "+className);				
	}

	static boolean componentInitialized = false;

	/**
	 * Initialize the component, gets called from initializeComponents()
	 */
	public static void initComponent() {
		trace("INITIALIZE Main CYAN Mixin Component, caller: " + KDebug.getCallerClassName());
		info("Activating mixin support...");
		MixinBootstrap.init();
		Mixins.addConfiguration("mixins.cyan.json");
	}
	
	/**
	 * Check if the component is already initialized
	 * 
	 * @return True if the component is initialized, false otherwise
	 */
	public static boolean isInitialized() {
		return componentInitialized;
	}
	
	static protected String getMarker() {
		return "CyanMixin";
	}
}
