package org.asf.cyan.fluid.api.transforming;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a parameter as local variable of the method injected into, allowing it
 * to be used.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 */
@Retention(RUNTIME)
@Target({ PARAMETER })
public @interface LocalVariable {
}
