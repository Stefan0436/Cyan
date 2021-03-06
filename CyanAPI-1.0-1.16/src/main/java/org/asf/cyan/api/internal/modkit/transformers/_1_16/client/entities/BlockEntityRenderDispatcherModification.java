package org.asf.cyan.api.internal.modkit.transformers._1_16.client.entities;

import java.util.Map;

import org.asf.cyan.api.events.ingame.blocks.BlockEntityRendererRegistryEvent;
import org.asf.cyan.api.events.objects.ingame.blocks.BlockEntityRendererRegistryEventObject;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.level.block.entity.BlockEntityType;

@FluidTransformer
@TargetClass(target = "net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher")
public class BlockEntityRenderDispatcherModification {

	private final Font font = null;
	public final TextureManager textureManager = null;
	private final Map<BlockEntityType<?>, BlockEntityRenderer<?>> renderers = null;
	public Options options = null;

	@Constructor
	@InjectAt(location = InjectLocation.TAIL)
	private void ctor() {
		BlockEntityRendererRegistryEventObject modEntities = new BlockEntityRendererRegistryEventObject(this,
				textureManager, font, options);
		BlockEntityRendererRegistryEvent.getInstance().dispatch(modEntities).getResult();
		for (BlockEntityType<?> type : modEntities.getEntities().keySet()) {
			BlockEntityRenderer<?> renderer = modEntities.getEntities().get(type);
			renderers.put(type, renderer);
		}
	}

}
