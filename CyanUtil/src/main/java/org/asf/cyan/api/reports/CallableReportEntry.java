package org.asf.cyan.api.reports;

import java.util.function.Supplier;

public class CallableReportEntry extends ReportEntry<Supplier<?>> {

	public CallableReportEntry(Supplier<?> value) {
		super(value);
	}
	
	public CallableReportEntry(String key, Supplier<?> value) {
		super(key, value);
	}

}
