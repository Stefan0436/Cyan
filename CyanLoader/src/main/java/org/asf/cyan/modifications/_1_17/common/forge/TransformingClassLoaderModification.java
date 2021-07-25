package org.asf.cyan.modifications._1_17.common.forge;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

import org.apache.logging.log4j.LogManager;
import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.fluid.annotations.PlatformOnly;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.fluid.FluidAgent;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetOwner;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

@FluidTransformer
@PlatformOnly(LaunchPlatform.MCP)
@TargetClass(target = "cpw.mods.cl.ModuleClassLoader")
public abstract class TransformingClassLoaderModification {

	@TargetOwner(owner = "java.lang.ClassLoader")
	public abstract Class<?> defineClass(String nm, ByteBuffer buff, ProtectionDomain dom);

	@InjectAt(location = InjectLocation.HEAD)
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (CyanLoader.doNotTransform(name)) {
			return getClass().getClassLoader().loadClass(name);
		}
		try {
			Object[] cyanOverrideStream = CyanLoader.getClassData(name);
			if (cyanOverrideStream != null) {
				byte[] data = ((InputStream) cyanOverrideStream[0]).readAllBytes();
				((InputStream) cyanOverrideStream[0]).close();

				Permissions perms = new Permissions();
				perms.add(new AllPermission());

				Object cl = this;
				return defineClass(name, ByteBuffer.wrap(data),
						new ProtectionDomain(new CodeSource((URL) cyanOverrideStream[1], (Certificate[]) null), perms,
								(ClassLoader) cl, null));
			} else {
				Class<?> loadedCls = FluidAgent.getLoadedClass(name);
				if (loadedCls != null)
					return loadedCls;
			}
		} catch (IOException e) {
			LogManager.getLogger(getClass().getSimpleName())
					.warn("Failed to load " + name + ", delegating to parent classloader!", e);
			return getClass().getClassLoader().loadClass(name);
		}

		return null;
	}

}
