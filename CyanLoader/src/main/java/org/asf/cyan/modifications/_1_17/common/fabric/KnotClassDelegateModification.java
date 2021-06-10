package org.asf.cyan.modifications._1_17.common.fabric;

import java.io.IOException;
import java.io.InputStream;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.fluid.annotations.PlatformOnly;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

@FluidTransformer
@PlatformOnly(LaunchPlatform.YARN)
@TargetClass(target = "net.fabricmc.loader.launch.knot.KnotClassDelegate")
public class KnotClassDelegateModification {

	@InjectAt(location = InjectLocation.HEAD)
	public byte[] getRawClassByteArray(String name, boolean skipOriginalLoader) throws IOException {
		InputStream cyanOverrideStream = CyanLoader.getFabricClassStream(name);
		if (cyanOverrideStream != null) {
			byte[] data = cyanOverrideStream.readAllBytes();
			cyanOverrideStream.close();
			return data;
		}
		
		return null;
	}
	
}
