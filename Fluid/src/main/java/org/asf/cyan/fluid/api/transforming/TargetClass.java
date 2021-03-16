package org.asf.cyan.fluid.api.transforming;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the target of a Fluid transformer.<br />
 * Can also be used for {@link TargetType Type Replacement}.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TargetClass {
	public String target();
}
