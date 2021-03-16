package org.asf.cyan.fluid.api.transforming;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Methods annotated with this annotation are called on class load.<br />
 * <br />
 * The following parameters are supported:<br />
 *  - <code>FluidClassPool</code> - the class pool used to load the target class, also contains all transformer jars.<br />
 *  - <code>ClassNode</code> - the first will contain the target class.<br />
 *  - <code>ClassNode</code> - the second will contain the transformer class.<br />
 *  - <code>String</code> - target class deobfuscated name.<br />
 * <br />
 * Please separate the <code>@ASM</code> methods from your normal transformer,
 * also try not to access ANY game types, it can cause a class dependency
 * loop.<br />
 * <br />
 * <b>Please note that methods annotated with this annotation must be static AND
 * public.</b>
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface ASM {
}
