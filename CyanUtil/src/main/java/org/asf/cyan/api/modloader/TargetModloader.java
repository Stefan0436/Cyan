package org.asf.cyan.api.modloader;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 
 * Sets the target modloader of a component.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface TargetModloader {
	public Class<? extends Modloader> value();
	public boolean any() default(false);
}
