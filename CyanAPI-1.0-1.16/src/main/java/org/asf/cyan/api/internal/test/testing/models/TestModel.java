package org.asf.cyan.api.internal.test.testing.models;

import org.asf.cyan.api.internal.test.testing.TestEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

public class TestModel extends EntityModel<TestEntity> {
	private final ModelPart bb_main;

	public TestModel() {
		texWidth = 50;
		texHeight = 50;
		bb_main = new ModelPart(this);
		bb_main.setPos(0.0F, 24.0F, 0.0F);
		bb_main.texOffs(0, 0).addBox(-6.0F, -6.0F, 0.0F, 6.0F, 6.0F, 6.0F, 0.0F, false);
	}

	@Override
	public void setupAnim(TestEntity entity, float limbSwing, float limbSwingAmount, float age, float netHeadYaw,
			float headPitch) {
	}

	@Override
	public void renderToBuffer(PoseStack pos, VertexConsumer buffer, int packedLight, int packedOverlay, float red,
			float green, float blue, float alpha) {
		bb_main.render(pos, buffer, packedLight, packedOverlay);
	}

}