package org.asf.cyan.fluid.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the target of a Fluid transformer
 * @author Stefan0436 - AerialWorks Software Foundation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.TYPE_USE })
public @interface TargetClass {
  public String target();
}
