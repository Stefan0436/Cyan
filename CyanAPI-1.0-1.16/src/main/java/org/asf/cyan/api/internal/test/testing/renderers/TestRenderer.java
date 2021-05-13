package org.asf.cyan.api.internal.test.testing.renderers;

import org.asf.cyan.api.internal.test.testing.TestEntity;
import org.asf.cyan.api.internal.test.testing.models.TestModel;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class TestRenderer extends MobRenderer<TestEntity, TestModel> {

	public TestRenderer(EntityRenderDispatcher dispatcher) {
		super(dispatcher, new TestModel(), 0.3F);
	}

	public ResourceLocation getTextureLocation(TestEntity entity) {
		return new ResourceLocation("cyan", "textures/entities/test.png");
	}

}