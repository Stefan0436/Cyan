package org.asf.cyan.fluid.api.transforming;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Replaces the annotated type to the specified <code>target</code> class, this
 * system is referred to as <b>Type Replacement</b>, it allows for having
 * temporary types that are changed to the target class at runtime.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 */
@Retention(RUNTIME)
@Target({ PARAMETER, METHOD, FIELD })
public @interface TargetType {
	public String target();
}
