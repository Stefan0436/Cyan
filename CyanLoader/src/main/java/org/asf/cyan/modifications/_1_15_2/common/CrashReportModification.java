package org.asf.cyan.modifications._1_15_2.common;

import java.io.File;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetName;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;
import org.asf.cyan.fluid.api.transforming.information.metadata.TransformerMetadata;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.modifications._1_15_2.typereplacers.CrashReportCategoryMock;

/**
 * 
 * Modifies the crash report so that Cyan information is displayed.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@FluidTransformer
@TargetClass(target = "net.minecraft.CrashReport")
public class CrashReportModification {

	@TargetType(target = "net.minecraft.CrashReportCategory")
	private final CrashReportCategoryMock systemDetails = null;

	private boolean areCyanTransformersPresent = false;
	private StackTraceElement[] uncategorizedStackTrace = new StackTraceElement[0];

	private final Throwable exception = null;

	@TargetName(target = "getDetails")
	@InjectAt(location = InjectLocation.TAIL, targetCall = "append(java.lang.String)", targetOwner = "java.lang.StringBuilder", offset = 1)
	public void getDetails1(StringBuilder var1) {
		if (uncategorizedStackTrace != null && uncategorizedStackTrace.length != 0) {
			areCyanTransformersPresent = false;

			TransformerMetadata.createStackTrace(uncategorizedStackTrace, t -> {

				if (!areCyanTransformersPresent) {
					var1.append("Transformers:").append("\n");
				}

				var1.append("\t").append(t).append("\n");
				areCyanTransformersPresent = true;

			}, t -> {

				if (!areCyanTransformersPresent) {
					var1.append("Transformers:").append("\n");
				}

				var1.append("\t- ").append(t).append("\n");
				areCyanTransformersPresent = true;

			});

			if (areCyanTransformersPresent) {
				File output = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "transformer-backtrace");
				try {
					TransformerMetadata.dumpErrorBacktrace(
							exception.getClass().getTypeName() + ": " + exception.getMessage(), uncategorizedStackTrace,
							output);

					var1.append("Additional information:\n");
					var1.append("\tTransformer metadata dumped in " + output.getCanonicalPath());
					var1.append("\n");
					var1.append("\tChanged class files dumped in "
							+ new File(MinecraftInstallationToolkit.getMinecraftDirectory(),
									"transformer-backtrace/classes").getCanonicalPath());
					var1.append("\n");
					var1.append("\tThe dump will be deleted on next "
							+ Modloader.getModloader().getSimpleName().toUpperCase() + " startup.");
				} catch (Exception e) {
					var1.append("Failure:\n\tCould not dump FLUID transformer metadata, an exception was thrown: "
							+ e.getClass().getTypeName() + ": " + e.getMessage()).append("\n");
					for (StackTraceElement ele : e.getStackTrace()) {
						var1.append("\t- ").append("at ").append(ele).append("\n");
					}
				}
				var1.append("\n");
			}
		}
	}

//	// Disabled because it cannot fully deobfuscate the stack trace
//	@TargetName(target = "getDetails")
//	@InjectAt(location = InjectLocation.HEAD)
//	public void getDetails2(StringBuilder var1) {
//		if (uncategorizedStackTrace != null && uncategorizedStackTrace.length != 0) {
//			StackTraceElement[] newStackTrace = new StackTraceElement[uncategorizedStackTrace.length];
//			int index = 0;
//			for (StackTraceElement element : uncategorizedStackTrace) {
//				newStackTrace[index++] = new StackTraceElement(null, element.getModuleName(),
//						element.getModuleVersion(), Transformer.getDeobfName(element.getClassName()),
//						element.getMethodName(), element.getFileName(), element.getLineNumber());
//			}
//			uncategorizedStackTrace = newStackTrace;
//		}
//	}

	// Temporary system to remove the class loader from the crash reports
	@TargetName(target = "getDetails")
	@InjectAt(location = InjectLocation.HEAD)
	public void getDetails2(StringBuilder var1) {
		if (uncategorizedStackTrace != null && uncategorizedStackTrace.length != 0) {
			StackTraceElement[] newStackTrace = new StackTraceElement[uncategorizedStackTrace.length];
			int index = 0;
			for (StackTraceElement element : uncategorizedStackTrace) {
				newStackTrace[index++] = new StackTraceElement(null, element.getModuleName(),
						element.getModuleVersion(), element.getClassName(), element.getMethodName(),
						element.getFileName(), element.getLineNumber());
			}
			uncategorizedStackTrace = newStackTrace;
		}
	}

	@InjectAt(location = InjectLocation.HEAD, targetCall = "setDetail(java.lang.String, net.minecraft.CrashReportDetail)", targetOwner = "net.minecraft.CrashReportCategory", offset = 2)
	private void initDetails() {
		String modloaders = "";
		String loaderversions = "";

		for (Modloader modloader : Modloader.getAllModloaders()) {
			if (!modloaders.isEmpty()) {
				modloaders += ", ";
				loaderversions += ", ";
			}

			modloaders += modloader.getSimpleName();
			loaderversions += modloader.getName() + "; "
					+ (modloader.getVersion() == null ? "Generic" : modloader.getVersion());
		}

		systemDetails.setDetail("Running Modloader(s)", modloaders);
		systemDetails.setDetail("Modloader Version(s)", loaderversions);
		systemDetails.setDetail("Modloader Phase", Modloader.getModloader().getPhase());
		systemDetails.setDetail("Loaded " + Modloader.getModloader().getSimpleName().toUpperCase() + " Mods",
				Modloader.getModloader().getLoadedMods().length);
		systemDetails.setDetail("Loaded " + Modloader.getModloader().getSimpleName().toUpperCase() + " Coremods",
				Modloader.getModloader().getLoadedCoremods().length);
		systemDetails.setDetail("Applied transformers", TransformerMetadata.getLoadedTransformers().length);

		// TODO: add cyan category to tail
	}
}
