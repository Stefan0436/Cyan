package org.asf.cyan.modifications._1_15_2.typereplacers;

import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;

@TargetClass(target = "net.minecraft.CrashReportCategory")
public abstract class CrashReportCategoryMock {
	@TargetType(target = "net.minecraft.CrashReportCategory")
	public abstract void setDetail(String msg, Object value);
}
