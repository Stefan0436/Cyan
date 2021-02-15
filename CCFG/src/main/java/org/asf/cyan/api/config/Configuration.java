package org.asf.cyan.api.config;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Stream;

import org.asf.cyan.api.config.annotations.Comment;
import org.asf.cyan.api.config.annotations.Comments;
import org.asf.cyan.api.config.annotations.Exclude;
import org.asf.cyan.api.config.annotations.OptionalEntry;
import org.asf.cyan.api.config.serializing.CCFGPutPropAction;
import org.asf.cyan.api.config.serializing.ObjectSerializer;

/**
 * 
 * CCFG Configuration Class
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 * @param <T> Self
 *
 */
public abstract class Configuration<T extends Configuration<T>> {
	/**
	 * The configuration base directory
	 */
	@Exclude
	public static String baseDir = "";

	boolean enableSave = false;
	static ArrayList<Configuration<?>> configStore = new ArrayList<Configuration<?>>();
	String localBaseDir = "";
	File base = null;
	File conf = null;
	HashMap<String, String> propertiesMemory = new HashMap<String, String>();

	/**
	 * Save all known configuration files
	 */
	public static void saveAllConfigurations() {
		for (Configuration<?> config : configStore) {
			if (config.enableSave)
				config.writeAll();
		}
	}

	/**
	 * Construct new instance of this configuration using the default base directory
	 */
	public Configuration() {
		this(baseDir);
	}

	/**
	 * Construct new instance of this configuration using the base directory
	 * specified
	 * 
	 * @param baseDir The base directory to use
	 */
	public Configuration(String baseDir) {
		localBaseDir = baseDir;
		configStore.add(this);
		if (filename() != null && folder() != null && localBaseDir != null) {
			base = new File(localBaseDir + "/" + folder());
			conf = new File(base, filename());
		}
	}

	/**
	 * Configuration file name
	 * 
	 * @return File name
	 */
	public abstract String filename();

	/**
	 * Configuration folder name/path, such as config/test1/test0 or just config
	 * 
	 * @return Folder path
	 */
	public abstract String folder();

	/**
	 * Read the configuration from its file. (or create it if not found)<br/>
	 * If the configuration object contains new properties, they will be written to
	 * the file.
	 * 
	 * @return Self
	 * @throws IOException If reading fails.
	 */
	@SuppressWarnings("unchecked")
	public T readAll() throws IOException {
		if (!base.exists())
			base.mkdirs();

		if (conf.exists())
			return readAll(Files.readString(conf.toPath()), true);
		else
			writeAll(true);

		return (T) this;
	}
	
	/**
	 * Check if the configuration file exists
	 * @return True if the file exists, false otherwise
	 */
	public boolean exists() {
		if (filename() != null && folder() != null) {
			return conf.exists();
		} else return false;
	}

	@SuppressWarnings("unchecked")
	T readAll(String content, boolean allowWrite) {
		if (allowWrite)
			enableSave = true;

		ObjectSerializer.parse(content, new CCFGPutPropAction() {
			@Override
			public void run(String key, String txt) {
				try {
					Field f = Configuration.this.getClass().getField(key);
					f.setAccessible(true);
					setProp(f, txt, true);
				} catch (NoSuchFieldException | SecurityException e) {
					// TODO: Warning log for unrecognized keys
				} catch (IllegalArgumentException | IOException e) {
					// TODO: Write error to logs
				}
			}			
		});

		if (hasChanges() && allowWrite)
			writeAll();

		return (T) this;
	}
	
	/**
	 * Read the configuration file from a content string.
	 * 
	 * @param content Content string
	 * @return Self
	 */
	public T readAll(String content) {
		return readAll(content, false);
	}

	/**
	 * Save the configuration to its file (not yet implemented, read only api for now)
	 */
	@Deprecated
	public void writeAll() {
		writeAll(!exists());
		enableSave = true;
	}

	/**
	 * Not yet implemented, read only api for now
	 */
	@Deprecated
	void writeAll(boolean newfile) {
		// TODO
	}

	/**
	 * Not yet implemented, read only api for now
	 */
	@Deprecated
	@Override
	public String toString() {
		return toString(true);
	}

	private String toString(boolean newfile) {
		// TODO
		return "";
	}

	/**
	 * Check if the configuration has changes
	 * 
	 * @return True if changes are present, false otherwise
	 */
	public boolean hasChanges() {
		for (Field f : getClass().getFields()) {
			f.setAccessible(true);
			if (hasChanged(f, getProp(f)) && !isExcluded(f))
				return true;
		}
		return false;
	}

	/**
	 * Check if a key is optional (internal)
	 * @param key Input field
	 * @return True if optional, false otherwise
	 */
	boolean isOptional(Field key) {
		try {
			return Stream.of(key.getAnnotations())
					.anyMatch(t -> t.annotationType().getTypeName().equals(OptionalEntry.class.getTypeName()));
		} catch (SecurityException e) {
			return false;
		}
	}

	/**
	 * Check if a field is excluded (internal)
	 * @param key Input field
	 * @return True if excluded, false otherwise
	 */
	boolean isExcluded(Field key) {
		try {
			return Stream.of(key.getAnnotations())
					.anyMatch(t -> t.annotationType().getTypeName().equals(Exclude.class.getTypeName()));
		} catch (SecurityException e) {
			return false;
		}
	}

	private String getComment(Annotation[] annotations, boolean header) {
		if (Stream.of(annotations).anyMatch(t -> t.annotationType().getTypeName().equals(Comment.class.getTypeName())
				&& !((Comment) t).afterValue()) == header) {
			String comment = "";
			Optional<Comment> o = Stream.of(annotations)
					.filter(t -> t.annotationType().getTypeName().equals(Comment.class.getTypeName()))
					.map(t -> (Comment) t).findFirst();
			if (!o.isEmpty()) {
				for (String line : o.get().value()) {
					if (comment != "")
						comment += System.lineSeparator() + line;
					else
						comment = line;
				}
				return comment;
			}
		}
		if (Stream.of(annotations)
				.anyMatch(t -> t.annotationType().getTypeName().equals(Comments.class.getTypeName()))) {
			String comment = "";
			Optional<Comments> o = Stream.of(annotations)
					.filter(t -> t.annotationType().getTypeName().equals(Comments.class.getTypeName()))
					.map(t -> (Comments) t).findFirst();
			if (!o.isEmpty()) {
				for (Comment commentC : o.get().value()) {
					if (commentC.afterValue() == header)
						continue;
					for (String line : commentC.value()) {
						if (comment != "")
							comment += System.lineSeparator() + line;
						else
							comment = line;
					}
				}
				return comment;
			}
		}
		return "";
	}

	/**
	 * Get the configuration header comment (won't return current comment, only the
	 * default)
	 * 
	 * @return Header comment
	 */
	public String getConfigHeader() {
		return getComment(getClass().getAnnotations(), true);
	}

	/**
	 * Get the configuration footer comment (won't return current comment, only the
	 * default)
	 * 
	 * @return Footer comment
	 */
	public String getConfigFooter() {
		return getComment(getClass().getAnnotations(), false);
	}

	/**
	 * Get a property header comment (comment before property tag, won't return
	 * current comment, only the default)
	 * 
	 * @param key The property
	 * @return Header comment
	 */
	public String getKeyHeaderComment(Field key) {
		return getComment(key.getAnnotations(), true);
	}

	/**
	 * Get a property footer comment (comment after value, won't return current
	 * comment, only the default)
	 * 
	 * @param key The property
	 * @return Header comment
	 */
	public String getKeyFooterComment(Field key) {
		return getComment(key.getAnnotations(), false);
	}

	/**
	 * Get the CCFG-serialized value of a field
	 * @param key Input field
	 * @return Output CCFG string
	 */
	String getProp(Field key) {
		try {
			if (isExcluded(key))
				throw new SecurityException("This property is excluded.");
			Object v =key.get(this);
			if (v == null) 
				return null;
			return ObjectSerializer.serialize(v);
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException | IOException e) {
			return null;
		}
	}

	private void setProp(Field key, String value, boolean setToStore) throws IOException {
		try {
			if (isExcluded(key))
				throw new SecurityException("This property is excluded.");
			key.set(this, ObjectSerializer.deserialize(value, key, this));
			if (setToStore)
				propertiesMemory.put(key.getName(), value);
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException | IOException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Check if a field has changed (internal)
	 * @param key Field to check
	 * @param comparison Comparison CCFG-serialized string
	 * @return True if the field has changed or is new, false otherwise
	 */
	boolean hasChanged(Field key, String comparison) {
		if (!reqIsPresent(key))
			return true;
		String in = propertiesMemory.get(key.getName());
		if (in == null) return in == comparison;
		boolean changed = !propertiesMemory.get(key.getName()).equals(comparison);
		return changed;
	}

	/**
	 * Check if a field is present (internal)
	 * @param key Field to check
	 * @return True if the field is present, false otherwise
	 */
	boolean isPresent(Field key) {
		return propertiesMemory.containsKey(key.getName());
	}

	/**
	 * Check if a field is present, returns true if optional and not present (internal)
	 * @param key Field to check
	 * @return True if the field is present, false otherwise
	 */
	boolean reqIsPresent(Field key) {
		if (isPresent(key))
			return true;
		else if (isOptional(key))
			return true;
		else
			return false;
	}
	
	protected static <T extends Configuration<T>> T instanciateFromSerialzer(Class<T> input) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		return (T)input.getConstructor().newInstance();
	}
}
