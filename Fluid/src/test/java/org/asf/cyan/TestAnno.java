package org.asf.cyan;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

@Retention(RUNTIME)
@Target(METHOD)
public @interface TestAnno {
	public int test1();
	public String test2();
	public double test3();
	public InjectLocation test4();
}
