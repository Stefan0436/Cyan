package org.asf.cyan.mods.dependencies;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 
 * Methods annotated with this annotation will be called when the mod in the
 * method parameters is loaded, so support can be implemented.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface ModSupportHandler {
	public String value();
}
