package org.asf.cyan.fluid;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.fluid.api.ClassLoadHook;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

/**
 * Fluid Agent Class, without this, Fluid won't work
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class FluidAgent extends CyanComponent {
	/**
	 * Main agent startup method
	 * 
	 * @param args Arguments
	 * @param inst Java instrumentation
	 */
	public static void agentmain(final String args, final Instrumentation inst) {
		ArrayList<ClassLoadHook> hooks = new ArrayList<ClassLoadHook>();
		for (ClassLoadHook hook : Fluid.getHooks()) {
			String target = Fluid.mapClass(hook.targetPath());
			hook.build();
			hook.intialize(target.replaceAll("\\.", "/"));
			hooks.add(hook);
		}

		inst.addTransformer(new ClassFileTransformer() {
			@Override
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
					ProtectionDomain protectionDomain, byte[] classfileBuffer) {

				try {
					boolean match = false;

					if (hooks.stream().anyMatch(t -> t.getTarget().equals(className))) {
						match = true;
					}

					// TODO: transformer match checking

					if (!match)
						return null;

					String newPath = className.replaceAll("/", ".");
					ClassPool cp = ClassPool.getDefault();
					cp.insertClassPath(new ClassClassPath(FluidAgent.class));
					CtClass cc = cp.get(newPath);

					for (ClassLoadHook hook : hooks.stream().filter(t -> t.getTarget().equals(className))
							.toArray(t -> new ClassLoadHook[t])) {
						try {
							debug("Applying hook " + hook.getClass().getTypeName() + " to class " + className);
							cc = hook.apply(cc, cp, loader, classBeingRedefined, protectionDomain, classfileBuffer);
						} catch (NotFoundException | CannotCompileException e) {
							error("FLUID hook apply failed, hook type: " + hook.getClass().getTypeName(), e);
						}
					}

					// TODO: Transformers

					byte[] byteCode = cc.toBytecode();
					cc.detach();
					return byteCode;
				} catch (NotFoundException | IOException | CannotCompileException e) {
					error("FLUID transformation failed", e);
				}

				return null;
			}
		});
	}
}
