package org.asf.cyan.api.config.serializing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * CCFG Configuration 'put-element' action
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class CCFGPutPropAction {
	/**
	 * The action run method, adds the element to the output map/config/etc
	 * 
	 * @param key Element key
	 * @param txt Serialized value
	 */
	public abstract void run(String key, String txt);

	/**
	 * Check if the entry is valid, optional override
	 * 
	 * @param line Input CCFG line
	 * @return True if the line is valid, false otherwise
	 */
	public boolean isValidEntry(String line) {
		return line.matches("^[A-Za-z_$][A-Za-z0-9]*> (.*|\\R*)$");
	}

	/**
	 * Post-process an entry, optional override
	 * 
	 * @param line Input line
	 * @return Object collection with the following values: <b>0 = key, 1 = line out,
	 *         2(int) = brquote, 3(boolean) = wasBrQuote, 4(boolean) = indent,
	 *         5(boolean) = skip_current</b>
	 */
	public Object[] processEntry(String line) {
		Object[] data = new Object[6]; // 0 = key, 1 = line out, 2(int) = brquote, 3(boolean) = wasBrQuote, 4(boolean)
										// = indent, 5(boolean) = skip_current

		Matcher m = Pattern.compile("^([A-Za-z_$][A-Za-z0-9]*)> (.*|\\R*)$").matcher(line);
		m.matches();
		data[0] = m.group(1);
		String dat = m.group(2);
		boolean isCategory = dat.startsWith("{");

		if (isCategory) {
			data[2] = 1;
			data[3] = true;
			data[4] = true;
			data[5] = true;
		} else {
			line = line.substring(data[0].toString().length() + 2);
		}
		data[1] = line;

		return data;
	}
}
