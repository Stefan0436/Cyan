package org.asf.cyan.cornflower.gradle.flowerinternal.implementation.cyan.game;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.asf.aos.util.service.extra.slib.util.ArrayUtil;
import org.asf.cyan.CyanLoader;
import org.asf.cyan.core.CyanCore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CornflowerLaunchWrapper {
	@SuppressWarnings("unchecked")
	public static void main(String[] arguments) throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		String mainClass = "org.asf.cyan.CyanIDEWrapper";
		Method mth = Class.forName("java.lang.System").getMethod("getProperty", String.class);
		Object conf = mth.invoke(null, "cornflowerLaunchWrapper");
		if (conf != null) {
			JsonObject config = JsonParser.parseString(conf.toString()).getAsJsonObject();

			if (config.has("mainClass")) {
				mainClass = config.get("mainClass").getAsString();
			}
			if (config.has("commands")) {
				config.get("commands").getAsJsonArray().forEach((ele) -> {
					ele.getAsJsonObject().entrySet().forEach((ent) -> {
						String name = ent.getKey();
						if (name.equals("loadclass")) {
							try {
								CyanCore.addAdditionalClass(Class.forName(ent.getValue().getAsString()));
							} catch (ClassNotFoundException e) {
								throw new RuntimeException(e);
							}
						} else if (name.equals("reference")) {
							try {
								CyanCore.addCoreUrl(new URL(ent.getValue().getAsString()));
							} catch (MalformedURLException e) {
								throw new RuntimeException(e);
							}
						} else if (name.equals("enabledevmode")) {
							CyanLoader.enableDeveloperMode();
						} else if (name.equals("package")) {
							CyanCore.addAllowedPackage(ent.getValue().getAsString());
						} else if (name.equals("addmods")) {
							ent.getValue().getAsJsonObject().entrySet().forEach((ent2) -> {
								try {
									Class<?> cls = CyanLoader.class;
									Field f = cls.getDeclaredField("classesMap");
									f.setAccessible(true);
									Map<String, String[]> classesMap = (Map<String, String[]>) f.get(null);
									classesMap.put(ent2.getKey(),
											ArrayUtil.append(classesMap.getOrDefault(ent.getKey(), new String[0]),
													new String[] { ent2.getValue().getAsString() }));
									f.set(null, classesMap);
								} catch (Exception e) {
									throw new RuntimeException(e);
								}
							});
						}
					});
				});
			}
		}

		Class.forName(mainClass).getMethod("main", String[].class).invoke(null, new Object[] { arguments });
	}
}
