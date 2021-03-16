package org.asf.cyan.modifications._1_15_2.typereplacers;

import java.io.File;

import org.asf.cyan.fluid.api.transforming.TargetClass;

@TargetClass(target = "net.minecraft.CrashReport")
public abstract class CrashReportMock {
	public abstract String getFriendlyReport();
	public abstract File getSaveFile();
	public abstract boolean saveToFile(File output);
	public abstract Throwable getException();
}
