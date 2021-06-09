package org.asf.cyan.modifications._1_17.common;

import java.io.File;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetName;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;
import org.asf.cyan.fluid.api.transforming.information.metadata.TransformerMetadata;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;

/**
 * 
 * Modifies the crash report so that Cyan information is displayed.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@FluidTransformer
@TargetClass(target = "net.minecraft.CrashReportCategory")
public class CrashReportCategoryModification {

	private boolean areCyanTransformersPresent = false;
	private StackTraceElement[] stackTrace = new StackTraceElement[0];

	@TargetName(target = "getDetails")
	@InjectAt(location = InjectLocation.TAIL)
	public void getDetails1(StringBuilder var1) {
		if (stackTrace != null && stackTrace.length != 0) {
			areCyanTransformersPresent = false;

			TransformerMetadata.createStackTrace(stackTrace, t -> {

				if (!areCyanTransformersPresent) {
					var1.append("\n").append("Transformers:").append("\n");
				}

				var1.append("\t").append(t).append("\n");
				areCyanTransformersPresent = true;

			}, t -> {

				if (!areCyanTransformersPresent) {
					var1.append("\n").append("Transformers:").append("\n");
				}

				var1.append("\t- ").append(t).append("\n");
				areCyanTransformersPresent = true;

			});

			if (areCyanTransformersPresent) {
				File output = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "transformer-backtrace");
				try {
					TransformerMetadata.dumpErrorBacktrace("Unknown, should have been saved already.", stackTrace,
							output);

					var1.append("Additional information:\n");
					var1.append("\tTransformer metadata dumped in " + output.getCanonicalPath());
					var1.append("\n");
					var1.append("\tChanged class files dumped in "
							+ new File(MinecraftInstallationToolkit.getMinecraftDirectory(),
									"transformer-backtrace/classes").getCanonicalPath());
					var1.append("\n");
					var1.append("\tThe dump will be deleted on next " + Modloader.getModloader().getSimpleName().toUpperCase()
							+ " startup.");
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
//	@InjectAt(location = InjectLocation.HEAD)
//	@TargetName(target = "getDetails")
//	public void getDetails2(StringBuilder var1) {
//		if (stackTrace != null && stackTrace.length != 0) {
//			StackTraceElement[] newStackTrace = new StackTraceElement[stackTrace.length];
//			int index = 0;
//			for (StackTraceElement element : stackTrace) {
//				newStackTrace[index++] = new StackTraceElement(null, element.getModuleName(),
//						element.getModuleVersion(), Transformer.getDeobfName(element.getClassName()),
//						element.getMethodName(), element.getFileName(), element.getLineNumber());
//			}
//			stackTrace = newStackTrace;
//		}
//	}

	// Temporary system to remove the class loader from the crash report
	@InjectAt(location = InjectLocation.HEAD)
	@TargetName(target = "getDetails")
	public void getDetails2(StringBuilder var1) {
		if (stackTrace != null && stackTrace.length != 0) {
			StackTraceElement[] newStackTrace = new StackTraceElement[stackTrace.length];
			int index = 0;
			for (StackTraceElement element : stackTrace) {
				newStackTrace[index++] = new StackTraceElement(null, element.getModuleName(),
						element.getModuleVersion(), element.getClassName(), element.getMethodName(),
						element.getFileName(), element.getLineNumber());
			}
			stackTrace = newStackTrace;
		}
	}
}
