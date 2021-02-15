package org.asf.cyan.fluid;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.fluid.api.ClassLoadHook;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.mappings.MAPTYPE;
import org.asf.cyan.fluid.mappings.Mapping;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

public class Fluid extends CyanComponent {
	private static ArrayList<Mapping<?>> loadedMappings = new ArrayList<Mapping<?>>();
	private static ArrayList<Class<FluidTransformer>> loadedTransformers = new ArrayList<Class<FluidTransformer>>();
	private static ArrayList<ClassLoadHook> loadedHooks = new ArrayList<ClassLoadHook>();
	private static boolean closed = false;
	private static boolean beforeLoad = true;
	private static boolean warn = true;
	private static boolean deobfuscate = true;
	
	/**
	 * Get an array of currently loaded transformers, INTERNAL USE ONLY
	 * @return Array of loaded FLUID transformers
	 */
	@SuppressWarnings("unchecked")
	static Class<FluidTransformer>[] getTransformers() {
		return loadedTransformers.toArray(t->(Class<FluidTransformer>[])Array.newInstance(Class.class, t));
	}

	/**
	 * Get an array of currently loaded hooks, INTERNAL USE ONLY
	 * @return Array of loaded FLUID loading hooks
	 */
	static ClassLoadHook[] getHooks() {
		return loadedHooks.toArray(t->new ClassLoadHook[t]);
	}
	
	public static boolean isDeobfuscatorEnabled() {
		return deobfuscate;
	}
	
	/**
	 * Disable the FLUID deobfuscation engine, can only be called before FLUID has been closed (useful for when using other mod loaders together with Cyan)
	 */
	public static void disableDeobfuscator() {
		if (!closed && !beforeLoad) {
			deobfuscate = false;
		} else
			throw new IllegalStateException(
					"Cannot disable the deobfuscator after FLUID has been closed or before it has been opened!");
		deobfuscate = false;
	}

	/**
	 * Close the FLUID mappings loader (Cyan does this itself)
	 */
	public static void closeFluidLoader() {
		if (closed)
			throw new IllegalStateException("Cannot close FLUID more than once!");
		closed = true;
	}

	/**
	 * Open the FLUID mappings loader, ignored after closing (Cyan does this itself)
	 */
	public static void openFluidLoader() {
		if (!beforeLoad)
			throw new IllegalStateException("Cannot re-open FLUID!");
		beforeLoad = false;
	}

	/**
	 * Register transformers in FLUID, can only be called from CORELOAD (or before
	 * closing fluid's loader)
	 * 
	 * @param transformer Fluid transformer to load
	 * @throws IllegalStateException If called after the transformer loader has closed
	 */
	public static void registerTransformer(Class<FluidTransformer> transformer) throws IllegalStateException {
		if (!closed && !beforeLoad) {
			loadedTransformers.add(transformer);
		} else
			throw new IllegalStateException(
					"Cannot register transformers after FLUID has been closed or before it has been opened!");
	}

	/**
	 * Register class loading hooks in FLUID, can only be called from CORELOAD (or before
	 * closing fluid's loader)
	 * 
	 * @param hook Fluid class loading hook to load
	 * @throws IllegalStateException If called after the mappings loader has closed
	 */
	public static void registerHook(ClassLoadHook hook) throws IllegalStateException {
		if (!closed && !beforeLoad) {
			loadedHooks.add(hook);
		} else
			throw new IllegalStateException(
					"Cannot register hooks after FLUID has been closed or before it has been opened!");
	}

	/**
	 * Load mappings into FLUID, can only be called from CORELOAD (or before closing
	 * fluid's loader)
	 * 
	 * @param mappings Fluid mappings to load
	 * @throws IllegalStateException If called after the mappings loader has closed
	 */
	public static void loadMappings(Mapping<?> mappings) throws IllegalStateException {
		if (!closed && !beforeLoad) {
			loadedMappings.add(mappings);
		} else
			throw new IllegalStateException(
					"Cannot add mappings after FLUID has been closed or before it has been opened!");
	}

	/**
	 * Map the class name into its obfuscated counterpart
	 * 
	 * @param input Input class path (deobfuscated)
	 * @return Obfuscated class path or the input if not found
	 */
	public static String mapClass(String input) {
		Mapping<?> map = mapClassToMapping(input, t -> true);
		if (map != null)
			return map.obfuscated;
		return input;
	}

	static Mapping<?> mapClassToMapping(String input, Function<Mapping<?>, Boolean> fn) {
		for (Mapping<?> mappings : loadedMappings) {
			Mapping<?> map = mappings.mapClassToMapping(input, fn, false);
			if (map != null) return map;
		}
		return null;
	}
	
	/**
	 * Map the method name into its obfuscated counterpart
	 * 
	 * @param classPath        Input class path (deobfuscated)
	 * @param methodName       Input method name (deobfuscated)
	 * @param methodParameters Input method parameters (deobfuscated paths)
	 * @return Obfuscated method name or the input if not found
	 */
	public static String mapMethod(String classPath, String methodName, String... methodParameters) {
		return mapMethod(classPath, methodName, false, methodParameters);
	}

	/**
	 * Map the method name into its obfuscated counterpart
	 * 
	 * @param classPath        Input class path (deobfuscated)
	 * @param methodName       Input method name (deobfuscated)
	 * @param getPath          True to return class path and method name, false to
	 *                         only return the method name
	 * @param methodParameters Input method parameters (deobfuscated paths)
	 * @return Obfuscated method path (class and method) or name, depending on
	 *         whether getPath is true.
	 */
	public static String mapMethod(String classPath, String methodName, boolean getPath, String... methodParameters) {
		final String mName = methodName;
		Mapping<?> map = mapClassToMapping(classPath,
				t -> Stream.of(t.mappings).anyMatch(t2 -> t2.mappingType == MAPTYPE.METHOD && t2.name.equals(mName)
						&& Arrays.equals(t2.argumentTypes, methodParameters)));
		if (map != null) {
			classPath = map.obfuscated;
			methodName = Stream.of(map.mappings).filter(t2 -> t2.mappingType == MAPTYPE.METHOD && t2.name.equals(mName)
					&& Arrays.equals(t2.argumentTypes, methodParameters)).findFirst().get().obfuscated;
		}
		if (getPath)
			return classPath + "." + methodName;
		else
			return methodName;
	}

	/**
	 * Map the property name into its obfuscated counterpart
	 * 
	 * @param classPath    Input class path (deobfuscated)
	 * @param propertyName Input property name (deobfuscated)
	 * @return Obfuscated property name or the input if not found
	 */
	public static String mapProperty(String classPath, String propertyName) {
		final String pName = propertyName;
		Mapping<?> map = mapClassToMapping(classPath, t -> Stream.of(t.mappings).anyMatch(
				t2 -> t2.mappingType == MAPTYPE.PROPERTY && t2.name.equals(pName)));
		if (map != null)
			propertyName = Stream.of(map.mappings).filter(
					t2 -> t2.mappingType == MAPTYPE.PROPERTY && t2.name.equals(pName))
					.findFirst().get().obfuscated;
		return propertyName;
	}
	
	/**
	 * Turns off the warning if fluid is not closed before loading the agent 
	 */
	public static void noAgentWarn() {
		warn = false;
	}

	/**
	 * Load the fluid agent
	 */
	public static void loadAgent() {
		if (System.getProperty("jdk.attach.allowAttachSelf") == null || !System.getProperty("jdk.attach.allowAttachSelf").equals("true")) {
			throw new RuntimeException("Cannot load the FLUID agent without jvm argument -Djdk.attach.allowAttachSelf=true");
		}
		if (!closed && warn) {
			warn("Fluid agent is loading, but the FLUID API was not closed, this is unrecommended and unsafe, you can use Fluid.noAgentWarn() to ignore this message");
		}
		try {
			final VirtualMachine vm = VirtualMachine.attach(Long.toString(ProcessHandle.current().pid()));
			String path = FluidAgent.class.getProtectionDomain().getCodeSource().getLocation().getFile();
			if (System.getProperty("cyanAgentJar") != null && !path.endsWith(".jar"))
				path = System.getProperty("cyanAgentJar");
			vm.loadAgent(path);
			vm.detach();
		} catch (AgentLoadException | AgentInitializationException | IOException | AttachNotSupportedException e) {
			error("Failed to load agent", e);
		}
	}
}
