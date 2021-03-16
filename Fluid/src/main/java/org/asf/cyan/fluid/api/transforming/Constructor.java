package org.asf.cyan.fluid.api.transforming;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks the method as a constructor so constructors can be transformed. (<b>Not
 * properly implemented, clinit only</b>)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Constructor {
	public boolean clinit() default (false);
}
