package modkit.events.objects.ingame.blocks;

import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * 
 * Entity Renderer Context - Contains information passed to block entity
 * renderers for construction.
 * 
 * @since ModKit 1.1
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class BlockEntityRendererContext<T extends BlockEntity> {

	@SuppressWarnings("unused")
	private BlockEntityRendererContext() {
	}

	public BlockEntityRendererContext(Font font, TextureManager textureManager,
			BlockEntityRenderDispatcher dispatcher, Options options, BlockEntityType<T> entityType,
			Object[] gameTypes) {
		this.font = font;
		this.textureManager = textureManager;
		this.dispatcher = dispatcher;
		this.options = options;
		this.gameTypes = gameTypes;
		Object[] types = new Object[5 + gameTypes.length];
		int i2 = 0;
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
	private TextureManager textureManager;
	private BlockEntityRenderDispatcher dispatcher;
	private Options options;
	private Object[] gameTypes;
	private BlockEntityType<T> entityType;

	/**
	 * Retrieves the block entity renderer dispatcher instance
	 */
	public BlockEntityRenderDispatcher getDispatcher() {
		return dispatcher;
	}

	/**
	 * Retrieves the font instance
	 */
	public Font getFont() {
		return font;
	}

	/**
	 * Retrieves the block entity type
	 */
	public BlockEntityType<T> getType() {
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
