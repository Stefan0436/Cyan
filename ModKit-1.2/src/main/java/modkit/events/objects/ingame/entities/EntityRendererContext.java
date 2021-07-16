package modkit.events.objects.ingame.entities;

import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * 
 * Entity Renderer Context - Contains information passed to entity renderers for
 * construction.
 * 
 * @since ModKit 1.1
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class EntityRendererContext<T extends Entity> {

	@SuppressWarnings("unused")
	private EntityRendererContext() {
	}

	public EntityRendererContext(ResourceManager resourceManager, Font font, TextureManager textureManager,
			EntityRenderDispatcher dispatcher, Options options, EntityType<T> entityType, Object[] gameTypes) {
		this.resourceManager = resourceManager;
		this.font = font;
		this.textureManager = textureManager;
		this.dispatcher = dispatcher;
		this.options = options;
		this.gameTypes = gameTypes;
		Object[] types = new Object[6 + gameTypes.length];
		int i2 = 0;
		types[i2++] = resourceManager;
		types[i2++] = font;
		types[i2++] = textureManager;
		types[i2++] = dispatcher;
		types[i2++] = options;
		types[i2++] = entityType;
		for (int i = 0; i < gameTypes.length; i++) {
			types[i2++] = gameTypes[i];
		}
		this.entityType = entityType;
	}

	private Font font;
	private ResourceManager resourceManager;
	private TextureManager textureManager;
	private EntityRenderDispatcher dispatcher;
	private Options options;
	private Object[] gameTypes;
	private EntityType<T> entityType;
	
	/**
	 * Retrieves the entity renderer dispatcher instance
	 */
	public EntityRenderDispatcher getDispatcher() {
		return dispatcher;
	}
	
	/**
	 * Retrieves the font instance
	 */
	public Font getFont() {
		return font;
	}

	/**
	 * Retrieves the ResourceManager instance
	 */
	public ResourceManager getResourceManager() {
		return resourceManager;
	}

	/**
	 * Retrieves the entity type
	 */
	public EntityType<T> getType() {
		return entityType;
	}

	/**
	 * Retrieves the client options
	 */
	public Options getOptions() {
		return options;
	}

	/**
	 * Retrieves the texture manager
	 */
	public TextureManager getTextureManager() {
		return textureManager;
	}

	/**
	 * Converts this instance to the given game type (use in 1.17 for retrieving the
	 * context instance)
	 * 
	 * @param <E> Game type
	 * @param cls Game type class
	 * @return Game type or null if incompatible
	 */
	@SuppressWarnings("unchecked")
	public <E> E getAsGameType(Class<E> cls) {
		for (Object type : gameTypes) {
			if (cls.isAssignableFrom(type.getClass()))
				return (E) type;
		}
		return null;
	}
}
