package org.asf.cyan.api.fluid.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 
 * Loader-version-greater-than annotation (filters modloader version support)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface LoaderVersionGreaterThan {
	public String name();

	public String version();

	public String gameVersionList() default ("");
}
