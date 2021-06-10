package org.asf.cyan.modifications._1_17.server.paper;

import java.util.Optional;

import org.asf.cyan.fluid.api.transforming.Reflect;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;

@TargetClass(target = "net.minecraft.world.entity.EntityType")
public class EntityTypeMock<T> {
	
	@Reflect
	@TargetType(target = "java.util.Optional")
	public static Optional<EntityTypeMock<?>> byString(String location) {
		return null;
	}
	
}
