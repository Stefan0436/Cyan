package org.asf.cyan.api.internal.modkit.transformers._1_17.client.entities;

import java.util.Map;
import java.util.function.Function;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import modkit.events.ingame.rendering.entity.EntityRendererRegistryEvent;
import modkit.events.objects.ingame.rendering.context.EntityRendererContext;
import modkit.events.objects.ingame.rendering.entity.EntityRendererRegistryEventObject;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

@FluidTransformer
@TargetClass(target = "net.minecraft.client.renderer.entity.EntityRenderers")
public class EntityRenderersModification {

	private static final Map<EntityType<?>, EntityRendererProvider<?>> PROVIDERS = null;

	@Constructor(clinit = true)
	@InjectAt(location = InjectLocation.TAIL)
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void registerRenderers() {
		EntityRendererRegistryEventObject modEntities = new EntityRendererRegistryEventObject();
		EntityRendererRegistryEvent.getInstance().dispatch(modEntities).getResult();

		Map<EntityType<?>, Function<EntityRendererContext<?>, EntityRenderer<?>>> ctors = modEntities
				.getEntityConstructors();
		for (EntityType<?> type : ctors.keySet()) {
			Function<EntityRendererContext<?>, EntityRenderer<?>> renderer = ctors.get(type);
			PROVIDERS.put(type, (ctx) -> {
				EntityRendererContext<?> context = new EntityRendererContext(ctx.getResourceManager(), ctx.getFont(),
						ctx.getEntityRenderDispatcher().textureManager, ctx.getEntityRenderDispatcher(),
						ctx.getEntityRenderDispatcher().options, type,
						new Object[] { ctx, ctx.getItemRenderer(), ctx.getModelSet() });
				return (EntityRenderer<Entity>) renderer.apply(context);
			});
		}
	}

}
