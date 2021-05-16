package org.asf.cyan.fluid;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.fluid.api.transforming.ASM;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.Erase;
import org.asf.cyan.fluid.api.transforming.Exclude;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.Modifiers;
import org.asf.cyan.fluid.api.transforming.Reflect;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;
import org.asf.cyan.fluid.api.transforming.information.metadata.TransformerMetadata;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.asf.cyan.fluid.bytecode.FluidClassWriter;
import org.asf.cyan.fluid.remapping.MAPTYPE;
import org.asf.cyan.fluid.remapping.Mapping;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Main FLUID Transforming Engine, called by the agent, can be used manually.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class Transformer extends CyanComponent {
	private static Transformer selectedTransformer;

	// Basic information getters
	protected abstract String getDeobfNameInternal(String clName);

	protected abstract String getImplementationName();

	// Transformer
	protected class TransformContext {
		public ClassNode targetClass;

		public String mappedName;
		public String obfuscatedName;

		public String transformerOwner;

		public String transformerType;
		public ClassNode transformer;

		public FluidClassPool programPool;
		public FluidClassPool transformerPool;
	}

	protected synchronized void runTransformers(ClassNode target, String clName, String loadingName,
			HashMap<String, String> transformerOwners, HashMap<String, ArrayList<ClassNode>> transformers,
			FluidClassPool transformerPool, FluidClassPool programPool) {

		if (!transformers.containsKey(loadingName))
			return;

		ArrayList<ClassNode> arr = transformers.get(loadingName);
		int transformerIndex = 0;

		boolean asmMethods = false;
		ArrayList<String> transformedMethods = new ArrayList<String>();
		ArrayList<String> transformedFields = new ArrayList<String>();

		for (ClassNode transformer : arr) {
			String typeName = transformer.name.replaceAll("/", ".");

			try {
				debug("Applying transformer " + typeName + " to class "
						+ Fluid.mapClass(loadingName.replaceAll("/", ".")));

				TransformContext ctx = new TransformContext();
				ctx.transformerType = typeName;
				ctx.transformer = transformer;
				ctx.mappedName = loadingName;
				ctx.obfuscatedName = clName;
				ctx.targetClass = target;
				ctx.programPool = programPool;
				ctx.transformerPool = transformerPool;

				if (Modifier.isInterface(transformer.access)) {
					if (AnnotationInfo.isAnnotationPresent(Modifiers.class, transformer)) {
						applyClassModifiers(ctx, target.access, Modifier.INTERFACE | AnnotationInfo
								.getAnnotation(Modifiers.class, transformer).<Integer>get("modifiers").intValue());
					}
					for (MethodNode method : transformer.methods) {
						if (AnnotationInfo.isAnnotationPresent(Exclude.class, method))
							continue;

						boolean found = false;
						ArrayList<String> types = new ArrayList<String>();
						FluidMethodInfo info = FluidMethodInfo.create(method);
						String returnType = info.returnType;
						if (AnnotationInfo.isAnnotationPresent(TargetClass.class, method)) {
							returnType = AnnotationInfo.getAnnotation(TargetType.class, method).get("target");
							returnType = Fluid.mapClass(returnType);
						}

						int index = 0;
						for (String type : info.types) {
							String typePath = type;
							for (AnnotationInfo annotation : FluidMethodInfo.getParameterAnnotations(method, index)) {
								if (annotation.is(TargetType.class)) {
									typePath = annotation.get("target");
									typePath = Fluid.mapClass(typePath);
								}
							}
							types.add(typePath);
							index++;
						}

						String methName = Fluid.mapMethod(clName, method.name, types.toArray(new String[types.size()]));
						if (AnnotationInfo.isAnnotationPresent(Constructor.class, method)) {
							methName = AnnotationInfo.getAnnotation(Constructor.class, method).get("clinit", false)
									? "<clinit>"
									: "<init>";
						}
						int newMod = method.access;
						newMod = newMod - Modifier.ABSTRACT;
						if (AnnotationInfo.isAnnotationPresent(Modifiers.class, method)) {
							newMod = AnnotationInfo.getAnnotation(Modifiers.class, method).get("modifiers");
						}
						int oldMod = newMod;

						for (MethodNode methodNode : target.methods) {
							String desc = methodNode.desc;

							String mTypesStr = desc.substring(1, desc.lastIndexOf(")"));
							String mReturnType = Fluid.parseDescriptor(desc.substring(desc.lastIndexOf(")") + 1));

							String[] mTypes = Fluid.parseMultipleDescriptors(mTypesStr);

							if (methodNode.name.equals(methName)
									&& Arrays.equals(mTypes, types.toArray(new String[types.size()]))) {
								debug("Transforming method: " + method.name + "...");
								if (applyMethodInterfaceTransformer(ctx, methodNode, method, oldMod, newMod,
										FluidMethodInfo.create(methodNode.name, mTypes, mReturnType))) {
									found = true;
									break;
								}
							}
						}

						if (!found)
							throw new RuntimeException("Unable to apply access transformer (interface): " + typeName
									+ ", could not apply method: " + method.name
									+ ", its counterpart could not be found.");

						transformedMethods.add(info.name + " " + info.toDescriptor() + " " + clName + " " + method.name
								+ "&" + method.desc + " " + oldMod + " " + newMod);
					}
					target.interfaces.add(transformer.name);
				} else {
					for (MethodNode meth : transformer.methods) {
						if (AnnotationInfo.isAnnotationPresent(Exclude.class, meth))
							continue;

						if (AnnotationInfo.isAnnotationPresent(ASM.class, meth)) {
							asmMethods = true;
							continue;
						}
						if (Modifier.isAbstract(meth.access) || AnnotationInfo.isAnnotationPresent(Reflect.class, meth))
							continue;

						String descriptor = meth.desc;
						FluidMethodInfo info = FluidMethodInfo.create(meth);
						info.remap(clName, transformer, info, programPool);

						String methName = info.name;
						descriptor = info.toDescriptor();

						final String descFinal = descriptor;
						final String methNameFinal = methName;

						if (AnnotationInfo.isAnnotationPresent(Erase.class, meth)) {
							if (!target.methods.stream()
									.anyMatch(t -> t.name.equals(methNameFinal) && t.desc.equals(descFinal))) {
								throw new RuntimeException("Unable to transform method " + methNameFinal + " of class "
										+ clName + " as it could not be found, transformer: " + typeName);
							}

							MethodNode targetNode = target.methods.stream()
									.filter(t -> t.name.equals(methNameFinal) && t.desc.equals(descFinal)).findFirst()
									.get();

							debug("Transforming method: " + targetNode.name + "... (using transformer method: "
									+ meth.name + ")");

							int newMod = targetNode.access;
							if (AnnotationInfo.isAnnotationPresent(Modifiers.class, meth)) {
								newMod = AnnotationInfo.getAnnotation(Modifiers.class, meth).get("modifiers");
							}

							applyMethodRewriteTransformer(ctx, targetNode, meth, newMod, info);

							FluidMethodInfo ninfo = FluidMethodInfo.create(meth);
							ninfo.remap(clName, transformer, ninfo, false, ctx.programPool);

							transformedMethods.add(ninfo.name + " " + ninfo.toDescriptor() + " " + clName + " "
									+ meth.name + "&" + meth.desc + " " + newMod + " " + newMod);
						} else if (AnnotationInfo.isAnnotationPresent(InjectAt.class, meth)) {
							if (!target.methods.stream()
									.anyMatch(t -> t.name.equals(methNameFinal) && t.desc.equals(descFinal))) {
								throw new RuntimeException("Unable to transform method " + methNameFinal + " of class "
										+ clName + " as it could not be found, transformer: " + typeName);
							}

							MethodNode targetNode = target.methods.stream()
									.filter(t -> t.name.equals(methNameFinal) && t.desc.equals(descFinal)).findFirst()
									.get();

							debug("Transforming method: " + targetNode.name + "... (using transformer method: "
									+ meth.name + ")");

							AnnotationInfo targetData = AnnotationInfo.getAnnotation(InjectAt.class, meth);
							String targetMethName = null;
							String targetMethTypes[] = null;
							String targetMethCls = null;
							if (targetData.get("targetCall") != null) {
								FluidMethodInfo targetCall = FluidMethodInfo
										.create(targetData.<String>get("targetCall"));
								targetMethCls = target.name;

								String targetCls = clName;
								if (targetData.get("targetOwner") != null) {
									targetCls = targetData.get("targetOwner");
									targetMethCls = Fluid.mapClass(targetCls).replaceAll("\\.", "/");
								}

								String superName = targetCls;
								ClassNode clsT = null;
								try {
									clsT = ctx.programPool.getClassNode(Fluid.mapClass(superName));
								} catch (ClassNotFoundException e1) {
									superName = null;
								}

								boolean found = false;
								if (Fluid.getMappings().length == 0)
									found = true;

								while (!found && superName != null) {
									for (Mapping<?> map : Fluid.getMappings()) {
										for (Mapping<?> mp : map.mappings) {
											if (mp.name.equals(superName)) {
												if (Stream.of(mp.mappings)
														.anyMatch(t -> t.mappingType == MAPTYPE.METHOD
																&& t.name.equals(targetCall.name)
																&& Arrays.equals(t.argumentTypes, targetCall.types))) {
													found = true;
													break;
												}
											}
										}
										if (found)
											break;
									}
									if (!found) {
										superName = null;
										if (clsT.superName != null && !clsT.superName
												.equals(Object.class.getTypeName().replaceAll("\\.", "/"))) {
											superName = getDeobfName(clsT.superName.replaceAll("/", "."));
											try {
												clsT = ctx.programPool.getClassNode(clsT.superName);
											} catch (ClassNotFoundException e) {
												break;
											}
										}
									}
								}

								targetCall.remap(superName, transformer, targetCall, ctx.programPool);
								targetMethName = targetCall.name;
								targetMethTypes = targetCall.types;
							}

							int newMod = targetNode.access;
							int oldMod = newMod;
							if (AnnotationInfo.isAnnotationPresent(Modifiers.class, meth)) {
								newMod = AnnotationInfo.getAnnotation(Modifiers.class, meth).get("modifiers");
							}

							TargetInfo targetInfo = new TargetInfo();
							targetInfo.location = targetData.<InjectLocation>get("location");
							targetInfo.offset = targetData.get("offset", 0);
							targetInfo.targetMethodClass = targetMethCls;
							targetInfo.targetMethodName = targetMethName;
							targetInfo.targetMethodTypes = targetMethTypes;

							applyInjectAt(ctx, targetInfo, targetNode, meth, oldMod, newMod, info);

							FluidMethodInfo ninfo = FluidMethodInfo.create(meth);
							ninfo.remap(clName, transformer, ninfo, false, ctx.programPool);
							transformedMethods.add(ninfo.name + " " + ninfo.toDescriptor() + " " + clName + " "
									+ meth.name + "&" + meth.desc + " " + oldMod + " " + newMod);
						} else {
							if (meth.name.equals("<init>"))
								continue;

							if (target.methods.stream()
									.anyMatch(t -> t.name.equals(methNameFinal) && t.desc.equals(descFinal))) {
								if (!meth.name.equals("<clinit>"))
									throw new RuntimeException("Unable to add method " + methNameFinal + " to class "
											+ clName
											+ " as a method with the same signature already exists, transformer: "
											+ typeName);
								else
									continue;
							}

							for (int i = 0; i < meth.localVariables.size(); i++) {
								LocalVariableNode lvn = meth.localVariables.get(i);
								if (Fluid.parseDescriptor(lvn.desc).equals(transformer.name.replaceAll("/", "."))) {
									lvn.desc = Fluid.getDescriptor(target.name);
								}

								meth.localVariables.set(i, lvn);
							}

							int newMod = meth.access;
							if (AnnotationInfo.isAnnotationPresent(Modifiers.class, meth)) {
								newMod = AnnotationInfo.getAnnotation(Modifiers.class, meth).get("modifiers");
							}
							int oldMod = meth.access;

							FluidMethodInfo ninfo = createMethod(ctx, meth, methName, oldMod, newMod);

							debug("Created method " + ninfo.name);
							transformedMethods.add(ninfo.name + " " + ninfo.toDescriptor() + " " + clName + " "
									+ meth.name + "&" + meth.desc + " " + oldMod + " " + newMod + " true");
						}
					}

					for (FieldNode field : transformer.fields) {
						String superName = clName;
						ClassNode clsT = null;
						try {
							clsT = ctx.programPool.getClassNode(Fluid.mapClass(superName));
						} catch (ClassNotFoundException e1) {
							superName = null;
						}
						boolean found = false;
						final String fName = field.name;
						if (Fluid.getMappings().length == 0)
							found = true;
						while (!found && superName != null) {
							for (Mapping<?> map : Fluid.getMappings()) {
								for (Mapping<?> mp : map.mappings) {
									if (mp.name.equals(superName)) {
										if (Stream.of(mp.mappings).anyMatch(
												t -> t.mappingType == MAPTYPE.PROPERTY && t.name.equals(fName))) {
											found = true;
											break;
										}
									}
								}
								if (found)
									break;
							}
							if (!found) {
								superName = null;
								if (clsT.superName != null
										&& !clsT.superName.equals(Object.class.getTypeName().replaceAll("\\.", "/"))) {
									superName = getDeobfName(clsT.superName.replaceAll("/", "."));
									try {
										clsT = ctx.programPool.getClassNode(clsT.superName);
									} catch (ClassNotFoundException e) {
										break;
									}
								}
							}
						}

						if (superName == null)
							superName = clName;

						String fname = Fluid.mapProperty(superName, field.name);
						String ftype = Fluid.parseDescriptor(field.desc);

						if (AnnotationInfo.isAnnotationPresent(TargetType.class, field)) {
							ftype = AnnotationInfo.getAnnotation(TargetType.class, field).get("target");
						}

						int newMod = -1;
						if (AnnotationInfo.isAnnotationPresent(Modifiers.class, field)) {
							newMod = AnnotationInfo.getAnnotation(Modifiers.class, field).get("modifiers");
						}
						boolean isNew = false;

						if (!checkField(fname, ctx.programPool, target)) {
							field.desc = Fluid.getDescriptor(Fluid.mapClass(ftype));
							target.fields.add(field);
							isNew = true;
						} else {
							field = getField(fname, ctx.programPool, target);
						}

						int oldMod = field.access;
						if (newMod != -1) {
							field.access = newMod;
						}

						transformedFields.add(fName + " " + Fluid.getDescriptor(ftype) + " " + superName + " " + fName
								+ " " + oldMod + " " + field.access + " " + isNew);
					}

					for (String _interface : transformer.interfaces) {
						if (!target.interfaces.contains(_interface))
							target.interfaces.add(_interface);
					}

					if (asmMethods) {
						debug("Loading transformer " + typeName + " as a class, it contains @ASM methods...");
						try {
							Class<?> transformerCls = ClassLoader.getSystemClassLoader().loadClass(typeName);
							for (Method meth : transformerCls.getMethods()) {
								if (Modifier.isStatic(meth.getModifiers()) && meth.isAnnotationPresent(ASM.class)) {
									debug("Transforming " + clName + " with ASM-based transformer " + meth.getName()
											+ "... Transformer class: " + typeName);
									ArrayList<Object> params = new ArrayList<Object>();
									boolean clsPoolAdded = false;
									boolean clsNameAdded = false;
									boolean error = false;
									int nodes = 0;
									for (Parameter param : meth.getParameters()) {
										if (param.getType().getTypeName().equals(FluidClassPool.class.getTypeName())
												&& !clsPoolAdded) {
											params.add(ctx.programPool);
											clsPoolAdded = true;
										} else if (param.getType().getTypeName().equals(ClassNode.class.getTypeName())
												&& nodes < 2) {
											if (nodes == 0) {
												params.add(ctx.targetClass);
												nodes++;
											} else if (nodes == 1) {
												params.add(ctx.transformer);
												nodes++;
											}
										} else if (param.getType().isAssignableFrom(String.class) && !clsNameAdded) {
											params.add(loadingName);
											clsNameAdded = true;
										} else {
											error("Unable to run @ASM transformer method: " + meth.getName()
													+ ", could not recognize parameter: " + param.getName()
													+ " of type " + param.getType().getTypeName());
											error("Known parameters:");
											error(" - second ClassNode   - transformer class");
											error(" - first ClassNode    - target class");
											error(" - FluidClassPool     - class pool used to load the transformers and classes");
											error(" - String             - deobfuscated class name");

											error = true;
											break;
										}
									}
									if (!error) {
										try {
											meth.invoke(null, params.toArray(t -> new Object[t]));
										} catch (IllegalAccessException | IllegalArgumentException
												| InvocationTargetException e) {
											error("Failed to transform with ASM transformer " + meth.getName()
													+ ", transformer: " + typeName, e);
										}
									}
								}
							}
						} catch (ClassNotFoundException e) {
							error("Failed to load transformer " + typeName + " as a class, needed for @ASM methods.",
									e);
						}
					}
				}

				TransformerMetadata.createMetadata(transformer, transformerOwners.get(transformer.name),
						transformedFields, transformedMethods, ctx.programPool);
				try {
					transformerPool.detachClass(transformer.name);
					transformer = transformerPool.getClassNode(transformer.name);
				} catch (ClassNotFoundException e) {
				}
				arr.set(transformerIndex, transformer);
				transformers.put(loadingName, arr);
				transformerIndex++;
			} catch (Exception ex) {
				fatal("FLUID transformation failed! Transformer: " + typeName, ex);
				File output = new File(Fluid.getDumpDir(), "transformer-backtrace");
				try {
					TransformerMetadata.dumpErrorBacktrace(ex.getClass().getTypeName() + ": " + ex.getMessage(),
							ex.getStackTrace(), output);
				} catch (Exception e) {
					error("Could not dump FLUID transformer metadata, an exception was thrown.", e);
				}
				System.exit(1);
			}
		}
	}

	protected class TargetInfo {
		public InjectLocation location;
		public int offset;

		public String targetMethodName;
		public String targetMethodClass;
		public String[] targetMethodTypes;
	}

	protected abstract void applyClassModifiers(TransformContext context, int oldModifiers, int newModifiers);

	protected abstract boolean applyMethodInterfaceTransformer(TransformContext context, MethodNode target,
			MethodNode transformer, int oldMod, int newMod, FluidMethodInfo methodInfo);

	protected abstract void applyMethodRewriteTransformer(TransformContext context, MethodNode target,
			MethodNode transformer, int newModifiers, FluidMethodInfo targetInfo);

	protected abstract void applyInjectAt(TransformContext context, TargetInfo targetInfo, MethodNode target,
			MethodNode transformer, int oldModifiers, int newModifiers, FluidMethodInfo methodInfo);

	protected abstract FluidMethodInfo createMethod(TransformContext context, MethodNode transformer, String methodName,
			int oldModifiers, int newModifiers);

	// Field getters
	protected abstract boolean checkField(String fname, FluidClassPool pool, ClassNode cls);

	protected abstract FieldNode getField(String fname, FluidClassPool pool, ClassNode cls);

	// FMI
	protected abstract List<AnnotationInfo> getParameterAnnotations(MethodNode method, int param);

	protected abstract FluidMethodInfo createFMI(String name, String[] types, String returnType, String owner);

	protected abstract FluidMethodInfo createFMI(String methodName, String methodDesc);

	protected abstract FluidMethodInfo createFMI(String methodIdentifier);

	protected abstract FluidMethodInfo createFMI(MethodInsnNode method);

	protected abstract FluidMethodInfo createFMI(MethodNode method);

	protected abstract FluidMethodInfo createFMI(String name, String[] types, String returnType);

	protected abstract MethodNode remapFMI(FluidMethodInfo self, String clName, ClassNode transformerNode,
			FluidMethodInfo method, boolean fullRemap, FluidClassPool pool);

	protected abstract FluidMethodInfo applyFMI(FluidMethodInfo self, MethodNode method);

	protected abstract FluidMethodInfo applyFMI(FluidMethodInfo self, String owner, MethodInsnNode method);

	protected abstract FluidMethodInfo transformFMI(FluidMethodInfo self, InsnList instructions,
			ClassNode transformerNode, String clName, ClassNode cls, FluidClassPool pool);

	// AnnoInfo
	protected abstract AnnotationInfo createAnnoInfo(AnnotationNode node);

	protected abstract AnnotationInfo[] createAnnoInfo(AbstractInsnNode node);

	protected abstract AnnotationInfo[] createAnnoInfo(MethodNode node);

	protected abstract AnnotationInfo[] createAnnoInfo(FieldNode node);

	protected abstract AnnotationInfo[] createAnnoInfo(ClassNode node);

	/**
	 * Assigns the implementation FLUID uses. (recommended to extend the CYAN
	 * implementation)
	 * 
	 * @param transformerEngine Transformer engine to use
	 */
	protected static void setImplementation(Transformer transformerEngine) {
		debug("Assigning FLUID Modification Engine... Using the " + transformerEngine.getImplementationName()
				+ " Modification Engine...");
		selectedTransformer = transformerEngine;
	}

	/**
	 * Get the deobfuscated class name of the class specified
	 * 
	 * @param clName Class name do deobfuscated
	 * @return Deobfuscated class name
	 */
	public static String getDeobfName(String clName) {
		return selectedTransformer.getDeobfNameInternal(clName);
	}

	/**
	 * Transform a class (DO NOT CALL MORE THAN ONCE FOR EACH NODE)
	 * 
	 * @param cls               Class to transform
	 * @param transformerOwners Transformer owners
	 * @param transformers      HashMap containing each transformer node (key is the
	 *                          target class, obfuscated name)
	 * @param clName            Deobfuscated class name
	 * @param loadingName       Obfuscated loading path (separated with slashes)
	 * @param pool              Class pool loading the class
	 * @param transformerPool   Class pool containing the transformers
	 */
	public static byte[] transform(ClassNode cls, HashMap<String, String> transformerOwners,
			HashMap<String, ArrayList<ClassNode>> transformers, String clName, String loadingName, FluidClassPool pool,
			FluidClassPool transformerPool, ClassLoader loader) {

		selectedTransformer.runTransformers(cls, clName, loadingName, transformerOwners, transformers, transformerPool,
				pool);

		FluidClassWriter clsWriter = new FluidClassWriter(pool, ClassWriter.COMPUTE_FRAMES, loader);
		cls.accept(clsWriter);
		byte[] bytecode = clsWriter.toByteArray();

		try {
			pool.rewriteClass(cls.name, bytecode);
		} catch (ClassNotFoundException e) {
		}

		return bytecode;
	}

	public static class FluidMethodInfo {
		public String name;
		public String[] types;
		public String returnType;

		public static List<AnnotationInfo> getParameterAnnotations(MethodNode method, int param) {
			return selectedTransformer.getParameterAnnotations(method, param);
		}

		public static FluidMethodInfo create(MethodNode method) {
			return selectedTransformer.createFMI(method);
		}

		public static FluidMethodInfo create(MethodInsnNode method) {
			return selectedTransformer.createFMI(method);
		}

		public static FluidMethodInfo create(String methodName, String methodDesc) {
			return selectedTransformer.createFMI(methodName, methodDesc);
		}

		public static FluidMethodInfo create(String methodIdentifier) {
			return selectedTransformer.createFMI(methodIdentifier);
		}

		public static FluidMethodInfo create(String name, String[] types, String returnType) {
			return selectedTransformer.createFMI(name, types, returnType);
		}

		public static FluidMethodInfo create(String name, String[] types, String returnType, String owner) {
			return selectedTransformer.createFMI(name, types, returnType, owner);
		}

		public MethodNode remap(String clName, ClassNode transformerNode, FluidMethodInfo method, FluidClassPool pool) {
			return remap(clName, transformerNode, method, true, pool);
		}

		public MethodNode remap(String clName, ClassNode transformerNode, FluidMethodInfo method, boolean fullRemap,
				FluidClassPool pool) {
			return selectedTransformer.remapFMI(this, clName, transformerNode, method, fullRemap, pool);
		}

		public FluidMethodInfo apply(MethodNode method) {
			return selectedTransformer.applyFMI(this, method);
		}

		public FluidMethodInfo apply(String owner, MethodInsnNode method) {
			return selectedTransformer.applyFMI(this, owner, method);
		}

		public FluidMethodInfo transform(InsnList instructions, ClassNode transformerNode, String clName, ClassNode cls,
				FluidClassPool pool) {
			return selectedTransformer.transformFMI(this, instructions, transformerNode, clName, cls, pool);
		}

		@Override
		public String toString() {
			return name + " (" + Fluid.getDescriptors(types) + ")" + Fluid.getDescriptor(returnType);
		}

		@Override
		public boolean equals(Object compareTo) {
			return compareTo.toString().equals(toString());
		}

		public String toDescriptor() {
			return "(" + Fluid.getDescriptors(types) + ")" + Fluid.getDescriptor(returnType);
		}
	}

	public static class AnnotationInfo {
		public String name;
		public HashMap<String, Object> values = new HashMap<String, Object>();

		@Override
		public String toString() {
			return name;
		}

		public static AnnotationInfo getAnnotation(Class<?> annotation, ClassNode node) {
			if (node.visibleAnnotations != null) {
				for (AnnotationNode anno : node.visibleAnnotations) {
					AnnotationInfo info = create(anno);
					if (info.name.replaceAll("/", ".").equals(annotation.getTypeName()))
						return info;
				}
			}

			if (node.invisibleAnnotations != null) {
				for (AnnotationNode anno : node.invisibleAnnotations) {
					AnnotationInfo info = create(anno);
					if (info.name.replaceAll("/", ".").equals(annotation.getTypeName()))
						return info;
				}
			}

			return null;
		}

		public static AnnotationInfo getAnnotation(Class<?> annotation, MethodNode node) {
			if (node.visibleAnnotations != null) {
				for (AnnotationNode anno : node.visibleAnnotations) {
					AnnotationInfo info = create(anno);
					if (info.name.replaceAll("/", ".").equals(annotation.getTypeName()))
						return info;
				}
			}

			if (node.invisibleAnnotations != null) {
				for (AnnotationNode anno : node.invisibleAnnotations) {
					AnnotationInfo info = create(anno);
					if (info.name.replaceAll("/", ".").equals(annotation.getTypeName()))
						return info;
				}
			}

			return null;
		}

		public static AnnotationInfo getAnnotation(Class<?> annotation, FieldNode node) {
			if (node.visibleAnnotations != null) {
				for (AnnotationNode anno : node.visibleAnnotations) {
					AnnotationInfo info = create(anno);
					if (info.name.replaceAll("/", ".").equals(annotation.getTypeName()))
						return info;
				}
			}

			if (node.invisibleAnnotations != null) {
				for (AnnotationNode anno : node.invisibleAnnotations) {
					AnnotationInfo info = create(anno);
					if (info.name.replaceAll("/", ".").equals(annotation.getTypeName()))
						return info;
				}
			}

			return null;
		}

		public static AnnotationInfo getAnnotation(Class<?> annotation, AbstractInsnNode node) {
			if (node.visibleTypeAnnotations != null) {
				for (AnnotationNode anno : node.visibleTypeAnnotations) {
					AnnotationInfo info = create(anno);
					if (info.name.replaceAll("/", ".").equals(annotation.getTypeName()))
						return info;
				}
			}

			if (node.invisibleTypeAnnotations != null) {
				for (AnnotationNode anno : node.invisibleTypeAnnotations) {
					AnnotationInfo info = create(anno);
					if (info.name.replaceAll("/", ".").equals(annotation.getTypeName()))
						return info;
				}
			}

			return null;
		}

		public static boolean isAnnotationPresent(AnnotationInfo info, ClassNode cls) {

			if (cls.visibleAnnotations != null) {
				for (AnnotationNode anno : cls.visibleAnnotations) {
					if (create(anno).equals(info))
						return true;
				}
			}

			if (cls.invisibleAnnotations != null) {
				for (AnnotationNode anno : cls.invisibleAnnotations) {
					if (create(anno).equals(info))
						return true;
				}
			}

			return false;
		}

		public static boolean isAnnotationPresent(AnnotationInfo info, MethodNode cls) {

			if (cls.visibleAnnotations != null) {
				for (AnnotationNode anno : cls.visibleAnnotations) {
					if (create(anno).equals(info))
						return true;
				}
			}

			if (cls.invisibleAnnotations != null) {
				for (AnnotationNode anno : cls.invisibleAnnotations) {
					if (create(anno).equals(info))
						return true;
				}
			}

			return false;
		}

		public static boolean isAnnotationPresent(AnnotationInfo info, FieldNode cls) {

			if (cls.visibleAnnotations != null) {
				for (AnnotationNode anno : cls.visibleAnnotations) {
					if (create(anno).equals(info))
						return true;
				}
			}

			if (cls.invisibleAnnotations != null) {
				for (AnnotationNode anno : cls.invisibleAnnotations) {
					if (create(anno).equals(info))
						return true;
				}
			}

			return false;
		}

		public static boolean isAnnotationPresent(AnnotationInfo info, AbstractInsnNode cls) {

			if (cls.visibleTypeAnnotations != null) {
				for (AnnotationNode anno : cls.visibleTypeAnnotations) {
					if (create(anno).equals(info))
						return true;
				}
			}

			if (cls.invisibleTypeAnnotations != null) {
				for (AnnotationNode anno : cls.invisibleTypeAnnotations) {
					if (create(anno).equals(info))
						return true;
				}
			}

			return false;
		}

		public static boolean isAnnotationPresent(Class<? extends Annotation> info, ClassNode cls) {

			if (cls.visibleAnnotations != null) {
				for (AnnotationNode anno : cls.visibleAnnotations) {
					if (create(anno).name.replaceAll("/", ".").equals(info.getTypeName()))
						return true;
				}
			}

			if (cls.invisibleAnnotations != null) {
				for (AnnotationNode anno : cls.invisibleAnnotations) {
					if (create(anno).name.replaceAll("/", ".").equals(info.getTypeName()))
						return true;
				}
			}

			return false;
		}

		public static boolean isAnnotationPresent(Class<? extends Annotation> info, MethodNode cls) {

			if (cls.visibleAnnotations != null) {
				for (AnnotationNode anno : cls.visibleAnnotations) {
					if (create(anno).name.replaceAll("/", ".").equals(info.getTypeName()))
						return true;
				}
			}

			if (cls.invisibleAnnotations != null) {
				for (AnnotationNode anno : cls.invisibleAnnotations) {
					if (create(anno).name.replaceAll("/", ".").equals(info.getTypeName()))
						return true;
				}
			}

			return false;
		}

		public static boolean isAnnotationPresent(Class<? extends Annotation> info, FieldNode cls) {

			if (cls.visibleAnnotations != null) {
				for (AnnotationNode anno : cls.visibleAnnotations) {
					if (create(anno).name.replaceAll("/", ".").equals(info.getTypeName()))
						return true;
				}
			}

			if (cls.invisibleAnnotations != null) {
				for (AnnotationNode anno : cls.invisibleAnnotations) {
					if (create(anno).name.replaceAll("/", ".").equals(info.getTypeName()))
						return true;
				}
			}

			return false;
		}

		public static boolean isAnnotationPresent(Class<? extends Annotation> info, AbstractInsnNode cls) {

			if (cls.visibleTypeAnnotations != null) {
				for (AnnotationNode anno : cls.visibleTypeAnnotations) {
					if (create(anno).name.replaceAll("/", ".").equals(info.getTypeName()))
						return true;
				}
			}

			if (cls.invisibleTypeAnnotations != null) {
				for (AnnotationNode anno : cls.invisibleTypeAnnotations) {
					if (create(anno).name.replaceAll("/", ".").equals(info.getTypeName()))
						return true;
				}
			}

			return false;
		}

		@Override
		public boolean equals(Object compareTo) {
			return compareTo.toString().equals(toString());
		}

		@SuppressWarnings("unchecked")
		public <T> T get(String param, T def) {
			return (T) values.getOrDefault(param, (Object) def);
		}

		public <T> T get(String param) {
			return get(param, null);
		}

		public boolean is(Class<?> cls) {
			return cls.getTypeName().equals(name.replaceAll("/", "."));
		}

		public Class<?> annotationType() {
			try {
				return Class.forName(name.replaceAll("/", "."));
			} catch (ClassNotFoundException e) {
				return null;
			}
		}

		public static AnnotationInfo create(AnnotationNode node) {
			return selectedTransformer.createAnnoInfo(node);
		}

		public static AnnotationInfo[] create(ClassNode node) {
			return selectedTransformer.createAnnoInfo(node);
		}

		public static AnnotationInfo[] create(MethodNode node) {
			return selectedTransformer.createAnnoInfo(node);
		}

		public static AnnotationInfo[] create(FieldNode node) {
			return selectedTransformer.createAnnoInfo(node);
		}

		public static AnnotationInfo[] create(AbstractInsnNode node) {
			return selectedTransformer.createAnnoInfo(node);
		}
	}
}
