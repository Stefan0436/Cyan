package org.asf.cyan.api.common;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Automatic detection annotation for CyanComponents.<br/>
 * Requires a protected static initComponent method to be present (no arguments)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@Retention(RUNTIME)
@Target(TYPE_USE)
public @interface CYAN_COMPONENT {
}
