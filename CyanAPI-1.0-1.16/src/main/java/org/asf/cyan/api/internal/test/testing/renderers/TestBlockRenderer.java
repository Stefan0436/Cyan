package org.asf.cyan.api.internal.test.testing.renderers;

import org.asf.cyan.api.internal.test.testing.blockentities.TestBlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;

public class TestBlockRenderer extends BlockEntityRenderer<TestBlockEntity> {
	private final ModelPart bb_main;

	public TestBlockRenderer(BlockEntityRenderDispatcher var1) {
		super(var1);
		bb_main = new ModelPart(0, 0, 0, 0);
		bb_main.addBox(-16.0F, -16.0F, 0.0F, 16.0F, 16.0F, 16.0F, 0.0F, false);
	}

	@Override
	public void render(TestBlockEntity arg0, float arg1, PoseStack arg2, MultiBufferSource arg3, int arg4, int arg5) {
		bb_main.render(arg2, arg3.getBuffer(RenderType.solid()), 0, 0);
	}

}
