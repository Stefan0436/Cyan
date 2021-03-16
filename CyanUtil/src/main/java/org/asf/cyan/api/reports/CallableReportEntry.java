package org.asf.cyan.api.reports;

import java.util.concurrent.Callable;

public class CallableReportEntry extends ReportEntry<Callable<?>> {

	public CallableReportEntry(Callable<?> value) {
		super(value);
	}
	
	public CallableReportEntry(String key, Callable<?> value) {
		super(key, value);
	}

}
