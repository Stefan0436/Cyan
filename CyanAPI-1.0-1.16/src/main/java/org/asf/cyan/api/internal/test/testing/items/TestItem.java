package org.asf.cyan.api.internal.test.testing.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TestItem extends Item {

	public TestItem(Properties props) {
		super(props);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (player.getServer() != null)
			player.setLevel(player.getServer().getLevel(Level.END));
		return InteractionResultHolder.success(new ItemStack(this, player.getItemInHand(hand).getCount() - 1));
	}

}
