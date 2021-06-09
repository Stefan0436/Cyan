package org.asf.cyan.modifications._1_17.common;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.Reflect;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;
import org.asf.cyan.fluid.api.transforming.information.metadata.TransformerMetadata;

/**
 * 
 * Modifies the crash report so that Cyan information is displayed.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@FluidTransformer
@TargetClass(target = "net.minecraft.SystemReport")
public class SystemReportModification {

	@Reflect
	public void setDetail(String msg, String value) {
	}

	@Constructor
	@InjectAt(location = InjectLocation.HEAD, targetCall = "setDetail(java.lang.String, java.lang.String)", offset = 2)
	private void initDetails() {
		String modloaders = CyanLoader.getModloader(CyanLoader.class).getLoaders();
		String loaderversions = CyanLoader.getModloader(CyanLoader.class).getLoaderVersions();

		setDetail("Running Modloader(s)", modloaders);
		setDetail("Modloader Version(s)", loaderversions);
		setDetail("Modloader Phase", Modloader.getModloader().getPhase().toString());
		CyanLoader.getModloader(CyanLoader.class).addLoadedModInfo((t1, t2) -> setDetail(t1, t2.toString()));
		setDetail("Applied transformers", Integer.toString(TransformerMetadata.getLoadedTransformers().length));
	}

}
