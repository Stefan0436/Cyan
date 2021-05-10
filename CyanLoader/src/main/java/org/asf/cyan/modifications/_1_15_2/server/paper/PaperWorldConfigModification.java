package org.asf.cyan.modifications._1_15_2.server.paper;

import java.util.Map;

import org.asf.cyan.api.fluid.annotations.PlatformOnly;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Erase;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;

@FluidTransformer
@PlatformOnly(LaunchPlatform.SPIGOT)
@TargetClass(target = "com.destroystokyo.paper.PaperWorldConfig")
public abstract class PaperWorldConfigModification {

	private final String worldName = null;

	@TargetType(target = "org.bukkit.configuration.file.YamlConfiguration")
	private YamlConfigurationMock config;

	public Map<Object, Integer> entityPerChunkSaveLimits = null;

	@Erase
	public void lambda$entityPerChunkSaveLimits$2(
			@TargetType(target = "net.minecraft.resources.ResourceLocation") ResourceLocationMock entity) {
		EntityTypeMock<?> type = (EntityTypeMock<?>) EntityTypeMock.byString(entity.toString())
				.orElseThrow(new CyanErrorSupplier(entity.toString()));

		String path = ".entity-per-chunk-save-limit.";
		if (!entity.getNamespace().equals("minecraft")) {
			path += entity.getNamespace() + "-";
		}
		path += entity.getPath();

		int value = config.getInt("world-settings." + worldName + path,
				config.getInt("world-settings.default" + path, -1));

		if (value != -1) {
			entityPerChunkSaveLimits.put(type, value);
		}
	}
}
