package org.asf.cyan.api.internal.modkit.transformers._1_17.client.entities;

import java.util.Map;
import java.util.function.Function;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import modkit.events.ingame.rendering.blockentity.BlockEntityRendererRegistryEvent;
import modkit.events.objects.ingame.rendering.blockentity.BlockEntityRendererRegistryEventObject;
import modkit.events.objects.ingame.rendering.context.BlockEntityRendererContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

@FluidTransformer
@TargetClass(target = "net.minecraft.client.renderer.blockentity.BlockEntityRenderers")
public class BlockEntityRenderersModification {

	private static final Map<BlockEntityType<?>, BlockEntityRendererProvider<?>> PROVIDERS = null;

	@Constructor(clinit = true)
	@InjectAt(location = InjectLocation.TAIL)
	@SuppressWarnings({ "unchecked", "rawtypes", "resource" })
	private static void registerRenderers() {
		BlockEntityRendererRegistryEventObject modEntities = new BlockEntityRendererRegistryEventObject();
		BlockEntityRendererRegistryEvent.getInstance().dispatch(modEntities).getResult();

		Map<BlockEntityType<?>, Function<BlockEntityRendererContext<?>, BlockEntityRenderer<?>>> ctors = modEntities
				.getEntityConstructors();
		for (BlockEntityType<?> type : ctors.keySet()) {
			Function<BlockEntityRendererContext<?>, BlockEntityRenderer<?>> renderer = ctors.get(type);
			PROVIDERS.put(type, (ctx) -> {
				BlockEntityRendererContext<?> context = new BlockEntityRendererContext(ctx.getFont(),
						Minecraft.getInstance().getTextureManager(), ctx.getBlockEntityRenderDispatcher(),
						Minecraft.getInstance().options, type, new Object[] { ctx, ctx.getBlockRenderDispatcher(),
								ctx.getModelSet(), ctx.getBlockEntityRenderDispatcher().level });
				return (BlockEntityRenderer<BlockEntity>) renderer.apply(context);
			});
		}
	}

}
