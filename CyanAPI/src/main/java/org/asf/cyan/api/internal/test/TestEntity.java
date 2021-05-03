package org.asf.cyan.api.internal.test;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class TestEntity extends Mob {

	private EntityType<?> entityType;
	private Level level;

	public TestEntity(EntityType<? extends TestEntity> type, Level level) {
		super(type, level);
		this.entityType = type;
		this.level = level;
	}

	public static Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0D).add(Attributes.MOVEMENT_SPEED, 0.25D);
	}

}
