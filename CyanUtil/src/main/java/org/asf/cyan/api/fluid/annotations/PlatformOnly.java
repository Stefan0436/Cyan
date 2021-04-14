package org.asf.cyan.api.fluid.annotations;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Target;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;

/**
 * 
 * Platform-only filter for FLUID transformers (only applies to mods with a
 * cyanutil modloader)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@Target(TYPE)
public @interface PlatformOnly {
	public LaunchPlatform value();
}