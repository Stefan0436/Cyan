package org.asf.cyan.fluid.bytecode;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.config.serializing.internal.Splitter;
import org.asf.cyan.fluid.bytecode.enums.ComparisonMethod;
import org.asf.cyan.fluid.bytecode.sources.IClassSourceProvider;
import org.asf.cyan.fluid.bytecode.sources.LoaderClassSourceProvider;
import org.asf.cyan.fluid.bytecode.sources.URLClassSourceProvider;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * 
 * FLUID Class Pool, ASM Support Class, allows for the loading of classes from
 * jars and folders
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class FluidClassPool extends CyanComponent implements Closeable {
	/**
	 * Main implementation, used to create class pool instances
	 */
	protected static FluidClassPool implementation = new FluidClassPool();
	
	protected FluidClassPool newInstance() {
		return new FluidClassPool();
	}
	
	private class ClassEntry {
		public ClassNode node;
		public String firstName;
	}

	protected FluidClassPool() {
	}

	private ArrayList<IClassSourceProvider<?>> sources = new ArrayList<IClassSourceProvider<?>>();
	private ArrayList<ClassEntry> classes = new ArrayList<ClassEntry>();

	private ByteArrayOutputStream topOutput = new ByteArrayOutputStream();
	private ZipOutputStream output = new ZipOutputStream(topOutput);
	private ArrayList<String> entries = new ArrayList<String>();
	private ArrayList<String> excluded = new ArrayList<String>();
	private ArrayList<String> includedclasses = new ArrayList<String>();

	/**
	 * Gets the URL source providers as URLs.
	 */
	public URL[] getURLSources() {
		return sources.stream().filter(t -> t.providerObject() instanceof URL).map(t -> (URL) t.providerObject())
				.toArray(t -> new URL[t]);
	}

	/**
	 * Add class names to the included list, if none are added, all will be loaded
	 * on jar import.
	 * 
	 * @param name Class name
	 */
	public void addIncludedClass(String name) {
		includedclasses.add(name);
	}

	public ClassNode[] getLoadedClasses() {
		return classes.stream().map(t -> t.node).toArray(t -> new ClassNode[t]);
	}

	/**
	 * Create a class pool with the default classpath.<br />
	 * <b>NOTE: does not added classes loaded by other class loaders, the bootstrap
	 * class path is not added either.</b>
	 * 
	 * @return New FluidClassPool
	 */
	public static FluidClassPool create() {
		FluidClassPool pool = implementation.newInstance();
		pool.addSource(new LoaderClassSourceProvider(ClassLoader.getSystemClassLoader()));
		pool.addDefaultCp();
		return pool;
	}

	/**
	 * Create an empty class pool
	 * 
	 * @return New FluidClassPool
	 */
	public static FluidClassPool createEmpty() {
		return implementation.newInstance();
	}

	/**
	 * Add source URLs, can be a jar or class folder
	 * 
	 * @param source Source URL
	 */
	public void addSource(URL source) {
		addSource(new URLClassSourceProvider(source));
	}

	/**
	 * Add source providers.
	 * 
	 * @param provider Source provider
	 */
	public void addSource(IClassSourceProvider<?> provider) {
		boolean present = false;
		ArrayList<IClassSourceProvider<?>> backupSources = new ArrayList<IClassSourceProvider<?>>(sources);
		switch (provider.getComparisonMethod()) {
		case OBJECT_EQUALS:
			present = backupSources.stream().anyMatch(t -> t.getComparisonMethod() == ComparisonMethod.OBJECT_EQUALS
					&& t.providerObject().equals(provider.providerObject()));
		case CLASS_EQUALS:
			present = backupSources.stream()
					.anyMatch(t -> t.getComparisonMethod() == ComparisonMethod.CLASS_EQUALS && t.providerObject()
							.getClass().getTypeName().equals(provider.providerObject().getClass().getTypeName()));
		case CLASS_ISASSIGNABLE:
			present = backupSources.stream()
					.anyMatch(t -> t.getComparisonMethod() == ComparisonMethod.CLASS_ISASSIGNABLE
							&& t.providerObject().getClass().isAssignableFrom(provider.providerObject().getClass()));
		case LOGICAL_EQUALS:
			present = backupSources.stream().anyMatch(t -> t.getComparisonMethod() == ComparisonMethod.LOGICAL_EQUALS
					&& t.providerObject() == provider.providerObject());
		}

		if (!present) {
			sources.add(provider);
		}
	}

	/**
	 * Loads a class from a byte array
	 * 
	 * @param bytecode Bytecode to convert to a ClassNode
	 * @param name     Class name, renames the class that has been created
	 * @return ClassNode created with the given bytecode, returns existing if one is
	 *         found.
	 */
	public ClassNode readClass(String name, byte[] bytecode) {
		name = name.replaceAll("\\.", "/");

		ArrayList<ClassEntry> clsLstBackup = new ArrayList<ClassEntry>(classes);
		for (ClassEntry cls : clsLstBackup) {
			if (cls.node.name.equals(name)) {
				return cls.node;
			}
		}

		ClassNode node = new ClassNode();
		ClassReader reader = new ClassReader(bytecode);
		reader.accept(node, 0);

		fixLocalVariableNames(node);
		node.name = name;

		ClassEntry entry = new ClassEntry();
		entry.node = node;
		entry.firstName = name.replace(".", "/");
		classes.add(entry);
		return node;
	}

	/**
	 * Loads a class from a stream
	 * 
	 * @param strm The stream to read into a class
	 * @param name Class name, renames the class that has been created
	 * @return ClassNode created with the given bytecode, returns existing if one is
	 *         found.
	 * @throws IOException if the reading fails.
	 */
	public ClassNode readClass(String name, InputStream strm) throws IOException {
		name = name.replaceAll("\\.", "/");

		ArrayList<ClassEntry> clsLstBackup = new ArrayList<ClassEntry>(classes);
		for (ClassEntry cls : clsLstBackup) {
			if (cls.node.name.equals(name))
				return cls.node;
		}

		ClassNode node = new ClassNode();
		ClassReader reader = new ClassReader(strm);
		reader.accept(node, 0);

		fixLocalVariableNames(node);
		node.name = name;

		ClassEntry entry = new ClassEntry();
		entry.node = node;
		entry.firstName = name.replace(".", "/");
		classes.add(entry);
		return node;
	}

	/**
	 * Get a class node by name, loads it if possible, returns a loaded class if
	 * present.
	 * 
	 * @param name Name of the class to get
	 * @return ClassNode
	 * @throws ClassNotFoundException if the class cannot be found.
	 */
	public ClassNode getClassNode(String name) throws ClassNotFoundException {
		name = name.replaceAll("\\.", "/");
		final String nameFinal = name;

		Optional<ClassEntry> oldN = new ArrayList<ClassEntry>(classes).stream()
				.filter(t -> t.firstName.equals(nameFinal)).findFirst();
		if (!oldN.isEmpty())
			return oldN.get().node;

		for (ClassEntry cls : new ArrayList<ClassEntry>(classes)) {
			if (cls.node.name.equals(name))
				return cls.node;
		}

		ArrayList<IClassSourceProvider<?>> backupProviders = new ArrayList<IClassSourceProvider<?>>(sources);
		for (IClassSourceProvider<?> provider : backupProviders) {
			try {
				ClassNode node = new ClassNode();
				InputStream strm = provider.getStream(name);
				if (strm == null)
					continue;

				ClassReader reader = new ClassReader(strm);
				reader.accept(node, 0);
				fixLocalVariableNames(node);
				strm.close();

				ClassEntry entry = new ClassEntry();
				entry.node = node;
				entry.firstName = name.replace(".", "/");
				classes.add(entry);

				return node;
			} catch (IOException ex) {
			}
		}
		throw new ClassNotFoundException("Cannot find class " + name.replaceAll("/", "."));
	}

	/**
	 * Write a file to the memory stream, so it can be saved to an output archive
	 * later, duplicate entries are skipped.
	 * 
	 * @param path Output path
	 * @param data Byte array containing the content
	 * @throws IOException If writing fails
	 */
	public void addFile(String path, byte[] data) throws IOException {
		if (entries.contains(path))
			return;
		path = path.replaceAll("//", "/");

		final String pathFinal = path;
		if (excluded.stream().anyMatch(t -> pathFinal.matches(t)))
			return;

		ZipEntry entry = new ZipEntry(path);
		output.putNextEntry(entry);
		output.write(data);
		output.closeEntry();
		entries.add(path);
	}

	/**
	 * Write a file to the memory stream, so it can be saved to an output archive
	 * later, duplicate entries are skipped.
	 * 
	 * @param path Output path
	 * @param strm InputStream to write into the memory archive.
	 * @throws IOException If writing fails
	 */
	public void addFile(String path, InputStream strm) throws IOException {
		if (entries.contains(path))
			return;
		path = path.replaceAll("//", "/");

		final String pathFinal = path;
		if (excluded.stream().anyMatch(t -> pathFinal.matches(t)))
			return;

		ZipEntry entry = new ZipEntry(path);
		output.putNextEntry(entry);
		strm.transferTo(output);
		output.closeEntry();
		entries.add(path);
	}

	/**
	 * Convert a class to bytecode
	 * 
	 * @param name Class name
	 * @return Byte array
	 */
	public byte[] getByteCode(String name) {
		name = name.replaceAll("\\.", "/");
		ArrayList<ClassEntry> clsLstBackup = new ArrayList<ClassEntry>(classes);
		for (ClassEntry cls : clsLstBackup) {
			if (cls.node.name.equals(name))
				return getByteCode(cls.node);
		}
		return null;
	}

	byte[] getByteCode(ClassNode node) {
		ClassWriter writer = new ClassWriter(0);
		node.accept(writer);
		return writer.toByteArray();
	}

	/**
	 * Import an archive so that its resources are added to the memory stream,
	 * please try to use addSource as much as possible, use this if you don't want
	 * the archive registering as a source.<br/>
	 * If the archive contains classes, they will be added to the class pool.<br/>
	 * Duplicate entries are ignored, the first jar will save them.<br/>
	 * 
	 * @param strm Zip input stream
	 * @throws IOException If importing the archive fails
	 */
	public void importArchive(ZipInputStream strm) throws IOException {
		importArchive(strm, true);
	}

	/**
	 * Import an archive so that its resources are added to the memory stream,
	 * please try to use addSource as much as possible, use this if you don't want
	 * the archive registering as a source.<br/>
	 * If the archive contains classes, they will be added to the class pool.<br/>
	 * Duplicate entries are ignored, the first jar will save them.<br/>
	 * 
	 * @param strm        Zip input stream
	 * @param loadClasses true to load classes that have not been loaded before,
	 *                    false to add them to the archive without loading
	 * @throws IOException If importing the archive fails
	 */
	public void importArchive(ZipInputStream strm, boolean loadClasses) throws IOException {
		ZipEntry entry = strm.getNextEntry();
		while (entry != null) {
			String path = entry.getName().replace("\\", "/");
			if (path.endsWith(".class")) {
				String name = path;
				name = name.substring(0, name.lastIndexOf(".class"));

				final String nameFinal = name;
				if (!this.classes.stream()
						.anyMatch(t -> t.node.name.equals(nameFinal) || t.firstName.equals(nameFinal))) {
					if (loadClasses && (includedclasses.size() == 0 || includedclasses.contains(nameFinal)))
						readClass(name, strm);
					else
						addFile(path, strm);
				}
			} else {
				addFile(path, strm);
			}
			entry = strm.getNextEntry();
		}
	}

	/**
	 * Removes a class from the pool
	 * 
	 * @param name Class name
	 * @throws ClassNotFoundException If the class cannot be found.
	 */
	public void detachClass(String name) throws ClassNotFoundException {
		name = name.replaceAll("\\.", "/");
		ArrayList<ClassEntry> clsLstBackup = new ArrayList<ClassEntry>(classes);
		for (ClassEntry cls : clsLstBackup) {
			if (cls.node.name.equals(name)) {
				classes.remove(cls);
				return;
			}
		}
		throw new ClassNotFoundException("Could not find class " + name.replaceAll("/", "."));
	}

	/**
	 * Exclude an entry so it won't be added to the output stream.
	 * 
	 * @param path Entry regex.
	 */
	public void excludeEntry(String path) {
		if (excluded.contains(path))
			return;
		path = path.replaceAll("//", "/");
		excluded.add(path);
	}

	/**
	 * Write to the output archive, <b>WARNING: imports all resources of all sources
	 * and closes the streams afterwards, added files will need to be re-added if
	 * you want to create another archive.</b>
	 * 
	 * @return Output byte array
	 * @throws IOException If generating the array fails
	 */
	public byte[] writeOutputArchive() throws IOException {
		for (IClassSourceProvider<?> provider : sources) {
			try {
				if (Stream.of(provider.getClass().getMethods())
						.anyMatch(t -> !Modifier.isStatic(t.getModifiers()) && t.getName().equals("isZipLike")
								&& t.getParameterCount() == 0 && boolean.class.isAssignableFrom(t.getReturnType()))
						&& (boolean) provider.getClass().getMethod("isZipLike").invoke(provider)) {
					ZipInputStream strm = new ZipInputStream(provider.getBasicStream());
					importArchive(strm, false);
					strm.close();
				}
			} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | IOException e) {
			}
		}

		for (ClassEntry node : classes) {
			addFile(node.node.name.replaceAll("\\.", "/") + ".class", getByteCode(node.node));
		}

		output.close();
		topOutput.close();
		byte[] outputarr = topOutput.toByteArray();
		entries.clear();
		topOutput = new ByteArrayOutputStream();
		output = new ZipOutputStream(topOutput);
		return outputarr;
	}

	/**
	 * Transfer the output archive to anoter stream, <b>WARNING: imports all
	 * resources of all sources and closes the streams afterwards, added files will
	 * need to be re-added if you want to create another archive.</b>
	 * 
	 * @param outputstrm The output stream
	 * @throws IOException If transferring fails
	 */
	public void transferOutputArchive(OutputStream outputstrm) throws IOException {
		for (IClassSourceProvider<?> provider : sources) {
			try {
				if (Stream.of(provider.getClass().getMethods())
						.anyMatch(t -> !Modifier.isStatic(t.getModifiers()) && t.getName().equals("isZipLike")
								&& t.getParameterCount() == 0 && boolean.class.isAssignableFrom(t.getReturnType()))
						&& (boolean) provider.getClass().getMethod("isZipLike").invoke(provider)) {
					ZipInputStream strm = new ZipInputStream(provider.getBasicStream());
					importArchive(strm, false);
					strm.close();
				}
			} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | IOException e) {
			}
		}

		for (ClassEntry node : classes) {
			addFile(node.node.name.replaceAll("\\.", "/") + ".class", getByteCode(node.node));
		}

		output.close();
		topOutput.close();
		topOutput.writeTo(outputstrm);
		entries.clear();
		topOutput = new ByteArrayOutputStream();
		output = new ZipOutputStream(topOutput);
	}

	private void addDefaultCp() {
		for (String path : Splitter.split(System.getProperty("java.class.path"), ':')) {
			if (path.equals("."))
				continue;

			File f = new File(path);
			try {
				this.addSource(f.toURI().toURL());
			} catch (MalformedURLException e) {
				error("Failed to load class path entry " + path, e);
			}
		}
	}

	@Override
	public void close() throws IOException {
		output.close();
		topOutput.close();
		sources.clear();
		classes.clear();
		entries.clear();
	}

	// Re-generates the variable names if they have unusable names
	private static void fixLocalVariableNames(ClassNode cls) {
		for (MethodNode meth : cls.methods) {
			int varIndex = 1;
			if (meth.localVariables != null) {
				for (LocalVariableNode var : meth.localVariables) {
					if (var.name == null || !var.name.matches("^[A-Za-z0-9_$]+$")) {
						var.name = "var" + varIndex++;
					}
				}
			}
		}
	}

	/**
	 * Reads the bytecode into an existing class
	 * 
	 * @param name     Class name
	 * @param bytecode Bytecode to import
	 * @return New class node
	 * @throws ClassNotFoundException
	 */
	public ClassNode rewriteClass(String name, byte[] bytecode) throws ClassNotFoundException {
		name = name.replaceAll("\\.", "/");
		ArrayList<ClassEntry> clsLstBackup = new ArrayList<ClassEntry>(classes);
		for (ClassEntry cls : clsLstBackup) {
			if (cls.node.name.equals(name)) {
				cls.node = new ClassNode();
				ClassReader reader = new ClassReader(bytecode);
				reader.accept(cls.node, 0);
				fixLocalVariableNames(cls.node);
				return cls.node;
			}
		}
		throw new ClassNotFoundException("Could not find class " + name.replaceAll("/", "."));
	}

	/**
	 * Reads the bytecode into an existing class
	 * 
	 * @param name  Class name
	 * @param input Bytecode stream to import
	 * @return New class node
	 * @throws ClassNotFoundException If the class cannot be found
	 * @throws IOException            If reading fails
	 */
	public ClassNode rewriteClass(String name, InputStream input) throws ClassNotFoundException, IOException {
		name = name.replaceAll("\\.", "/");
		ArrayList<ClassEntry> clsLstBackup = new ArrayList<ClassEntry>(classes);
		for (ClassEntry cls : clsLstBackup) {
			if (cls.node.name.equals(name)) {
				cls.node = new ClassNode();
				ClassReader reader = new ClassReader(input);
				reader.accept(cls.node, 0);
				fixLocalVariableNames(cls.node);
				return cls.node;
			}
		}
		throw new ClassNotFoundException("Could not find class " + name.replaceAll("/", "."));
	}
}
