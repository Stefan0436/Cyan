package org.asf.cyan.mods.events;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import org.asf.cyan.api.events.extended.IExtendedEvent;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 
 * Automatic mod event listener, methods annotated with this annotation are
 * called if the event is run, this annotation applies to 'Extended Events'
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface SimpleEvent {
	public Class<? extends IExtendedEvent<?>> value();

	public boolean synchronize() default (false);
}
