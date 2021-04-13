package org.asf.cyan.fluid;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Stream;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.fluid.api.ClassLoadHook;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.asf.cyan.fluid.deobfuscation.DeobfuscationTarget;
import org.asf.cyan.fluid.deobfuscation.DeobfuscationTargetMap;
import org.asf.cyan.fluid.remapping.FluidClassRemapper;
import org.asf.cyan.fluid.remapping.FluidMemberRemapper;
import org.asf.cyan.fluid.remapping.MAPTYPE;
import org.asf.cyan.fluid.remapping.Mapping;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.MethodVisitor;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

/**
 * 
 * Fluid Runtime Modification Engine - Allows the making of changes to code
 * without needing to recompile jar files, based on Javassist and ASM. (mostly
 * ASM these days)<br />
 * <br />
 * <b>IMPORTANT NOTICE:</b> FLUID heavily depends on the presence of a
 * {@link org.asf.cyan.api.common.CyanComponent CyanComponents} implementation.
 * (such as CyanCore)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class Fluid extends CyanComponent {

	public static String getVersion() {
		URL info = Fluid.class.getResource("/fluid.info");
		StringBuilder builder = new StringBuilder();
		try {
			Scanner sc = new Scanner(info.openStream());
			while (sc.hasNext())
				builder.append(sc.nextLine());
			sc.close();
		} catch (IOException e) {
		}
		return builder.toString();
	}

	static Map<Character, String> descriptors = Map.of('V', "void", 'Z', "boolean", 'I', "int", 'J', "long", 'D',
			"double", 'F', "float", 'S', "short", 'C', "char", 'B', "byte");

	/**
	 * Get the descriptor for a type
	 * 
	 * @param type Input type
	 * @return Descriptor string
	 */
	public static String getDescriptor(String type) {
		String prefix = "";
		while (type.contains("[]")) {
			prefix += "[";
			type = type.substring(0, type.lastIndexOf("["));
		}
		int i = 0;
		for (String desc : descriptors.values()) {
			if (desc.equals(type))
				return prefix + descriptors.keySet().toArray(t -> new Character[t])[i].toString();
			i++;
		}
		if (type == "")
			return "";
		return prefix + "L" + type.replaceAll("\\.", "/") + ";";
	}

	/**
	 * Get the descriptor for a set of types
	 * 
	 * @param types Input types
	 * @return Descriptor string
	 */
	public static String getDescriptors(String[] types) {
		StringBuilder b = new StringBuilder();
		for (String type : types) {
			b.append(getDescriptor(type));
		}
		return b.toString();
	}

	/**
	 * Get the type of a descriptor
	 * 
	 * @param descriptor Input descriptor (only supports single descriptors)
	 * @return Primitive name of the descriptor
	 */
	public static String parseDescriptor(String descriptor) {
		return parseDescriptorIntern(descriptor, 0, descriptor.length())[0].toString();
	}

	private static ArrayList<Runnable> postInitHooks = new ArrayList<Runnable>();

	/**
	 * Register a post-init hook that is called after the FLUID transformer is
	 * ready.
	 */
	public static void registerPostInitHook(Runnable runnable) {
		postInitHooks.add(runnable);
	}

	/**
	 * Parse a descriptor with multiple types
	 * 
	 * @param descriptor Input descriptor
	 * @return Array list of types
	 */
	public static String[] parseMultipleDescriptors(String descriptor) {
		ArrayList<String> argumentTypes = new ArrayList<String>();
		int l = descriptor.length();
		for (int i = 0; i < l; i++) {
			Object[] info = parseDescriptorIntern(descriptor, i, l);
			i = (int) info[1];
			argumentTypes.add(info[0].toString());
		}
		return argumentTypes.toArray(t -> new String[t]);
	}

	private static Object[] parseDescriptorIntern(String descriptor, int start, int l) {
		StringBuilder out = new StringBuilder();
		boolean parseName = false;
		int arrays = 0;
		int i = start;

		for (i = start; i < l; i++) {
			char ch = descriptor.charAt(i);
			if (ch == 'L' && !parseName) {
				parseName = true;
			} else if (!parseName && ch == '[') {
				arrays++;
			} else if (!parseName) {
				out.append(descriptors.get(ch));
				if (arrays != 0) {
					for (int i2 = 0; i2 < arrays; i2++) {
						out.append("[]");
					}
					arrays = 0;
				}
				descriptor = out.toString();
				break;
			} else {
				if (ch == '/')
					out.append('.');
				else if (ch != ';') {
					out.append(ch);
				} else {
					if (arrays != 0) {
						for (int i2 = 0; i2 < arrays; i2++) {
							out.append("[]");
						}
						arrays = 0;
					}
					descriptor = out.toString();
					break;
				}
			}
		}
		out = null;
		return new Object[] { descriptor, i };
	}

	private static ArrayList<String> addedTransformerLocations = new ArrayList<String>();
	private static FluidClassPool transformerPool = FluidClassPool.createEmpty();
	private static ArrayList<Mapping<?>> loadedMappings = new ArrayList<Mapping<?>>();
	private static HashMap<String, String> loadedTransformers = new HashMap<String, String>();
	private static ArrayList<ClassLoadHook> loadedHooks = new ArrayList<ClassLoadHook>();
	private static boolean closed = false;
	private static boolean beforeLoad = true;
	private static boolean warn = true;

	/**
	 * Get the transformer class pool, INTERNAL USE ONLY
	 * 
	 * @return Fluid transformer class pool
	 */
	static FluidClassPool getTransformerPool() {
		return transformerPool;
	}

	/**
	 * Get an array of currently loaded mappings
	 * 
	 * @return Array of loaded mappings
	 */
	public static Mapping<?>[] getMappings() {
		return loadedMappings.toArray(t -> new Mapping<?>[t]);
	}

	/**
	 * Get an array of currently loaded transformers, INTERNAL USE ONLY
	 * 
	 * @return Array of loaded FLUID transformers
	 */
	static String[] getTransformers() {
		return loadedTransformers.keySet().toArray(t -> new String[t]);
	}

	/**
	 * Get an array of currently loaded transformer owners, INTERNAL USE ONLY
	 * 
	 * @return Array of loaded FLUID transformers
	 */
	static String[] getTransformerOwners() {
		return loadedTransformers.values().toArray(t -> new String[t]);
	}

	/**
	 * Get an array of currently loaded hooks, INTERNAL USE ONLY
	 * 
	 * @return Array of loaded FLUID loading hooks
	 */
	static ClassLoadHook[] getHooks() {
		return loadedHooks.toArray(t -> new ClassLoadHook[t]);
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
	 * Open the FLUID mappings loader, ignored after closing (Cyan does this by
	 * itself)
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
	 * @param transformer Fluid transformer to load (type name)
	 * @param source      Transformer source
	 * @throws IllegalStateException  If called after the transformer loader has
	 *                                closed
	 * @throws ClassNotFoundException If the transformer cannot be found
	 */
	public static void registerTransformer(String transformer, URL source)
			throws IllegalStateException, ClassNotFoundException {
		String owner = CallTrace.traceCall().getTypeName();
		if (owner.contains("."))
			owner = owner.substring(owner.lastIndexOf(".") + 1);
		if (owner.contains("$"))
			owner = owner.substring(0, owner.lastIndexOf("$"));
		registerTransformer(transformer, owner, source);
	}

	/**
	 * Register transformers in FLUID, can only be called from CORELOAD (or before
	 * closing fluid's loader)
	 * 
	 * @param transformer Fluid transformer to load (type name)
	 * @param owner       Transformer owner
	 * @param source      Transformer source
	 * @throws IllegalStateException  If called after the transformer loader has
	 *                                closed
	 * @throws ClassNotFoundException If the transformer cannot be found
	 */
	public static void registerTransformer(String transformer, String owner, URL source)
			throws IllegalStateException, ClassNotFoundException {
		if (!closed && !beforeLoad) {
			String simpleName = transformer.replaceAll("/", ".");
			if (simpleName.contains("."))
				simpleName = simpleName.substring(simpleName.lastIndexOf(".") + 1);

			if (!addedTransformerLocations.contains(source.toString())) {
				transformerPool.addSource(source);
				addedTransformerLocations.add(source.toString());
			}

			ClassNode transformerNode = transformerPool.getClassNode(transformer);

			info("Loading transformer " + simpleName + "...");
			if (transformerNode.visibleAnnotations == null || !transformerNode.visibleAnnotations.stream()
					.anyMatch(t -> parseDescriptor(t.desc).equals(FluidTransformer.class.getTypeName())))
				throw new IllegalArgumentException("Transformer does not have the @FluidTransformer annotation, class: "
						+ transformer.replaceAll("/", "."));
			loadedTransformers.put(transformer, owner);
		} else
			throw new IllegalStateException(
					"Cannot register transformers after FLUID has been closed or before it has been opened!");
	}

	/**
	 * Register class loading hooks in FLUID, can only be called from CORELOAD (or
	 * before closing fluid's loader)
	 * 
	 * @param hook Fluid class loading hook to load
	 * @throws IllegalStateException If called after the mappings loader has closed
	 */
	public static void registerHook(ClassLoadHook hook) throws IllegalStateException {
		if (!closed && !beforeLoad) {
			info("Loading class hook " + hook.getClass().getSimpleName() + ", target class: " + hook.targetPath());
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
			if (map != null)
				return map;
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
		Mapping<?> map = mapClassToMapping(classPath,
				t -> Stream.of(t.mappings).anyMatch(t2 -> t2.mappingType == MAPTYPE.PROPERTY && t2.name.equals(pName)));
		if (map != null)
			propertyName = Stream.of(map.mappings)
					.filter(t2 -> t2.mappingType == MAPTYPE.PROPERTY && t2.name.equals(pName)).findFirst()
					.get().obfuscated;
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
		if (System.getProperty("jdk.attach.allowAttachSelf") == null
				|| !System.getProperty("jdk.attach.allowAttachSelf").equals("true")) {
			throw new RuntimeException(
					"Cannot load the FLUID agent without jvm argument -Djdk.attach.allowAttachSelf=true");
		}
		debug("Loading FLUID agent... Searching for its jar...");
		if (!closed && warn) {
			warn("Fluid agent is loading, but the FLUID API was not closed, this is unrecommended and unsafe, you can use Fluid.noAgentWarn() to ignore this message");
		}
		try {
			debug("Initialize FLUID API...");
			FluidAgent.initialize();
			debug("Attaching to vm with PID " + Long.toString(ProcessHandle.current().pid()) + " (self)...");
			final VirtualMachine vm = VirtualMachine.attach(Long.toString(ProcessHandle.current().pid()));
			debug("Finding jar path...");
			String path = new File(FluidAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getCanonicalPath();
			debug("Path: " + path);
			if (System.getProperty("cyanAgentJar") != null && !path.endsWith(".jar")) {
				path = System.getProperty("cyanAgentJar");
				debug("OVERRIDE FROM COMMAND LINE, New path: " + path);
			}
			debug("Loading agent...");
			vm.loadAgent(path);
			debug("Detaching from VM...");
			vm.detach();
			debug("Done.");
		} catch (AgentLoadException | AgentInitializationException | IOException | AttachNotSupportedException
				| URISyntaxException e) {
			error("Failed to load agent", e);
		}
	}

	/**
	 * Create a member remapper by using the deobfuscation map
	 * 
	 * @param mp Input map
	 * @return Remapper programmed with the mappings.
	 */
	public static FluidMemberRemapper createMemberRemapper(DeobfuscationTargetMap mp) {
		return new FluidMemberRemapper(mp);
	}

	/**
	 * Create a class remapper by using the deobfuscation map
	 * 
	 * @param mp Input map
	 * @return Remapper programmed with the mappings.
	 */
	public static FluidClassRemapper createClassRemapper(DeobfuscationTargetMap mp) {
		return new FluidClassRemapper(mp);
	}

	/**
	 * Create an deobfuscation target map
	 * 
	 * @param classes  Classes to add
	 * @param pool     The class pool to use
	 * @param mappings The mappings to use
	 * @return DeobfuscationTargetMap
	 */
	public static DeobfuscationTargetMap createTargetMap(ClassNode[] classes, FluidClassPool pool,
			Mapping<?>... mappings) {
		DeobfuscationTargetMap mp = new DeobfuscationTargetMap();

		int i = 0;
		for (ClassNode cls : classes) {
			for (Mapping<?> root : mappings) {
				for (Mapping<?> clsMapping : root.mappings) {
					if (clsMapping.mappingType == MAPTYPE.CLASS
							&& clsMapping.obfuscated.equals(cls.name.replace("/", "."))) {
						DeobfuscationTarget target = new DeobfuscationTarget();

						target.jvmName = clsMapping.name.replaceAll("\\.", "/");
						target.outputName = clsMapping.name;

						for (MethodNode method : cls.methods) {
							String str = "";
							String desc = method.desc;
							String[] types = Fluid.parseMultipleDescriptors(
									desc.substring(1).substring(0, desc.substring(1).lastIndexOf(")")));

							for (int index = 0; index < types.length; index++) {
								String type = types[index];
								String tSuffix = "";
								if (type.contains("[]")) {
									tSuffix = type.substring(type.indexOf("["));
									type = type.substring(0, type.indexOf("["));
								}
								Mapping<?> mp2 = root.mapClassToMapping(type, t -> true, true);
								if (mp2 != null)
									types[index] = mp2.name + tSuffix;

								if (str.equals(""))
									str = type + tSuffix;
								else
									str += ", " + type + tSuffix;
							}

							for (Mapping<?> methodMap : clsMapping.mappings) {
								if (methodMap.mappingType.equals(MAPTYPE.METHOD)
										&& methodMap.obfuscated.equals(method.name)
										&& Arrays.equals(types, methodMap.argumentTypes)) {
									target.methods.put(methodMap.obfuscated + " " + method.desc, methodMap.name);
									break;
								}
							}
						}

						for (FieldNode field : cls.fields) {
							for (Mapping<?> fieldMap : clsMapping.mappings) {
								if (fieldMap.mappingType.equals(MAPTYPE.PROPERTY)
										&& fieldMap.obfuscated.equals(field.name)) {
									target.fields.put(fieldMap.obfuscated + " " + field.desc, fieldMap.name);
									break;
								}
							}
						}

						if (cls.superName != null
								&& !cls.superName.equals(Object.class.getTypeName().replaceAll("\\.", "/"))) {
							try {
								mapSuperAndInterfaces(root, mp, target, pool.getClassNode(cls.superName), pool);
							} catch (ClassNotFoundException e) {
							}
						}
						for (String inter : cls.interfaces) {
							try {
								mapSuperAndInterfaces(root, mp, target, pool.getClassNode(inter), pool);
							} catch (ClassNotFoundException e) {
							}
						}

						if (cls.name.contains("$")) {
							String host = cls.name.substring(0, cls.name.lastIndexOf("$"));
							try {
								mapNested(root, mp, target, pool.getClassNode(host), pool);
							} catch (ClassNotFoundException e) {
							}
						}

						mp.put(clsMapping.obfuscated.replaceAll("\\.", "/"), target);
						if (i % 100 == 0) {
							info("Mapped " + i + "/" + classes.length + " classes.");
						}
						i++;
						break;
					}
				}
			}
		}
		info("Mapped " + classes.length + "/" + classes.length + " classes.");

		return mp;
	}

	/**
	 * Deobfuscate classes in a pool (Uses loaded mappings)
	 * 
	 * @param pool Class pool
	 * @return DeobfuscationTargetMap for class processing
	 */
	public static DeobfuscationTargetMap deobfuscate(FluidClassPool pool) {
		return deobfuscate(pool, loadedMappings.toArray(t -> new Mapping[t]));
	}

	/**
	 * Deobfuscate classes in a pool
	 * 
	 * @param pool     Class pool
	 * @param mappings Mappings to use for deobfuscation
	 * @return DeobfuscationTargetMap for class processing
	 */
	public static DeobfuscationTargetMap deobfuscate(FluidClassPool pool, Mapping<?>... mappings) {
		ArrayList<ClassNode> nodes = new ArrayList<ClassNode>();
		for (Mapping<?> root : mappings) {
			for (Mapping<?> clsMapping : root.mappings) {
				if (clsMapping.mappingType == MAPTYPE.CLASS) {
					try {
						nodes.add(pool.getClassNode(clsMapping.obfuscated));
					} catch (ClassNotFoundException e) {
						warn("Could not load " + clsMapping.obfuscated + ", the class could not be found.");
					}
				}
			}
		}
		ClassNode[] classes = nodes.toArray(t -> new ClassNode[t]);
		DeobfuscationTargetMap mp = createTargetMap(classes, pool, mappings);
		deobfuscate(classes, pool, mappings);
		return mp;
	}

	/**
	 * Deobfuscate a single class by using mappings loaded by FLUID
	 * 
	 * @param cls  Class to deobfuscate
	 * @param pool Class pool
	 */
	public static void deobfuscate(ClassNode cls, FluidClassPool pool) {
		deobfuscate(cls, pool, loadedMappings.toArray(t -> new Mapping[t]));
	}

	/**
	 * Deobfuscate a set of classes by using mappings loaded by FLUID
	 * 
	 * @param classes Classes to deobfuscate
	 * @param pool    Class pool
	 */
	public static void deobfuscate(ClassNode[] classes, FluidClassPool pool) {
		deobfuscate(classes, pool, loadedMappings.toArray(t -> new Mapping[t]));
	}

	/**
	 * Deobfuscate a single class
	 * 
	 * @param cls      Class to deobfuscate
	 * @param pool     Class pool
	 * @param mappings Mappings to use for deobfuscation
	 */
	public static void deobfuscate(ClassNode cls, FluidClassPool pool, Mapping<?>... mappings) {
		try {
			Optional<Mapping<?>> clsMapping = null;

			Mapping<?> root = Stream.of(mappings).filter(t -> Stream.of(t.mappings).anyMatch(
					t2 -> t2.mappingType.equals(MAPTYPE.CLASS) && t2.obfuscated.equals(cls.name.replaceAll("/", "."))))
					.findFirst().get();

			clsMapping = Stream.of(root.mappings).filter(
					t2 -> t2.mappingType.equals(MAPTYPE.CLASS) && t2.obfuscated.equals(cls.name.replaceAll("/", ".")))
					.findFirst();

			if (clsMapping.isEmpty())
				return;

			deobfuscate(root, clsMapping.get(), cls, pool);
		} catch (Exception e) {
		}
	}

	/**
	 * Deobfuscate a set of classes
	 * 
	 * @param classes  Classes to deobfuscate
	 * @param pool     Class pool
	 * @param mappings Mappings to use for deobfuscation
	 */
	public static void deobfuscate(ClassNode[] classes, FluidClassPool pool, Mapping<?>... mappings) {
		int length = 0;
		for (ClassNode cls : classes) {
			Optional<Mapping<?>> root = Stream.of(mappings).filter(t -> Stream.of(t.mappings).anyMatch(
					t2 -> t2.mappingType.equals(MAPTYPE.CLASS) && t2.obfuscated.equals(cls.name.replaceAll("/", "."))))
					.findFirst();

			if (!root.isEmpty())
				length++;
		}
		int i = 0;
		for (ClassNode cls : classes) {
			try {
				Optional<Mapping<?>> clsMapping = null;

				Optional<Mapping<?>> root = Stream.of(mappings)
						.filter(t -> Stream.of(t.mappings).anyMatch(t2 -> t2.mappingType.equals(MAPTYPE.CLASS)
								&& t2.obfuscated.equals(cls.name.replaceAll("/", "."))))
						.findFirst();

				if (root.isEmpty())
					continue;

				clsMapping = Stream.of(root.get().mappings).filter(t2 -> t2.mappingType.equals(MAPTYPE.CLASS)
						&& t2.obfuscated.equals(cls.name.replaceAll("/", "."))).findFirst();

				if (clsMapping.isEmpty())
					continue;

				deobfuscate(root.get(), clsMapping.get(), cls, pool);

				if (i % 100 == 0) {
					info("Deobfuscated " + i + "/" + length + " classes.");
				}
			} catch (Exception e) {
				error("Failed to deobfuscate " + cls.name.replaceAll("/", "."), e);
			}
			i++;
		}
		info("Deobfuscated " + length + "/" + length + " classes.");
	}

	static void mapSuperAndInterfaces(Mapping<?> root, DeobfuscationTargetMap mp, DeobfuscationTarget target,
			ClassNode cls, FluidClassPool pool) {

		for (Mapping<?> clsMapping : root.mappings) {
			if (clsMapping.obfuscated.equals(cls.name.replaceAll("/", "."))) {
				for (MethodNode method : cls.methods) {
					String str = "";
					String desc = method.desc;
					String[] types = Fluid.parseMultipleDescriptors(
							desc.substring(1).substring(0, desc.substring(1).lastIndexOf(")")));

					for (int index = 0; index < types.length; index++) {
						String type = types[index];
						String tSuffix = "";
						if (type.contains("[]")) {
							tSuffix = type.substring(type.indexOf("["));
							type = type.substring(0, type.indexOf("["));
						}
						Mapping<?> mp2 = root.mapClassToMapping(type, t -> true, true);
						if (mp2 != null)
							types[index] = mp2.name + tSuffix;

						if (str.equals(""))
							str = type + tSuffix;
						else
							str += ", " + type + tSuffix;
					}

					for (Mapping<?> methodMap : clsMapping.mappings) {
						if (methodMap.mappingType.equals(MAPTYPE.METHOD) && !Modifier.isPrivate(method.access)
								&& methodMap.obfuscated.equals(method.name)
								&& Arrays.equals(types, methodMap.argumentTypes)) {
							if (!target.methods.containsKey(methodMap.obfuscated + " " + method.desc))
								target.methods.put(methodMap.obfuscated + " " + method.desc, methodMap.name);
							break;
						}
					}
				}

				for (FieldNode field : cls.fields) {
					for (Mapping<?> fieldMap : clsMapping.mappings) {
						if (fieldMap.mappingType.equals(MAPTYPE.PROPERTY) && !Modifier.isPrivate(field.access)
								&& fieldMap.obfuscated.equals(field.name)) {
							if (!target.fields.containsKey(fieldMap.obfuscated + " " + field.desc))
								target.fields.put(fieldMap.obfuscated + " " + field.desc, fieldMap.name);
							break;
						}
					}
				}

				if (cls.superName != null && !cls.superName.equals(Object.class.getTypeName().replaceAll("\\.", "/"))) {
					try {
						mapSuperAndInterfaces(root, mp, target, pool.getClassNode(cls.superName), pool);
					} catch (ClassNotFoundException e) {
					}
				}
				for (String inter : cls.interfaces) {
					try {
						mapSuperAndInterfaces(root, mp, target, pool.getClassNode(inter), pool);
					} catch (ClassNotFoundException e) {
					}
				}

				break;
			}
		}
	}

	static void mapNested(Mapping<?> root, DeobfuscationTargetMap mp, DeobfuscationTarget target, ClassNode cls,
			FluidClassPool pool) {

		for (Mapping<?> clsMapping : root.mappings) {
			if (clsMapping.obfuscated.equals(cls.name.replaceAll("/", "."))) {
				for (MethodNode method : cls.methods) {
					String str = "";
					String desc = method.desc;
					String[] types = Fluid.parseMultipleDescriptors(
							desc.substring(1).substring(0, desc.substring(1).lastIndexOf(")")));

					for (int index = 0; index < types.length; index++) {
						String type = types[index];
						String tSuffix = "";
						if (type.contains("[]")) {
							tSuffix = type.substring(type.indexOf("["));
							type = type.substring(0, type.indexOf("["));
						}
						Mapping<?> mp2 = root.mapClassToMapping(type, t -> true, true);
						if (mp2 != null)
							types[index] = mp2.name + tSuffix;

						if (str.equals(""))
							str = type + tSuffix;
						else
							str += ", " + type + tSuffix;
					}

					for (Mapping<?> methodMap : clsMapping.mappings) {
						if (methodMap.mappingType.equals(MAPTYPE.METHOD) && !Modifier.isPrivate(method.access)
								&& methodMap.obfuscated.equals(method.name)
								&& Arrays.equals(types, methodMap.argumentTypes)) {
							if (!target.methods.containsKey(methodMap.obfuscated + " " + method.desc))
								target.methods.put(methodMap.obfuscated + " " + method.desc, methodMap.name);
							break;
						}
					}
				}

				for (FieldNode field : cls.fields) {
					for (Mapping<?> fieldMap : clsMapping.mappings) {
						if (fieldMap.mappingType.equals(MAPTYPE.PROPERTY) && !Modifier.isPrivate(field.access)
								&& fieldMap.obfuscated.equals(field.name)) {
							if (!target.fields.containsKey(fieldMap.obfuscated + " " + field.desc))
								target.fields.put(fieldMap.obfuscated + " " + field.desc, fieldMap.name);
							break;
						}
					}
				}

				if (cls.superName != null && !cls.superName.equals(Object.class.getTypeName().replaceAll("\\.", "/"))) {
					try {
						mapSuperAndInterfaces(root, mp, target, pool.getClassNode(cls.superName), pool);
					} catch (ClassNotFoundException e) {
					}
				}
				for (String inter : cls.interfaces) {
					try {
						mapSuperAndInterfaces(root, mp, target, pool.getClassNode(inter), pool);
					} catch (ClassNotFoundException e) {
					}
				}

				if (cls.name.contains("$")) {
					String host = cls.name.substring(0, cls.name.lastIndexOf("$"));
					try {
						mapNested(root, mp, target, pool.getClassNode(host), pool);
					} catch (ClassNotFoundException e) {
					}
				}

				break;
			}
		}
	}

	/**
	 * Deobfuscate a single class
	 * 
	 * @param root       Root mapping to use for deobfuscation
	 * @param clsMapping The mapping to use for this class
	 * @param cls        Class to deobfuscate
	 * @param pool       Class pool
	 */
	public static void deobfuscate(Mapping<?> root, Mapping<?> clsMapping, ClassNode cls, FluidClassPool pool) {
		trace("DEOBFUSCATE class: " + cls.name.replaceAll("/", "."));

		for (MethodNode method : cls.methods) {
			String str = "";
			String desc = method.desc;
			String[] types = Fluid
					.parseMultipleDescriptors(desc.substring(1).substring(0, desc.substring(1).lastIndexOf(")")));

			for (int index = 0; index < types.length; index++) {
				String type = types[index];
				String tSuffix = "";
				if (type.contains("[]")) {
					tSuffix = type.substring(type.indexOf("["));
					type = type.substring(0, type.indexOf("["));
				}
				Mapping<?> mp2 = root.mapClassToMapping(type, t -> true, true);
				if (mp2 != null)
					types[index] = mp2.name + tSuffix;

				if (str.equals(""))
					str = type + tSuffix;
				else
					str += ", " + type + tSuffix;
			}

			for (Mapping<?> methodMap : clsMapping.mappings) {
				if (methodMap.mappingType.equals(MAPTYPE.METHOD) && methodMap.obfuscated.equals(method.name)
						&& Arrays.equals(types, methodMap.argumentTypes)) {
					trace("DEOBFUSCATE method " + methodMap.obfuscated + " (" + str + ") into " + methodMap.name);
					method.name = methodMap.name;
					break;
				}
			}
		}

		for (FieldNode field : cls.fields) {
			for (Mapping<?> fieldMap : clsMapping.mappings) {
				if (fieldMap.mappingType.equals(MAPTYPE.PROPERTY) && fieldMap.obfuscated.equals(field.name)) {
					trace("DEOBFUSCATE field " + fieldMap.obfuscated + " into " + fieldMap.name);
					field.name = fieldMap.name;
					break;
				}
			}
		}

		cls.name = clsMapping.name.replaceAll("\\.", "/");

		trace("DEOBFUSCATED " + clsMapping.name + ", remapping required for name change");
	}

	/**
	 * Process class references, required for deobfuscation to work.<br/>
	 * Run this function after deobfuscate
	 * 
	 * @param remapper Remapper to use
	 * @param pool     Class pool to use
	 * @param cls      The class to process
	 * @return Changed class node
	 */
	public static ClassNode remapClass(FluidClassRemapper remapper, FluidClassPool pool, ClassNode cls) {
		trace("REMAP " + cls.name + ", caller: " + CallTrace.traceCallName());
		ClassWriter writer = new ClassWriter(0);
		ClassVisitor clremapper = new ClassRemapper(writer, remapper);
		cls.accept(clremapper);
		try {
			pool.detachClass(cls.name);
		} catch (ClassNotFoundException e) {

		}
		cls = pool.readClass(cls.name, writer.toByteArray());
		return cls;
	}

	/**
	 * Process class members, required for deobfuscation to work.<br/>
	 * Run this function after remapClass
	 * 
	 * @param remapper Remapper to use
	 * @param pool     Class pool to use
	 * @param cls      The class to process
	 * @return Changed class node
	 */
	public static ClassNode remapClassMembers(FluidMemberRemapper remapper, FluidClassPool pool, ClassNode cls) {
		trace("REMAP " + cls.name + ", caller: " + CallTrace.traceCallName());
		for (MethodNode meth : cls.methods) {
			MethodNode newNode = new MethodNode();
			newNode.desc = meth.desc;
			newNode.name = meth.name;
			newNode.exceptions = meth.exceptions;
			newNode.parameters = meth.parameters;
			newNode.access = meth.access;
			MethodVisitor methremapper = new MethodRemapper(newNode, remapper);
			meth.accept(methremapper);
			meth.instructions = newNode.instructions;
			meth.tryCatchBlocks = newNode.tryCatchBlocks;
			meth.exceptions = newNode.exceptions;
			meth.parameters = newNode.parameters;
			meth.localVariables = newNode.localVariables;
		}
		return cls;
	}

	/**
	 * Remap classes in an array
	 * 
	 * @param memberRemapper Member remapper to use
	 * @param classRemapper  Class remapper to use
	 * @param pool           Class pool to use
	 * @param classesArray   The array of classes to process
	 */
	public static void remapClasses(FluidMemberRemapper memberRemapper, FluidClassRemapper classRemapper,
			FluidClassPool pool, ClassNode[] classesArray) {
		int i = 0;
		info("Remapping class members...");
		for (ClassNode cls : classesArray) {
			remapClassMembers(memberRemapper, pool, cls);
			if (i % 100 == 0) {
				info("Remapped " + i + "/" + classesArray.length + " classes.");
			}
			i++;
		}
		info("Remapped " + classesArray.length + "/" + classesArray.length + " classes.");

		i = 0;
		info("Remapping class references...");
		for (ClassNode cls : classesArray) {
			remapClass(classRemapper, pool, cls);
			if (i % 100 == 0) {
				info("Remapped " + i + "/" + classesArray.length + " classes.");
			}
			i++;
		}
		info("Remapped " + classesArray.length + "/" + classesArray.length + " classes.");
	}

	static Runnable[] getPostInitHooks() {
		return postInitHooks.toArray(t -> new Runnable[t]);
	}

	private static HashMap<String, String> extraAgents = new HashMap<String, String>();

	/**
	 * Adds an agent that is loaded by FLUID.
	 * 
	 * @param agentClass Agent class
	 * @param agentEntry Entry method name (premain or agentmain, null is also an
	 *                   option, it will then only load the class)
	 * @throws IllegalStateException If called after the FLUID API has closed
	 */
	public static void addAgent(String agentClass, String agentEntry) {
		if (!closed) {
			extraAgents.put(agentClass, agentEntry);
		} else
			throw new IllegalStateException(
					"Cannot register transformers after FLUID has been closed or before it has been opened!");
	}

	@SuppressWarnings("unchecked")
	public static HashMap<String, String> getAgents() {
		return (HashMap<String, String>) extraAgents.clone();
	}
}
