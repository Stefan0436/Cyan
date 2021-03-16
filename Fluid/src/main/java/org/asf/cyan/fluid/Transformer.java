package org.asf.cyan.fluid;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.asf.cyan.fluid.bytecode.FluidClassWriter;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
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
	protected abstract void transformInternal(ClassNode cls, HashMap<String, String> transformerOwners, HashMap<String, ArrayList<ClassNode>> transformers, String clName, String loadingName, FluidClassPool pool, FluidClassPool transformerPool);

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
	protected abstract MethodNode remapFMI(FluidMethodInfo self, String clName, ClassNode transformerNode, FluidMethodInfo method, boolean fullRemap, FluidClassPool pool);
	protected abstract FluidMethodInfo applyFMI(FluidMethodInfo self, MethodNode method);
	protected abstract FluidMethodInfo applyFMI(FluidMethodInfo self, String owner, MethodInsnNode method);
	protected abstract FluidMethodInfo transformFMI(FluidMethodInfo self, InsnList instructions, ClassNode transformerNode, String clName, ClassNode cls, FluidClassPool pool);
	
	// AnnoInfo
	protected abstract AnnotationInfo createAnnoInfo(AnnotationNode node);
	protected abstract AnnotationInfo[] createAnnoInfo(AbstractInsnNode node);
	protected abstract AnnotationInfo[] createAnnoInfo(MethodNode node);
	protected abstract AnnotationInfo[] createAnnoInfo(FieldNode node);
	protected abstract AnnotationInfo[] createAnnoInfo(ClassNode node);
	
	/**
	 * Assigns the implementation FLUID uses. (recommended to extend the CYAN implementation)
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
	public static byte[] transform(ClassNode cls, HashMap<String, String> transformerOwners, HashMap<String, ArrayList<ClassNode>> transformers, String clName,
			String loadingName, FluidClassPool pool, FluidClassPool transformerPool, ClassLoader loader) {
		selectedTransformer.transformInternal(cls, transformerOwners, transformers, clName, loadingName, pool, transformerPool);
		
		FluidClassWriter clsWriter = new FluidClassWriter(pool, ClassWriter.COMPUTE_FRAMES, loader);
		cls.accept(clsWriter);
		byte[] bytecode = clsWriter.toByteArray();

		try {
			pool.detachClass(cls.name, true);
		} catch (ClassNotFoundException e) {
		}
		cls = pool.readClass(cls.name, bytecode);
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

		public MethodNode remap(String clName, ClassNode transformerNode, FluidMethodInfo method, boolean fullRemap, FluidClassPool pool) {
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
