package org.asf.cyan.modifications._1_15_2.typereplacers;

import org.asf.cyan.fluid.api.transforming.Reflect;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;

@TargetClass(target = "net.minecraft.CrashReportCategory")
public class CrashReportCategoryMock {
	@Reflect
	@TargetType(target = "net.minecraft.CrashReportCategory")
	public void setDetail(String msg, Object value) {
	}

	public CrashReportCategoryMock(@TargetType(target = "net.minecraft.CrashReport") Object report, String name) {
	}

	@Reflect
	public void getDetails(StringBuilder var1) {
	}
}
