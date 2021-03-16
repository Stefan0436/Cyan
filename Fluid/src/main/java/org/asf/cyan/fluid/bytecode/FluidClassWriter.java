package org.asf.cyan.fluid.bytecode;

import java.lang.reflect.Modifier;

import org.asf.cyan.fluid.bytecode.sources.LoaderClassSourceProvider;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class FluidClassWriter extends ClassWriter {
	private FluidClassPool pool;
	private ClassLoader classLoader;

	/**
	 * Create a ClassWriter using a class pool to get the class information
	 * 
	 * @param pool        Class pool to use
	 * @param classLoader Fallback class loader
	 */
	public FluidClassWriter(FluidClassPool pool, ClassLoader classLoader) {
		this(pool, ClassWriter.COMPUTE_FRAMES, classLoader);
	}

	/**
	 * Create a ClassWriter using a class pool to get the class information
	 * 
	 * @param pool        Class pool to use
	 * @param flags       The flags to use
	 * @param classLoader Fallback class loader
	 */
	public FluidClassWriter(FluidClassPool pool, int flags, ClassLoader classLoader) {
		super(flags);
		this.pool = pool;
		this.classLoader = classLoader;
	}

	/**
	 * Create a ClassWriter using a class pool to get the class information
	 * 
	 * @param pool        Class pool to use
	 * @param classReader ClassReader to use
	 * @param flags       The flags to use
	 * @param classLoader Fallback class loader
	 */
	public FluidClassWriter(FluidClassPool pool, ClassReader classReader, int flags, ClassLoader classLoader) {
		super(classReader, flags);
		this.pool = pool;
		this.classLoader = classLoader;
	}

	// Ported the original getCommonSuperClass method to FLUID, credits to the
	// developers of ASM
	@Override
	public String getCommonSuperClass(final String type1, final String type2) {
		ClassNode class1 = getClass(type1);
		ClassNode class2 = getClass(type2);
		if (isAssignableFrom(class1, class2)) {
			return type1;
		}
		if (isAssignableFrom(class2, class1)) {
			return type2;
		}
		if (Modifier.isInterface(class1.access) || Modifier.isInterface(class2.access)) {
			return "java/lang/Object";
		} else {
			do {
				class1 = getSuperclass(class1);
				if (class1 == null) {
					return "java/lang/Object";
				}
			} while (!isAssignableFrom(class1, class2));
			return class1.name;
		}
	}

	private ClassNode getClass(String type) {
		if (classLoader != null) {
			pool.addSource(new LoaderClassSourceProvider(classLoader));
		}
		pool.addSource(new LoaderClassSourceProvider(getClassLoader()));

		ClassNode cls = null;
		try {
			cls = pool.getClassNode(type);
		} catch (ClassNotFoundException e) {
			throw new TypeNotPresentException(type, e);
		}
		return cls;
	}

	private ClassNode getSuperclass(ClassNode cls) {
		try {
			if (cls.superName == null)
				return getClass("java/lang/Object");

			return getClass(cls.superName);
		} catch (TypeNotPresentException e) {
			return null;
		}
	}

	private boolean isAssignableFrom(ClassNode cls1, ClassNode cls2) {
		boolean isAssingable = (cls1.name.equals(cls2.name) ? true
				: (cls2.superName == null ? cls1.name.equals("java/lang/Object") : cls2.superName.equals(cls1.name)));
		if (isAssingable)
			return true;
		else {
			ClassNode newCls = getSuperclass(cls2);
			if (newCls == null || (!cls1.name.equals("java/lang/Object")
					&& (cls2.superName == null || cls2.superName.equals("java/lang/Object"))))
				return false;
			else
				return isAssignableFrom(cls1, newCls);
		}
	}
}
