package org.asf.cyan.api.reports;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * 
 * Report node for {@link ReportCategory ReportCategory}.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ReportNode {

	public String name;
	public ReportEntryList entries;

	public ReportNode(String name) {
		this.name = name;
		this.entries = new ReportEntryList();
	}

	public ReportNode(String name, ReportEntryList entries) {
		this.name = name;
		this.entries = entries;
	}

	public ReportNode addAll(Collection<ReportEntry<?>> c) {
		entries.addAll(c);
		return this;
	}

	public ReportNode addAll(Map<String, ?> c) {
		c.forEach((k, v) -> {
			add(k, v);
		});
		return this;
	}

	public ReportNode addAll(int index, Collection<ReportEntry<?>> c) {
		entries.addAll(index, c);
		return this;
	}

	public ReportNode addAll(Object... reportMap) {
		if (reportMap.length % 2 == 0) {
			for (int i = 0; i < reportMap.length; i++) {
				if (!(reportMap[i] instanceof String)) {
					throw new IllegalArgumentException("Invalid argument type, expected a " + String.class.getTypeName()
							+ ", got " + reportMap[i].getClass().getTypeName());
				}
				String key = reportMap[i].toString();
				Object value = reportMap[i + 1];
				add(key, value);
				i++;
			}
		} else {
			throw new IndexOutOfBoundsException("Not a valid value map.");
		}
		return this;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ReportNode addAll(int index, Map<String, ?> c) {
		ArrayList<ReportEntry<?>> items = new ArrayList<ReportEntry<?>>();
		c.forEach((k, v) -> {
			items.add(new ReportEntry(k, v));
		});
		addAll(index, items);
		return this;
	}

	public ReportNode add(int index, ReportEntry<?> entry) {
		entries.add(index, entry);
		return this;
	}

	public ReportNode add(ReportEntry<?> entry) {
		entries.add(entry);
		return this;
	}

	public <T> ReportNode add(T value) {
		entries.add(null, value);
		return this;
	}

	public <T> ReportNode add(int index, T value) {
		entries.add(index, null, value);
		return this;
	}

	public <T> ReportNode add(int index, String key, T value) {
		entries.add(index, key, value);
		return this;
	}

	public <T> ReportNode add(String key, T value) {
		entries.add(key, value);
		return this;
	}

	public ReportNode add(int index, CallableReportEntry entry) {
		entries.add(index, entry);
		return this;
	}

	public ReportNode add(CallableReportEntry entry) {
		entries.add(entry);
		return this;
	}

	public <T> ReportNode add(int index, String key, Callable<?> value) {
		entries.add(index, key, value);
		return this;
	}

	public <T> ReportNode add(String key, Callable<?> value) {
		entries.add(key, value);
		return this;
	}

	public <T> ReportNode add(int index, Callable<?> value) {
		entries.add(index, value);
		return this;
	}

	public <T> ReportNode add(Callable<?> value) {
		entries.add(value);
		return this;
	}

}
