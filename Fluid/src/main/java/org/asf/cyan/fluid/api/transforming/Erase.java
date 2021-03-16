package org.asf.cyan.fluid.api.transforming;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Rewrites a method. (erases existing nodes)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Erase {

}
