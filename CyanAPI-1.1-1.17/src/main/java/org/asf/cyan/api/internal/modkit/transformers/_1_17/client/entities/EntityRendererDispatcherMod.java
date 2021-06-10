package org.asf.cyan.api.internal.modkit.transformers._1_17.client.entities;

import java.util.Map;
import java.util.function.Function;

import modkit.events.ingame.entities.EntityRendererRegistryEvent;
import modkit.events.objects.ingame.entities.EntityRendererContext;
import modkit.events.objects.ingame.entities.EntityRendererRegistryEventObject;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class EntityRendererDispatcherMod {

	@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
	public static void registerCyanModification(Font font, Map<EntityType<?>, EntityRendererProvider<?>> PROVIDERS,
			TextureManager textureManager, Options options, Object dispatcher) {
		EntityRendererRegistryEventObject modEntities = new EntityRendererRegistryEventObject(null, dispatcher,
				textureManager, font, options);
		EntityRendererRegistryEvent.getInstance().dispatch(modEntities).getResult();

		Map<EntityType<?>, Function<EntityRendererContext<?>, EntityRenderer<?>>> ctors = modEntities
				.getEntityConstructors();
		for (EntityType<?> type : ctors.keySet()) {
			Function<EntityRendererContext<?>, EntityRenderer<?>> renderer = ctors.get(type);
			PROVIDERS.put(type, (ctx) -> {
				EntityRendererContext<?> context = new EntityRendererContext(ctx.getResourceManager(), ctx.getFont(),
						textureManager, modEntities.getDispatcher(), options, type,
						new Object[] { ctx, ctx.getItemRenderer(), ctx.getModelSet() });
				return (EntityRenderer<Entity>) renderer.apply(context);
			});
		}
		Map<EntityType<?>, EntityRenderer<?>> renderers = modEntities.getEntities(false);
		for (EntityType<?> type : renderers.keySet()) {
			EntityRenderer<?> renderer = renderers.get(type);
			PROVIDERS.put(type, (ctx) -> {
				return (EntityRenderer<Entity>) renderer;
			});
		}
	}

}
