package org.asf.cyan.cornflower.gradle.utilities;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
@Inherited
/**
 * Annotation for automatic task registration, requires the ITaskExtender interface
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public @interface RegisterTask {
}
