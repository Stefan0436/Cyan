package org.asf.cyan.api.config.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 
 * CCFG Comment collection, internal
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@Retention(RUNTIME)
@Target({ FIELD, TYPE })
public @interface Comments {
	/**
	 * Array of comments
	 * @return Array of comments
	 */
	Comment[] value();
}
