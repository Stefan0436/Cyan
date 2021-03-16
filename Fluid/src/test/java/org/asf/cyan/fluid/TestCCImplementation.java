package org.asf.cyan.fluid;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.fluid.implementation.CyanBytecodeExporter;
import org.asf.cyan.fluid.implementation.CyanReportBuilder;
import org.asf.cyan.fluid.implementation.CyanTransformer;
import org.asf.cyan.fluid.implementation.CyanTransformerMetadata;

public class TestCCImplementation extends CyanComponent {

	private static boolean init = false;
	public static boolean isInitialized() {
		return init;
	}
	protected static void initComponent() {
		init = true;
		try {
			debug("Closing FLUID API...");
			Fluid.closeFluidLoader();
		} catch (IllegalStateException e) {
			error("Failed to close FLUID!", e);
		}
	}
	
	@Override
	protected void setupComponents() {
		if (init)
			throw new IllegalStateException("Cyan components have already been initialized.");
		if (LOG == null)
			initLogger();
	}

	@Override
	protected void preInitAllComponents() {
		trace("OPEN FluidAPI Mappings Loader, caller: " + CallTrace.traceCallName());
		try {
			debug("Opening FLUID API...");
			Fluid.openFluidLoader();
		} catch (IllegalStateException e) {
			error("Failed to open FLUID!", e);
		}
		
		trace("INITIALIZE all components, caller: " + CallTrace.traceCallName());
		trace("CREATE ConfigurationBuilder instance, caller: " + CallTrace.traceCallName());
	}

	@Override
	protected void finalizeComponents() {}

	@Override
	protected Class<?>[] getComponentClasses() {
		return new Class<?>[] { CyanTransformer.class, CyanTransformerMetadata.class, CyanBytecodeExporter.class, TestCCImplementation.class, CyanReportBuilder.class };
	}
	
	public static void setDebugLog() {
		if (LOG == null)
			initLogger();
		trace(CallTrace.traceCallName() + " set the log level to DEBUG.");
		Configurator.setLevel("CYAN", Level.DEBUG);
	}
	
	public static void initializeComponents() throws IllegalStateException {
		TestCCImplementation impl = new TestCCImplementation();
		impl.initializeComponentClasses();
	}

}
