package org.asf.cyan.mixin.providers;

import java.net.URL;

import org.asf.cyan.core.CyanCore;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.service.IClassProvider;

public class CyanMixinClassProvider implements IClassProvider {

	private static CyanContainerHandle container = new CyanContainerHandle();
	
	@Override
	public URL[] getClassPath() {
		return CyanCore.getCoreClassLoader().getURLs();
	}

	@Override
	public Class<?> findClass(String name) throws ClassNotFoundException {
		return CyanCore.getCoreClassLoader().loadClass(name);
	}

	@Override
	public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
		return Class.forName(name, initialize, CyanCore.getCoreClassLoader());
	}

	@Override
	public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
		return null;
	}

	public static IContainerHandle getPrimaryContainer() {
		return container;
	}

}
