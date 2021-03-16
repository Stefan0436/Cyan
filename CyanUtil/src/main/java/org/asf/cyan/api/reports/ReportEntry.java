package org.asf.cyan.api.reports;

/**
 * 
 * Report entry for {@link ReportNode ReportNode} and {@link ReportEntryList ReportEntryList}.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ReportEntry<T> {
	public ReportEntry(T value) {
		this.key = null;
		this.value = value;
	}
	
	public ReportEntry(String key, T value) {
		this.key = key;
		this.value = value;
	}
	
	public static <T> ReportEntry<T> create(String key, T value) {
		return new ReportEntry<T>(key, value);
	}

	public static <T> ReportEntry<T> create(T value) {
		return new ReportEntry<T>(value);
	}
	
	public String key;
	public T value;
}
