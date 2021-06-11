package org.asf.cyan.api.internal.modkit.transformers._1_17.client.entities;

import java.util.Map;
import java.util.function.Supplier;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import modkit.events.ingame.blocks.BlockEntityRendererRegistryEvent;
import modkit.events.objects.ingame.blocks.BlockEntityRendererRegistryEventObject;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.level.block.entity.BlockEntityType;

@FluidTransformer
@TargetClass(target = "net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher")
public class BlockEntityRenderDispatcherModification {

	private final Font font = null;
	public final TextureManager textureManager = null; // FIXME: not passed in 1.17
	private final Map<BlockEntityType<?>, BlockEntityRenderer<?>> renderers = null;
	public Options options = null; // FIXME: not passed in 1.17

	@Constructor
	@InjectAt(location = InjectLocation.TAIL)
	private void ctor(@TargetType(target = "net.minecraft.client.gui.Font") Font var1,
			@TargetType(target = "net.minecraft.client.model.geom.EntityModelSet") EntityModelSet var2,
			Supplier<BlockRenderDispatcher> var3) {
		BlockEntityRendererRegistryEventObject modEntities = new BlockEntityRendererRegistryEventObject(this,
				textureManager, font, options);
		BlockEntityRendererRegistryEvent.getInstance().dispatch(modEntities).getResult();
		for (BlockEntityType<?> type : modEntities.getEntities().keySet()) {
			BlockEntityRenderer<?> renderer = modEntities.getEntities().get(type);
			renderers.put(type, renderer);
		}
	}

}
