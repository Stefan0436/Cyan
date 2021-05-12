package org.asf.cyan.api.internal.test.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class TestBlock extends Block {

	public TestBlock(Properties var1) {
		super(var1);
	}

	@Override
	public InteractionResult use(BlockState var1, Level var2, BlockPos var3, Player var4, InteractionHand var5,
			BlockHitResult var6) {
		return InteractionResult.CONSUME;
	}

}
