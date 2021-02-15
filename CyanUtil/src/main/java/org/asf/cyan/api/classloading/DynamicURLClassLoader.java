package org.asf.cyan.api.classloading;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

//import org.asf.cyan.api.common.CyanComponent;

/**
 * 
 * Dynamic URL class loader, allows for adding URLs at runtime
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class DynamicURLClassLoader extends URLClassLoader {

	/**
	 * Create a new instance of the dynamic URL class loader
	 */
	public DynamicURLClassLoader() {
		super(new URL[0], null);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param urls URLs to initialize with
	 */
	public DynamicURLClassLoader(URL[] urls) {
		super(urls, null);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param name Class loader name
	 */
	public DynamicURLClassLoader(String name) {
		super(name, new URL[0], null);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param urls URLs to initialize with
	 * @param name Class loader name
	 */
	public DynamicURLClassLoader(String name, URL[] urls) {
		super(name, urls, null);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param parent Parent class loader
	 * @param name   Class loader name
	 */
	public DynamicURLClassLoader(String name, ClassLoader parent) {
		super(name, new URL[0], parent);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param parent Parent class loader
	 */
	public DynamicURLClassLoader(ClassLoader parent) {
		super(new URL[0], parent);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param parent Parent class loader
	 * @param urls   URLs to initialize with
	 */
	public DynamicURLClassLoader(URL[] urls, ClassLoader parent) {
		super(new URL[0], parent);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param parent  Parent class loader
	 * @param urls    URLs to initialize with
	 * @param factory The URLStreamHandlerFactory to use
	 */
	public DynamicURLClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
		super(urls, parent, factory);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param parent Parent class loader
	 * @param urls   URLs to initialize with
	 * @param name   Class loader name
	 */
	public DynamicURLClassLoader(String name, URL[] urls, ClassLoader parent) {
		super(name, urls, parent);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param parent  Parent class loader
	 * @param urls    URLs to initialize with
	 * @param name    Class loader name
	 * @param factory The URLStreamHandlerFactory to use
	 */
	public DynamicURLClassLoader(String name, URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
		super(name, urls, parent, factory);
	}

	/**
	 * Add URL to the class loader
	 * 
	 * @param url URL to add
	 */
	public void addUrl(URL url) {
		super.addURL(url);
	}

	//@Override
	public URL[] getClassPath() {
		return this.getURLs();
	}
	
	public Class<?> getLoadedClass(String name) {
		return CyanClassTracker.loadedClasses.get(name);
	}
	
	@Override
	public Class<?> findClass(String name) throws ClassNotFoundException {
		Class<?> _class = null;
		try {
			if (CyanClassTracker.loadedClasses.containsKey(name)) {
				return CyanClassTracker.loadedClasses.get(name);
			} else throw new ClassNotFoundException("Could not find class "+name);
		} catch (ClassNotFoundException ex) {
			ClassLoader l = getParent();
			if (l == null) l = Thread.currentThread().getContextClassLoader();
			if (l == this) l = ClassLoader.getSystemClassLoader();
			_class = Class.forName(name, true, l);
		}
		if (!CyanClassTracker.loadedClasses.containsKey(name)) {
			CyanClassTracker.loadedClasses.put(name, _class);
		}
		return _class;
	}
		
	@Override
	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class<?> cl = null;
		try {
			cl = super.loadClass(name, resolve);
		} catch (ClassNotFoundException ex) {
			ClassLoader l = getParent();
			if (l == null) l = Thread.currentThread().getContextClassLoader();
			if (l == this) l = ClassLoader.getSystemClassLoader();
			cl = l.loadClass(name);
		}
		if (!CyanClassTracker.loadedClasses.containsKey(name)) {
			CyanClassTracker.loadedClasses.put(name, cl);
		}
		return cl;	
	}
	
	@Override
	public URL getResource(String name) {
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		Class<?> caller = null;
		try {
			caller = findClass(elements[2].getClassName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return caller.getClassLoader().getResource(name);
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		Class<?> caller = null;
		try {
			caller = findClass(elements[2].getClassName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return caller.getClassLoader().getResourceAsStream(name);
	}
	
	/*
	@Override
	public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
		return Class.forName(name, initialize, this);
	}

	@Override
	public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
		return Class.forName(name, initialize, CyanComponent.getAgentClassLoader());
	}
	*/
}
