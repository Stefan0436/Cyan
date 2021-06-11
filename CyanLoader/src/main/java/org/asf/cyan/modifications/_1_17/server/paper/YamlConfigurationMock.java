package org.asf.cyan.modifications._1_17.server.paper;

import org.asf.cyan.fluid.api.transforming.Reflect;
import org.asf.cyan.fluid.api.transforming.TargetClass;

@TargetClass(target = "org.bukkit.configuration.file.YamlConfiguration")
public class YamlConfigurationMock {

	@Reflect
	public int getInt(String key, int def) {
		return 0;
	}

}
