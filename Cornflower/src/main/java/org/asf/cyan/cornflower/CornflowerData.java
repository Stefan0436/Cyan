package org.asf.cyan.cornflower;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Cornflower information class, primarily for the gradle plugin.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class CornflowerData {

	static final JsonObject JSON = JsonParser.parseString(readJSON()).getAsJsonObject();

	static final String readJSON() {
		try {
			InputStreamReader r = new InputStreamReader(CornflowerData.class.getClassLoader().getResource("info.json").openStream());
			BufferedReader reader = new BufferedReader(r);
			StringBuffer buffer = new StringBuffer();
			String content = "";

			while ((content = reader.readLine()) != null) {
				buffer.append(content);
			}

			reader.close();
			r.close();
			content = buffer.toString();

			return content;
		} catch (IOException e) {
			return "{}";
		}
	}

	public static final String ID = JSON.get("id").getAsString();
	public static final String GROUP = JSON.get("group").getAsString();
	public static final String DISPLAY = JSON.get("display").getAsString();
	public static final String VERSION = JSON.get("version").getAsString();
}
