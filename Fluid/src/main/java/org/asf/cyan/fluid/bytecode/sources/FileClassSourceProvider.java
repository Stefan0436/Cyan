package org.asf.cyan.fluid.bytecode.sources;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.asf.cyan.fluid.bytecode.enums.ComparisonMethod;

/**
 * 
 * URL-based class source provider for the FLUID class pool.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class FileClassSourceProvider implements IClassSourceProvider<String> {

	private File file;

	public FileClassSourceProvider(File file) {
		this.file = file;
	}

	public boolean isZipLike() {
		return file.getName().endsWith(".jar") || file.getName().endsWith(".zip");
	}

	@Override
	public ComparisonMethod getComparisonMethod() {
		return ComparisonMethod.OBJECT_EQUALS;
	}

	@Override
	public String providerObject() {
		return file.getAbsolutePath();
	}

	@Override
	public InputStream getStream(String classType) {
		URL url;
		try {
			url = file.toURI().toURL();
		} catch (MalformedURLException e1) {
			return null;
		}
		if (url.toString().endsWith(".jar") || url.toString().endsWith(".zip")) {
			try {
				url = new URL("jar:" + url.toString() + "!/" + classType + ".class");
			} catch (MalformedURLException e) {
				return null;
			}
		} else {
			try {
				url = new URL(url + "/" + classType + ".class");
			} catch (MalformedURLException e) {
				return null;
			}
		}

		BufferedInputStream strm;
		try {
			strm = new BufferedInputStream(url.openStream());
		} catch (IOException e) {
			return null;
		}
		return strm;
	}

	@Override
	public InputStream getBasicStream() {
		try {
			return new FileInputStream(file);
		} catch (IOException e) {
			return null;
		}
	}

}
