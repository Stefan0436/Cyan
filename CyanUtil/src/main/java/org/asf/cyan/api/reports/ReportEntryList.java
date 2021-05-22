package org.asf.cyan.api.reports;

import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * 
 * Report entry list.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ReportEntryList extends ArrayList<ReportEntry<?>> {
	private static final long serialVersionUID = -526262117774967321L;

	public <T> ReportEntry<T> get(String key) {
		return get(key, (T) null);
	}

	@SuppressWarnings("unchecked")
	public <T> ReportEntry<T> get(String key, T def) {
		for (ReportEntry<?> ent : this) {
			if (ent.key.equals(key) && def.getClass().isAssignableFrom(ent.key.getClass()))
				return (ReportEntry<T>) ent;
		}
		return ReportEntry.create(key, def);
	}

	@SuppressWarnings("unchecked")
	public <T> ReportEntry<T> get(String key, ReportEntry<T> def) {
		for (ReportEntry<?> ent : this) {
			if (ent.key.equals(key) && def.getClass().isAssignableFrom(ent.getClass()))
				return (ReportEntry<T>) ent;
		}
		return def;
	}

	public <T> void add(String key, T value) {
		add(ReportEntry.create(key, value));
	}

	public <T> void add(int index, String key, T value) {
		add(index, ReportEntry.create(key, value));
	}

	public void add(int index, CallableReportEntry entry) {
		add(index, (ReportEntry<?>)entry);
	}

	public void add(CallableReportEntry entry) {
		add((ReportEntry<?>)entry);
	}

	public <T> void add(int index, String key, Supplier<?> value) {
		add(index, key, (Object)value);
	}

	public <T> void add(String key, Supplier<?> value) {
		add(key, (Object)value);
	}

	public <T> void add(int index, Supplier<?> value) {
		add(index, null, (Object)value);
	}

	public <T> void add(Supplier<?> value) {
		add(null, value);
	}

}
