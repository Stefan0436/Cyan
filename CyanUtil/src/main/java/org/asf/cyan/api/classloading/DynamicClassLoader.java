package org.asf.cyan.api.classloading;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.nio.ByteBuffer;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.HashMap;

/**
 * 
 * Dynamic URL class loader, allows for adding URLs at runtime.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class DynamicClassLoader extends URLClassLoader {
	
	private boolean secured = false;
	
	/**
	 * Secures the class loader, prevents options from being set. (can only be called ONCE)
	 */
	public void secure() {
		if (secured)
			throw new IllegalStateException("Classloader already secured!");
		
		apply();
		
		secured = true;
	}
	
	private boolean hasOption(int opt) {
		return (opt & options) == opt;
	}
	
	/**
	 * Apply all options, cannot be done after secure has been called. (secure calls this too)
	 */
	public void apply() {
		if (secured)
			throw new IllegalStateException("Classloader has been secured! Applying options denied!");
		
		if (hasOption(OPTION_LOAD))
			allowSelfToLoad = true;
		if (hasOption(OPTION_ALLOW_DEFINE))
			allowSelfToDefine = true;
		if (hasOption(OPTION_DENY_ADD_RUNTIME))
			denyAdding = true;
	}
	
	/**
	 * Allow the defining of classes. (can only be set before adding urls or calling secure)
	 */
	public static final int OPTION_ALLOW_DEFINE = 0x10;

	/**
	 * Sets the class loader to allow the loading of classes.
	 */
	public static final int OPTION_LOAD = 0x20;

	/**
	 * Prevents the adding of sources after the classloader has been secured.
	 */
	public static final int OPTION_DENY_ADD_RUNTIME = 0x30;

	/**
	 * Prevents the class loader from being secured by addUrl or addUrls.
	 */
	public static final int OPTION_PREVENT_AUTOSECURE = 0x40;
	
	private int options = 0;
	public void setOptions(int options) {
		if (secured)
			throw new IllegalStateException("Classloader has been secured!");
		
		this.options = this.options | options;
		
		if (hasOption(OPTION_PREVENT_AUTOSECURE))
			noSecureOnAdd = true;
	}
	
	private boolean noSecureOnAdd = false;
	private boolean allowSelfToLoad = false;
	private boolean allowSelfToDefine = false;
	private boolean denyAdding = false;
	private static HashMap<String, String> rewrittenResources = new HashMap<String, String>();

	static {
		System.getProperties().forEach((k, v) -> {
			String key = k.toString();
			String value = v.toString();
			if (key.startsWith("cyancore.resourceloader.rewriteresource.")) {
				rewrittenResources.put(key.substring("cyancore.resourceloader.rewriteresource.".length()), value);
			}
		});
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 */
	public DynamicClassLoader() {
		super(new URL[0], null);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param urls URLs to initialize with
	 */
	public DynamicClassLoader(URL[] urls) {
		super(urls, null);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param name Class loader name
	 */
	public DynamicClassLoader(String name) {
		super(name, new URL[0], null);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param urls URLs to initialize with
	 * @param name Class loader name
	 */
	public DynamicClassLoader(String name, URL[] urls) {
		super(name, urls, null);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param parent Parent class loader
	 * @param name   Class loader name
	 */
	public DynamicClassLoader(String name, ClassLoader parent) {
		super(name, new URL[0], parent);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param parent Parent class loader
	 */
	public DynamicClassLoader(ClassLoader parent) {
		super(new URL[0], parent);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param parent Parent class loader
	 * @param urls   URLs to initialize with
	 */
	public DynamicClassLoader(URL[] urls, ClassLoader parent) {
		super(new URL[0], parent);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param parent  Parent class loader
	 * @param urls    URLs to initialize with
	 * @param factory The URLStreamHandlerFactory to use
	 */
	public DynamicClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
		super(urls, parent, factory);
	}

	/**
	 * Create a new instance of the dynamic URL class loader
	 * 
	 * @param parent Parent class loader
	 * @param urls   URLs to initialize with
	 * @param name   Class loader name
	 */
	public DynamicClassLoader(String name, URL[] urls, ClassLoader parent) {
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
	public DynamicClassLoader(String name, URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
		super(name, urls, parent, factory);
	}

	/**
	 * Add URL to the class loader
	 * 
	 * @param url URL to add
	 */
	public void addUrl(URL url) {
		if (secured && denyAdding)
			throw new RuntimeException("Classloader has been secured, dynamic loading denied.");
		
		super.addURL(url);
		
		if (!secured && !noSecureOnAdd)
			secure();
	}

	/**
	 * Add URLs to the class loader
	 * 
	 * @param urls URLs to add
	 */
	public void addUrls(URL[] urls) {
		if (secured && denyAdding)
			throw new RuntimeException("Classloader has been secured, dynamic loading denied.");
		
		for (URL url : urls) {
			super.addURL(url);
		}
		
		if (!secured && !noSecureOnAdd)
			secure();
	}

	/**
	 * Add URLs to the class loader
	 * 
	 * @param urls URLs to add
	 */
	public void addUrls(Iterable<URL> urls) {
		if (secured && denyAdding)
			throw new RuntimeException("Classloader has been secured, dynamic loading denied.");
		
		for (URL url : urls) {
			super.addURL(url);
		}
		
		if (!secured && !noSecureOnAdd)
			secure();
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
			} else
				_class = super.findClass(name);
		} catch (ClassNotFoundException ex) {
			ClassLoader l = getParent();
			if (l == null)
				l = Thread.currentThread().getContextClassLoader();
			if (l == this)
				l = ClassLoader.getSystemClassLoader();
			_class = Class.forName(name, true, l);
		}
		if (!CyanClassTracker.loadedClasses.containsKey(name)) {
			CyanClassTracker.loadedClasses.put(name, _class);
		}
		return _class;
	}

	@Override
	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (CyanClassTracker.loadedClasses.containsKey(name))
			return CyanClassTracker.loadedClasses.get(name);

		Class<?> cl = null;
		try {
			cl = doLoadClass(name, resolve);
		} catch (ClassNotFoundException | NoClassDefFoundError ex) {
			ClassLoader l = getParent();
			if (l == null)
				l = Thread.currentThread().getContextClassLoader();
			if (l == this)
				l = ClassLoader.getSystemClassLoader();
			cl = l.loadClass(name);
		}

		if (!CyanClassTracker.loadedClasses.containsKey(name)) {
			CyanClassTracker.loadedClasses.put(name, cl);
		}
		return cl;
	}

	Class<?> doLoadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (!allowSelfToLoad) {
			if (getParent() == null)
				return ClassLoader.getSystemClassLoader().loadClass(name);
			else
				return getParent().loadClass(name);
		}
		
		if (!allowSelfToDefine)
			return super.loadClass(name, resolve);

		String path = name.replaceAll("\\.", "/") + ".class";
		for (URL u : getURLs()) {
			try {
				if (u.toString().endsWith(".jar") || u.toString().endsWith(".zip")) {
					try {
						u = new URL("jar:" + u.toString() + "!/" + path);
					} catch (MalformedURLException e) {
					}
				} else {
					try {
						u = new URL(u + "/" + path);
					} catch (MalformedURLException e) {
					}
				}

				BufferedInputStream strm = new BufferedInputStream(u.openStream());
				byte[] data = strm.readAllBytes();
				strm.close();
				
				Class<?> cls = defineClass(name, ByteBuffer.wrap(data), new CodeSource(u, (Certificate[]) null));
				if (resolve)
					this.resolveClass(cls);
				return cls;
			} catch (IOException ex) {
			}
		}
		throw new ClassNotFoundException("Cannot find class " + name);
	}

	private URL getResourceURL(String name) throws MalformedURLException {
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		Class<?> caller = null;
		int index = 2;
		try {
			caller = loadClass(elements[index++].getClassName());
			while (caller.getTypeName().equals(Class.class.getTypeName())
					|| caller.getTypeName().equals(DynamicClassLoader.class.getTypeName())) {
				caller = loadClass(elements[index++].getClassName());
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		ClassLoader cl = caller.getClassLoader();
		if (cl == this)
			cl = Thread.currentThread().getContextClassLoader();
		if (cl == this)
			cl = ClassLoader.getSystemClassLoader();
		URL cSource = caller.getProtectionDomain().getCodeSource().getLocation();
		String prefix = cSource.toString();
		if (rewrittenResources.containsKey(caller.getTypeName() + ".source")) {
			cSource = new File(rewrittenResources.get(caller.getTypeName() + ".source")).toURI().toURL();
			prefix = cSource.toString();
		}
		if (cSource.getProtocol().equals("jar")) {
			prefix = prefix.substring(0, prefix.lastIndexOf("!"));
			prefix = prefix + "!/";
		} else if (cSource.toString().endsWith("jar")) {
			prefix = "jar:" + prefix + "!/";
		} else if (!prefix.endsWith("/"))
			prefix += "/";
		prefix += name;
		return new URL(prefix);
	}

	@Override
	public URL getResource(String name) {
		try {
			URL resource = getResourceURL(name);
			try {
				resource.openStream().close();
			} catch (IOException ex) {
				return null;
			}
			return resource;
		} catch (MalformedURLException e) {
			return null;
		}
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		try {
			URL resource = getResourceURL(name);
			if (resource == null)
				return null;
			return resource.openStream();
		} catch (IOException e) {
			return null;
		}
	}

}
