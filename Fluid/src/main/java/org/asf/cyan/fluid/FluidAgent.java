package org.asf.cyan.fluid;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.fluid.Transformer.AnnotationInfo;
import org.asf.cyan.fluid.api.ClassLoadHook;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.asf.cyan.fluid.bytecode.sources.LoaderClassSourceProvider;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.api.classloading.DynamicClassLoader;
import org.asf.cyan.api.classloading.DynamicClassLoader.LoadedClassProvider;
import org.objectweb.asm.tree.ClassNode;

/**
 * Fluid Agent Class, without this, Fluid won't work
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class FluidAgent extends CyanComponent {

	/**
	 * Main premain startup method
	 * 
	 * @param args Arguments
	 * @param inst Java instrumentation
	 */
	public static void premain(final String args, final Instrumentation inst) {
		agentmain(args, inst);
	}

	public static String getMarker() {
		return "Agent";
	}

	private static FluidClassPool pool;
	private static ArrayList<ClassLoader> knownLoaders = new ArrayList<ClassLoader>();
	private static ArrayList<ClassLoadHook> hooks = new ArrayList<ClassLoadHook>();
	private static HashMap<String, String> transformerOwners = new HashMap<String, String>();
	private static HashMap<String, ArrayList<ClassNode>> transformers = new HashMap<String, ArrayList<ClassNode>>();
	private static boolean initialized = false;
	private static boolean loaded = false;

	public static void addChildAgent() {

	}

	public static void initialize() {
		if (initialized)
			throw new IllegalStateException("Cannot re-initialize FLUID!");

		for (ClassLoadHook hook : Fluid.getHooks()) {
			String target = Fluid.mapClass(hook.targetPath());
			hook.build();
			hook.intialize(target.replaceAll("\\.", "/"));
			hooks.add(hook);
		}

		int index = 0;
		for (String transformer : Fluid.getTransformers()) {
			ClassNode transformerNode;
			try {
				transformerNode = Fluid.getTransformerPool().getClassNode(transformer);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}

			String target = null;
			for (AnnotationInfo anno : AnnotationInfo.create(transformerNode)) {
				if (anno.is(TargetClass.class)) {
					target = anno.get("target");
				}
			}
			if (target != null) {
				target = Fluid.mapClass(target);
				ArrayList<ClassNode> trs = transformers.getOrDefault(target.replaceAll("\\.", "/"),
						new ArrayList<ClassNode>());
				trs.add(transformerNode);
				transformers.put(target.replaceAll("\\.", "/"), trs);
				transformerOwners.put(transformerNode.name, Fluid.getTransformerOwners()[index]);
			}
			index++;
		}

		pool = FluidClassPool.create();

		for (URL u : Fluid.getTransformerPool().getURLSources()) {
			if (!Stream.of(pool.getURLSources()).anyMatch(t -> t.toString().equals(u.toString()))) {
				pool.addSource(u);
			}
		}

		initialized = true;

		for (Runnable hook : Fluid.getPostInitHooks()) {
			hook.run();
		}
	}

	static boolean ranHooks = false;
	private static boolean loadedAgents = false;
	private static Instrumentation inst = null;

	/**
	 * Adds the given file to the system class path
	 * 
	 * @param f File to add
	 * @throws IOException If adding the jar fails
	 */
	public static void addToClassPath(File f) throws IOException {
		inst.appendToSystemClassLoaderSearch(new JarFile(f));
	}

	/**
	 * Main agent startup method
	 * 
	 * @param args Arguments
	 * @param inst Java instrumentation
	 */
	public static void agentmain(final String args, final Instrumentation inst) {
		if (FluidAgent.inst == null)
			FluidAgent.inst = inst;
		if (loaded)
			return;

		loaded = true;
		ClassLoader ld = FluidAgent.class.getClassLoader();

		inst.addTransformer(new ClassFileTransformer() {
			@Override
			public synchronized byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
					ProtectionDomain protectionDomain, byte[] classfileBuffer) {
				if (pool == null)
					return null;

				if (!loadedAgents) {
					loadedAgents = true;

					if (!DynamicClassLoader.knowsLoadedClassProvider("fluidagent")) {
						DynamicClassLoader.registerLoadedClassProvider(new LoadedClassProvider() {

							@Override
							public String name() {
								return "fluidagent";
							}

							@Override
							public Class<?> provide(String name) {
								return getLoadedClass(name);
							}
						});
					}

					Fluid.getAgents().forEach((cls, meth) -> {
						try {
							Class<?> agent = ld.loadClass(cls);
							if (meth != null) {
								Method mth = agent.getMethod(meth, String.class, Instrumentation.class);
								mth.invoke(null, args, inst);
							}
						} catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException
								| InvocationTargetException | NoSuchMethodException | SecurityException e) {
						}
					});
				}

				if (loader != null && !knownLoaders.contains(loader)) {
					knownLoaders.add(loader);
					pool.addSource(new LoaderClassSourceProvider(loader));
				}

				boolean match = false;
				boolean transformerMatch = false;
				if (hooks.stream().anyMatch(t -> {
					String target = t.getTarget();
					if (target.equals("@ANY"))
						return true;

					return target.equals(className);
				})) {
					match = true;
				}
				if (transformers.keySet().stream().anyMatch(t -> t.equals(className))) {
					transformerMatch = true;
				}

				if (!match && !transformerMatch) {
					try {
						pool.rewriteClass(className, classfileBuffer);
					} catch (ClassNotFoundException e) {
						pool.readClass(className, classfileBuffer);
					}
					return null;
				}

				byte[] bytecode = null;
				if (match) {
					ClassNode cls;
					try {
						cls = pool.rewriteClass(className, classfileBuffer);
					} catch (ClassNotFoundException e) {
						cls = pool.readClass(className, classfileBuffer);
					}
					ClassNode cc = cls;

					hooks.stream().filter(t -> {
						String target = t.getTarget();
						if (target.equals("@ANY"))
							return true;
						return target.equals(className);
					}).forEach(hook -> {
						try {
							if (!hook.isSilent())
								debug("Applying hook " + hook.getClass().getTypeName() + " to class " + className);

							hook.apply(cc, pool, loader, classBeingRedefined, protectionDomain, classfileBuffer);
						} catch (ClassNotFoundException e) {
							error("FLUID hook apply failed, hook type: " + hook.getClass().getTypeName(), e);
						}
					});

					bytecode = pool.getByteCode(cc.name);
				}
				if (transformerMatch) {
					String clName = className.replaceAll("/", ".");
					for (Mapping<?> map : Fluid.getMappings()) {
						boolean found = false;
						for (Mapping<?> mp : map.mappings) {
							if (mp.obfuscated.equals(clName)) {
								clName = mp.name;
								found = true;
								break;
							}
						}
						if (found)
							break;
					}

					ClassNode cls = null;
					if (bytecode != null) {
						try {
							cls = pool.rewriteClass(className, bytecode);
						} catch (ClassNotFoundException e) {
							cls = pool.readClass(className, bytecode);
						}
					} else {
						try {
							cls = pool.rewriteClass(className, classfileBuffer);
						} catch (ClassNotFoundException e) {
							cls = pool.readClass(className, classfileBuffer);
						}
					}

					Transformer.transform(cls, transformerOwners, transformers, clName, className, pool,
							Fluid.getTransformerPool(), loader);
					bytecode = pool.getByteCode(cls.name);
				} else {
					if (bytecode != null) {
						try {
							pool.rewriteClass(className, bytecode);
						} catch (ClassNotFoundException e) {
							pool.readClass(className, bytecode);
						}
					} else {
						try {
							pool.rewriteClass(className, classfileBuffer);
						} catch (ClassNotFoundException e) {
							pool.readClass(className, classfileBuffer);
						}
					}
				}

				return bytecode;
			}
		});
	}

	public static void forAllClasses(Consumer<Class<?>> function) {
		Class<?>[] classes = inst.getAllLoadedClasses();
		for (Class<?> cls : classes)
			function.accept(cls);
	}

	public static Class<?> getLoadedClass(String name) {
		Class<?>[] classes = inst.getAllLoadedClasses();
		for (Class<?> cls : classes)
			if (cls.getTypeName().equals(name))
				return cls;
		return null;
	}
}
