package org.asf.cyan.fluid.bytecode.sources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.asf.cyan.fluid.bytecode.enums.ComparisonMethod;

/**
 * 
 * ClassLoader-based source provider for the FLUID class pool.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class LoaderClassSourceProvider implements IClassSourceProvider<ClassLoader> {

	private ClassLoader classLoader;

	public LoaderClassSourceProvider(ClassLoader loader) {
		classLoader = loader;
	}

	@Override
	public ComparisonMethod getComparisonMethod() {
		return ComparisonMethod.LOGICAL_EQUALS;
	}

	@Override
	public ClassLoader providerObject() {
		return classLoader;
	}

	@Override
	public InputStream getStream(String classType) {
		URL u = classLoader.getResource(classType + ".class");
		if (u == null) {
			return null;
		}
		try {
			return u.openStream();
		} catch (IOException e2) {
		}
		return null;
	}

	@Override
	public InputStream getBasicStream() {
		return null;
	}

}
