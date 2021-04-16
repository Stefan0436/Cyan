package org.asf.cyan.api.fluid.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;

/**
 * 
 * Platform-exclude filter for FLUID transformers (only applies to mods with a
 * cyanutil modloader)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface PlatformExclude {
	public LaunchPlatform value();
}
