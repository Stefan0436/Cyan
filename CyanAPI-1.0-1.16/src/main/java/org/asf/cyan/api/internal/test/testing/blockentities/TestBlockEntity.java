package org.asf.cyan.api.internal.test.testing.blockentities;

import org.asf.cyan.api.internal.test.sides.ServerEvents;

import net.minecraft.world.level.block.entity.BlockEntity;

public class TestBlockEntity extends BlockEntity {

	public TestBlockEntity() {
		super(ServerEvents.CUSTOM_BLOCK_ENTITY);
	}
	
}
