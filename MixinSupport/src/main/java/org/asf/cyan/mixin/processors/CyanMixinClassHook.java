package org.asf.cyan.mixin.processors;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.stream.Stream;

import org.asf.cyan.fluid.api.ClassLoadHook;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.asf.cyan.mixin.services.CyanMixinService;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.service.MixinService;

/**
 * 
 * FLUID-based class hook so that mixin can load on cyan.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class CyanMixinClassHook extends ClassLoadHook {

	private static IMixinTransformer transformer;
	private static MixinEnvironment environment;
	private static boolean setPool = false;
	private static boolean activated = false;
	
	public static void activate() {
		activated = true;
	}
	
	@Override
	public boolean isSilent() {
		return true;
	}

	public CyanMixinClassHook() {
	}

	@Override
	public String getTarget() {
		return "@ANY";
	}

	@Override
	public String targetPath() {
		return "@ANY";
	}

	@Override
	public void build() {
	}

	@Override
	public void apply(ClassNode cc, FluidClassPool cp, ClassLoader loader, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws ClassNotFoundException {
		if (!activated)
			return;
		
		if (transformer == null) {
			environment = MixinEnvironment.getCurrentEnvironment();
			transformer = ((CyanMixinService) MixinService.getService()).getTransformer();
		}
		
		if (MixinService.getService() instanceof CyanMixinService && !setPool) {
			((CyanMixinService)MixinService.getService()).setClassPool(cp);
			setPool = true;
		}
		
		if (transformer == null)
			return;
		
		if (Stream.of(transformer.getClass().getMethods()).anyMatch(t -> t.getName().equals("transformClass"))) {
			Method m;
			try {
				m = transformer.getClass().getMethod("transformClass", MixinEnvironment.class, String.class,
						ClassNode.class);
				m.setAccessible(true);
				m.invoke(transformer, environment, cc.name.replaceAll("/", "."), cc);
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		} else {
			ClassWriter writer = new ClassWriter(0);
			cc.accept(writer);
			byte[] bytecode = transformer.transformClassBytes(cc.name.replaceAll("/", "."), cc.name.replaceAll("/", "."), writer.toByteArray());
			cp.detachClass(cc.name, true);
			cc = cp.readClass(cc.name, bytecode);
		}
	}

}
