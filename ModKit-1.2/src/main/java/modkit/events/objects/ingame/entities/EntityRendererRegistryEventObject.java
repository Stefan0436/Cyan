package modkit.events.objects.ingame.rendering.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.asf.cyan.api.events.extended.EventObject;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.versioning.Version;

import modkit.events.objects.ingame.rendering.context.EntityRendererContext;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * 
 * Entity Renderer Registry Event Object -- Register your entity renderers by
 * using this
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class EntityRendererRegistryEventObject extends EventObject {

	private Font font;
	private ReloadableResourceManager resourceManager;
	private TextureManager textureManager;
	private EntityRenderDispatcher dispatcher;
	private Options options;

	@SuppressWarnings("rawtypes")
	private HashMap<EntityType<?>, Function> entities = new HashMap<EntityType<?>, Function>();
	private HashMap<EntityType<?>, EntityRenderer<?>> rendererInstances = new HashMap<EntityType<?>, EntityRenderer<?>>();

	public EntityRendererRegistryEventObject() {

	}

	/**
	 * @deprecated Does not work in 1.17+, see addEntity.
	 */
	@Deprecated
	public EntityRendererRegistryEventObject(ReloadableResourceManager resourceManager, Object dispatcher,
			TextureManager textureManager, Font font, Options options) {
		this.resourceManager = resourceManager;
		this.dispatcher = (EntityRenderDispatcher) dispatcher;
		this.textureManager = textureManager;
		this.options = options;
		this.font = font;
	}

	/**
	 * Retrieves the resource manager
	 * 
	 * @deprecated Does not work in 1.17+, see addEntity.
	 */
	@Deprecated
	public ReloadableResourceManager getResourceManager() {
		if (Version.fromString(Modloader.getModloaderGameVersion()).isGreaterOrEqualTo(Version.fromString("1.17")))
			throw new RuntimeException("EntityRendererRegistryEventObject.getResourceManager() does not work in 1.17+");
		warn("DEPRECATION NOTICE: A mod called EntityRendererRegistryEventObject.getResourceManager(), it has been deprecated since Minecraft 1.17 was released, it should not be used.");

		return resourceManager;
	}

	/**
	 * Retrieves the entity dispatcher
	 * 
	 * @deprecated Does not work in 1.17+, see addEntity.
	 */
	@Deprecated
	public EntityRenderDispatcher getDispatcher() {
		if (Version.fromString(Modloader.getModloaderGameVersion()).isGreaterOrEqualTo(Version.fromString("1.17")))
			throw new RuntimeException("EntityRendererRegistryEventObject.getDispatcher() does not work in 1.17+");
		warn("DEPRECATION NOTICE: A mod called EntityRendererRegistryEventObject.getDispatcher(), it has been deprecated since Minecraft 1.17 was released, it should not be used.");

		return dispatcher;
	}

	/**
	 * Retrieves the texture manager
	 * 
	 * @deprecated Does not work in 1.17+, see addEntity.
	 */
	@Deprecated
	public TextureManager getTextureManager() {
		if (Version.fromString(Modloader.getModloaderGameVersion()).isGreaterOrEqualTo(Version.fromString("1.17")))
			throw new RuntimeException("EntityRendererRegistryEventObject.getTextureManager() does not work in 1.17+");
		warn("DEPRECATION NOTICE: A mod called EntityRendererRegistryEventObject.getTextureManager(), it has been deprecated since Minecraft 1.17 was released, it should not be used.");

		return textureManager;
	}

	/**
	 * Adds custom entity renderers to the game
	 * 
	 * @param <T>        Entity Class Type
	 * @param entityType Entity type
	 * @param renderer   Entity renderer
	 * @deprecated Please use <code>addEntity(type, constructor)</code>
	 */
	@Deprecated
	public <T extends Entity> void addEntity(EntityType<? extends T> entityType, EntityRenderer<? extends T> renderer) {
		if (Version.fromString(Modloader.getModloaderGameVersion()).isGreaterOrEqualTo(Version.fromString("1.17")))
			throw new RuntimeException(
					"EntityRendererRegistryEventObject.addEntity(type, renderer) does not work in 1.17+");
		warn("DEPRECATION NOTICE: A mod called EntityRendererRegistryEventObject.addEntity(type, renderer), it has been deprecated since Minecraft 1.17 was released, it should not be used.");

		this.<T>addEntity(entityType, (ctx) -> {
			return renderer;
		});
	}

	/**
	 * Adds custom entity renderers to the game
	 * 
	 * @param <T>         Entity Class Type
	 * @param entityType  Entity type
	 * @param constructor Entity constructor function, receives the context,
	 *                    supplies the new renderer instance.
	 * @since ModKit 1.1
	 */
	public <T extends Entity> void addEntity(EntityType<? extends T> entityType,
			Function<EntityRendererContext<T>, EntityRenderer<? extends T>> constructor) {
		this.entities.put(entityType, constructor);
	}

	/**
	 * Retrieves the client options
	 * 
	 * @deprecated Does not work in 1.17+, see addEntity.
	 */
	@Deprecated
	public Options getClientOptions() {
		if (Version.fromString(Modloader.getModloaderGameVersion()).isGreaterOrEqualTo(Version.fromString("1.17")))
			throw new RuntimeException("EntityRendererRegistryEventObject.getClientOptions() does not work in 1.17+");
		warn("DEPRECATION NOTICE: A mod called EntityRendererRegistryEventObject.getClientOptions(), it has been deprecated since Minecraft 1.17 was released, it should not be used.");

		return options;
	}

	/**
	 * Retrieves the map of registered mod entity renderers
	 * 
	 * @deprecated Will not work properly in 1.17+, avoid usage, forces construction
	 *             of all entity renderers will forged context.
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	public Map<EntityType<?>, EntityRenderer<?>> getEntities() {
		if (Version.fromString(Modloader.getModloaderGameVersion()).isGreaterOrEqualTo(Version.fromString("1.17")))
			throw new RuntimeException("EntityRendererRegistryEventObject.getEntities() does not work in 1.17+");

		entities.forEach((type, constr) -> {
			@SuppressWarnings("rawtypes")
			EntityRendererContext ctx = new EntityRendererContext(resourceManager, font, textureManager, dispatcher,
					options, type, new Object[0]);
			EntityRenderer<?> inst = (EntityRenderer<?>) constr.apply(ctx);
			rendererInstances.put(type, inst);
		});
		entities.clear();
		return new HashMap<EntityType<?>, EntityRenderer<?>>(rendererInstances);
	}

	/**
	 * Retrieves the map of entity constructors
	 * 
	 * @since ModKit 1.1
	 */
	@SuppressWarnings({ "unchecked" })
	public Map<EntityType<?>, Function<EntityRendererContext<?>, EntityRenderer<?>>> getEntityConstructors() {
		HashMap<EntityType<?>, Function<EntityRendererContext<?>, EntityRenderer<?>>> constructors = new HashMap<EntityType<?>, Function<EntityRendererContext<?>, EntityRenderer<?>>>();
		entities.forEach((type, func) -> {
			constructors.put(type, func);
		});
		entities.clear();
		return constructors;
	}

	/**
	 * Retrieves the font
	 * 
	 * @deprecated Does not work in 1.17+, see addEntity.
	 */
	@Deprecated
	public Font getFont() {
		if (Version.fromString(Modloader.getModloaderGameVersion()).isGreaterOrEqualTo(Version.fromString("1.17")))
			throw new RuntimeException("EntityRendererRegistryEventObject.getFont() does not work in 1.17+");
		warn("DEPRECATION NOTICE: A mod called EntityRendererRegistryEventObject.getFont(), it has been deprecated since Minecraft 1.17 was released, it should not be used.");

		return font;
	}

}
