package org.asf.cyan.fluid.api.transforming;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the owner of a Fluid transformer method, for {@link TargetType Type
 * Replacement}.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
public @interface TargetOwner {
	public String owner();
}
