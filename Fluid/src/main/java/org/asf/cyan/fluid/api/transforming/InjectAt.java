package org.asf.cyan.fluid.api.transforming;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

/**
 * Sets where to inject.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@Retention(RUNTIME)
@Target({ METHOD })
public @interface InjectAt {
	public InjectLocation location();
	public String targetCall() default("");
	public String targetOwner() default("");
	public int offset() default (0);
}
