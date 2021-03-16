package org.asf.cyan.fluid.api.transforming;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Sets the modifiers of the method to inject to. (interface transformers only)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 */
@Retention(RUNTIME)
@Target({ TYPE, FIELD, METHOD, CONSTRUCTOR })
public @interface Modifiers {
	public int modifiers();
}
