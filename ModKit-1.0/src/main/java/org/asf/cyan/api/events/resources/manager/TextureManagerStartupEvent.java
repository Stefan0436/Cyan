package org.asf.cyan.api.events.resources.manager;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;
import org.asf.cyan.api.events.objects.resources.TextureManagerEventObject;

/**
 * 
 * Texture Manager Startup Event -- Called on texture manager startup
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class TextureManagerStartupEvent extends AbstractExtendedEvent<TextureManagerEventObject> {

	private static TextureManagerStartupEvent implementation;

	@Override
	public String channelName() {
		return "modkit.start.textures.manager";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static TextureManagerStartupEvent getInstance() {
		return implementation;
	}

}
