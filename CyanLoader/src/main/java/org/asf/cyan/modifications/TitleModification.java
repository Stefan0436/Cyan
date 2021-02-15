package org.asf.cyan.modifications;

import java.security.ProtectionDomain;

import org.asf.cyan.fluid.api.ClassLoadHook;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

// TODO: Change to use FluidTransformer once it is finished
/**
 * 
 * Modifies the title of the minecraft main window
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class TitleModification extends ClassLoadHook {

	@Override
	public String targetPath() {
		return "com.mojang.blaze3d.platform.Window";
	}

	@Override
	public void build() {
		addMethodMapping("setTitle", String.class.getTypeName());
	}

	@Override
	public CtClass apply(CtClass cc, ClassPool cp, ClassLoader loader, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer)
			throws NotFoundException, CannotCompileException {
		String meth = mapMethod("setTitle", String.class.getTypeName());
		CtMethod m2 = cc.getDeclaredMethod(meth, new CtClass[] { cp.get(String.class.getTypeName()) });

		m2.addLocalVariable("cyan", cp.get(String.class.getTypeName()));
		m2.addLocalVariable("c", cp.get("java.lang.Class"));

		m2.insertBefore(
				"c = org.asf.cyan.core.CyanCore.getCoreClassLoader().loadClass(\"org.asf.cyan.core.CyanInfo\");\n"
						+ "cyan = c.getMethod(\"getCyanVersion\", new Class[0]).invoke(null, null).toString();\n"
						+ "$1=$1+\" - CyanLoader - Version \"+cyan;");

		return cc;
	}

}
