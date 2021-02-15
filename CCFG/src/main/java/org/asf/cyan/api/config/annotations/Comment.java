package org.asf.cyan.api.config.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 
 * CCFG Comment, comment a configuration or property
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@Repeatable(Comments.class)
@Retention(RUNTIME)
@Target({ FIELD, TYPE })
public @interface Comment {
	/**
	 * Comment lines
	 * 
	 * @return Array of lines in the comment
	 */
	String[] value();

	/**
	 * True if the comment is displayed AFTER the value, false if before the key
	 * 
	 * @return True if displayed after the value, false otherwise
	 */
	boolean afterValue() default false;
}
