package org.asf.cyan.api.config;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

import org.asf.cyan.api.config.serializing.CCFGGetPropAction;

/**
 * 
 * Experimental CCFG Generator, <b>no support for value overwriting yet</b>
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 * @param <T> Configuration type
 */
public class CCFGConfigGenerator<T extends Configuration<T>> extends CCFGGetPropAction<Configuration<T>> {

	boolean addAll = false;
	String oldcontent = "";

	Configuration<T> conf = null;
	ArrayList<IOException> exceptions = new ArrayList<IOException>();
	ArrayList<String> disabled = new ArrayList<String>();

	public CCFGConfigGenerator(Configuration<T> conf, boolean addAll, String oldcontent) {
		this.conf = conf;
		this.addAll = addAll;
		this.oldcontent = oldcontent;
	}

	@Override
	public String processPrefix(String key) {
		if (key == null) {
			if (!addAll) {
				return null;
			}

			String pref = conf.getConfigHeader();
			if (!pref.isEmpty()) {
				pref = pref.replaceAll("\r", "");
				String newPref = "";
				for (String line : pref.split("\n")) {
					if (newPref.isEmpty())
						newPref = "# " + line;
					else
						newPref += System.lineSeparator() + "# " + line;
				}
				return newPref;
			} else
				return null;
		}
		try {
			String pref = conf.getKeyHeaderComment(conf.getClass().getField(key));
			if (!pref.isEmpty()) {
				pref = pref.replaceAll("\r", "");
				String newPref = "";
				for (String line : pref.split("\n")) {
					if (newPref.isEmpty())
						newPref = "# " + line;
					else
						newPref += System.lineSeparator() + "# " + line;
				}
				return newPref;
			} else
				return null;
		} catch (NoSuchFieldException | SecurityException e) {
			return null;
		}
	}

	@Override
	public String processSuffix(String key) {
		if (key == null) {
			if (!addAll) {
				return null;
			}

			String suff = conf.getConfigFooter();
			if (!suff.isEmpty()) {
				suff = suff.replaceAll("\r", "");
				String newSuff = "";
				for (String line : suff.split("\n")) {
					if (newSuff.isEmpty())
						newSuff = "# " + line;
					else
						newSuff += System.lineSeparator() + "# " + line;
				}
				return newSuff;
			} else
				return null;
		}
		try {
			String suff = conf.getKeyFooterComment(conf.getClass().getField(key));
			if (!suff.isEmpty()) {
				suff = suff.replaceAll("\r", "");
				String newSuff = "";
				for (String line : suff.split("\n")) {
					if (newSuff.isEmpty())
						newSuff = "# " + line;
					else
						newSuff += System.lineSeparator() + "# " + line;
				}
				return newSuff;
			} else
				return null;
		} catch (NoSuchFieldException | SecurityException e) {
			return null;
		}
	}

	@Override
	public Object getProp(String key) {
		try {
			Field f = conf.getClass().getField(key);
			f.setAccessible(true);
			if (!conf.isExcluded(f))
				return f.get(conf);
			else
				return null;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			return null;
		}
	}

	@Override
	public String[] keys() {
		if (!addAll) {
			ArrayList<String> keys = new ArrayList<String>();
			for (Field f : conf.getClass().getFields()) {
				f.setAccessible(true);
				try {
					if (f.get(conf) == null)
						continue;
				} catch (IllegalArgumentException | IllegalAccessException e) {
				}
				if (!conf.isExcluded(f) && conf.hasChanged(f, conf.getProp(f), true)) {
					keys.add(f.getName());
				} else if (!conf.isExcluded(f) && conf.isOptional(f) && !conf.isPresent(f)
						&& (conf.getKeyHeaderComment(f) != "" || conf.getKeyFooterComment(f) != "")) {
					disabled.add(f.getName());
					keys.add(f.getName());
				}
			}
			return keys.toArray(t -> new String[t]);
		} else {
			ArrayList<String> keys = new ArrayList<String>();
			for (Field f : conf.getClass().getFields()) {
				f.setAccessible(true);
				try {
					if (!conf.isExcluded(f) && f.get(conf) != null)
						keys.add(f.getName());
				} catch (IllegalArgumentException | IllegalAccessException e) {
				}
			}
			return keys.toArray(t -> new String[t]);
		}
	}

	@Override
	public void error(IOException exception) {
		exceptions.add(exception);
	}

	@Override
	public void onAdd(String key, String value) {
		conf.propertiesMemory.put(key, value);
	}

	@Override
	public String postProcess(String key, String entry) {
		if (disabled.contains(key)) {
			String newEntry = "";
			for (String line : entry.split(System.lineSeparator())) {
				if (newEntry.isEmpty())
					newEntry = "# " + line;
				else
					newEntry += System.lineSeparator() + "# " + line;
			}
			return newEntry;
		}
		return entry;
	}

}
