package modkit.events.objects.resources;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.client.renderer.texture.TextureManager;

/**
 * 
 * Texture Manager Event Object -- Event object for all events related to the
 * texture manager.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class TextureManagerEventObject extends EventObject {
	private TextureManager textureManager;

	public TextureManagerEventObject(TextureManager textureManager) {
		this.textureManager = textureManager;
	}

	/**
	 * Retrieves the resource manager
	 */
	public TextureManager getTextureManager() {
		return textureManager;
	}

}
