package org.asf.cyan.mods.events;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 
 * Automatic mod event listener, methods annotated with this annotation are
 * called if the event is run, the method parameters will match the event parameters.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface AttachEvent {
	public String value();
	public boolean synchronize() default (false);
}
