package org.asf.cyan.api.config;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;
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
	public static String baseDir = ".";

	private static Consumer<String> warnLogger = null;
	private static Consumer<String> errorLogger = null;

	public static void setLoggers(Consumer<String> warnLogger, Consumer<String> errorLogger) {
		Configuration.warnLogger = warnLogger;
		Configuration.errorLogger = errorLogger;
	}

	boolean enableSave = false;
	static ArrayList<Configuration<?>> configStore = new ArrayList<Configuration<?>>();
	String localBaseDir = "";
	File base = null;
	File conf = null;
	HashMap<String, String> propertiesMemory = new HashMap<String, String>();

	public File getFile() {
		return conf;
	}

	/**
	 * Save all known configuration files
	 */
	public static void saveAllConfigurations() {
		for (Configuration<?> config : configStore) {
			if (config.enableSave) {
				try {
					config.writeAll();
				} catch (IOException e) {
					if (errorLogger != null)
						errorLogger.accept("Failed to write configuration " + config.filename() + ", exception: "
								+ e.getClass().getTypeName() + ": " + e.getMessage());
				}
			}
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

	protected void assignFile(String baseDir) {
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
			return readAll(Files.readString(conf.toPath()), true, false);
		else
			writeAll(true);

		return (T) this;
	}

	/**
	 * Check if the configuration file exists
	 * 
	 * @return True if the file exists, false otherwise
	 */
	public boolean exists() {
		if (filename() != null && folder() != null) {
			return conf.exists();
		} else
			return false;
	}

	@SuppressWarnings("unchecked")
	protected T readAll(String content, boolean allowWrite, boolean newfile) {
		if (allowWrite)
			enableSave = true;

		ObjectSerializer.parse(content, new CCFGPutPropAction() {
			@Override
			public void run(String key, String txt) {
				try {
					Field f = Configuration.this.getClass().getField(key);
					if (Modifier.isStatic(f.getModifiers()))
						throw new NoSuchFieldException(f.getName());
					f.setAccessible(true);
					setProp(f, txt, true);
				} catch (NoSuchFieldException | SecurityException e) {
					if (warnLogger != null && filename() != null)
						warnLogger.accept("Unrecognized config key '" + key + "', config: " + filename());
				} catch (IllegalArgumentException | IOException e) {
					if (errorLogger != null && filename() != null)
						errorLogger.accept("Failed to read configuration '" + filename() + "', exception: "
								+ e.getClass().getTypeName() + ": " + e.getMessage());
				}
			}
		});

		if (allowWrite && hasChanges()) {
			try {
				writeAll();
			} catch (IOException e) {
			}
		}

		return (T) this;
	}

	/**
	 * Read the configuration file from a content string.
	 * 
	 * @param content Content string
	 * @return Self
	 */
	public T readAll(String content) {
		return readAll(content, false, false);
	}

	/**
	 * Save the configuration to its file.
	 * 
	 * @throws IOException If writing fails.
	 */
	public void writeAll() throws IOException {
		writeAll(!exists());
		enableSave = true;
	}

	/**
	 * Save the configuration to its file.
	 * 
	 * @param newfile True if the file is new (will generate all keys), false
	 *                otherwise.
	 * 
	 * @throws IOException If writing fails.
	 */
	protected void writeAll(boolean newfile) throws IOException {
		String oldcontent = null;
		if (!newfile && exists()) {
			oldcontent = Files.readString(conf.toPath()).replaceAll("\r", "");
			if (oldcontent.endsWith("\n")) {
				oldcontent = oldcontent.substring(0, oldcontent.length() - 1);
			}
		}
		if (hasChanges() && !hasChanges(false)) { // FIXME: change when value overwriting is implemented
			if (oldcontent == null || oldcontent.isBlank()) {
				oldcontent = "";
			} else
				oldcontent += System.lineSeparator();
		}
		String ccfg = toString(newfile, oldcontent);
		if (!ccfg.endsWith(System.lineSeparator()))
			ccfg += System.lineSeparator();
		if (!conf.getParentFile().exists())
			conf.getParentFile().mkdirs();

		Files.writeString(conf.toPath(), ccfg);
	}

	/**
	 * 
	 * Generates the CCFG string.
	 * 
	 * @return CCFG String
	 */
	@Override
	public String toString() {
		return toString(true, null);
	}

	/**
	 * 
	 * Generates the CCFG string.
	 * 
	 * @param newfile    True if the file is new (will generate all keys), false
	 *                   otherwise.
	 * @param oldContent Old (unchanged) content.
	 * 
	 * @return CCFG String
	 */
	protected String toString(boolean newfile, String oldContent) {
		if (oldContent == null && newfile) {
			return ObjectSerializer.getCCFGString(this, new CCFGConfigGenerator<T>(this, newfile, oldContent));
		}
		if (!hasChanges() && !newfile) {
			return oldContent;
		}
		if (!newfile && hasChanges() && hasChanges(false)) { // FIXME: change when value overwriting is implemented
			return toString(true, null);
		}
		
		String value = ObjectSerializer.getCCFGString(this, new CCFGConfigGenerator<T>(this, newfile, oldContent));
		if (!newfile) {
			if (!value.isEmpty())
				value = (oldContent.isBlank() ? "" : oldContent + System.lineSeparator()) + value;
			else
				value = oldContent;
		}
		for (Field f : getClass().getFields()) {
			try {
				if (!isExcluded(f)) {
					Object val = f.get(this);
					if (val != null)
						propertiesMemory.put(f.getName(), ObjectSerializer.serialize(val));
				}
			} catch (IllegalArgumentException | IllegalAccessException | IOException e) {
				if (errorLogger != null)
					errorLogger.accept("Failed to generate configuration " + filename() + ", exception: "
							+ e.getClass().getTypeName() + ": " + e.getMessage());
			}
		}
		return value;
	}

	/**
	 * Check if the configuration has changes
	 * 
	 * @return True if changes are present, false otherwise
	 */
	public boolean hasChanges() {
		return hasChanges(true);
	}

	/**
	 * Check if the configuration has changes
	 * 
	 * @param checkIfPresent True to check if the key is present in memory storage,
	 *                       false otherwise.
	 * @return True if changes are present, false otherwise
	 */
	public boolean hasChanges(boolean checkIfPresent) {
		for (Field f : getClass().getFields()) {
			f.setAccessible(true);
			if (!isExcluded(f) && !reqIsPresent(f))
				return true;
			if (!isExcluded(f) && hasChanged(f, getProp(f), checkIfPresent))
				return true;
		}
		return false;
	}

	/**
	 * Check if a key is optional (internal)
	 * 
	 * @param key Input field
	 * @return True if optional, false otherwise
	 */
	protected boolean isOptional(Field key) {
		try {
			return Stream.of(key.getAnnotations())
					.anyMatch(t -> t.annotationType().getTypeName().equals(OptionalEntry.class.getTypeName()));
		} catch (SecurityException e) {
			return false;
		}
	}

	/**
	 * Check if a field is excluded (internal)
	 * 
	 * @param key Input field
	 * @return True if excluded, false otherwise
	 */
	protected boolean isExcluded(Field key) {
		try {
			return Stream.of(key.getAnnotations())
					.anyMatch(t -> t.annotationType().getTypeName().equals(Exclude.class.getTypeName()));
		} catch (SecurityException e) {
			return false;
		}
	}

	protected String getComment(Annotation[] annotations, boolean header) {
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
	 * 
	 * @param key Input field
	 * @return Output CCFG string
	 */
	protected String getProp(Field key) {
		try {
			if (isExcluded(key))
				throw new SecurityException("This property is excluded.");
			Object v = key.get(this);
			if (v == null)
				return null;
			return ObjectSerializer.serialize(v);
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException | IOException e) {
			return null;
		}
	}

	protected void setProp(Field key, String value, boolean setToStore) throws IOException {
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
	 * 
	 * @param key            Field to check
	 * @param comparison     Comparison CCFG-serialized string
	 * @param checkIfPresent True to check if the field is present in property
	 *                       storage.
	 * @return True if the field has changed or is new, false otherwise
	 */
	protected boolean hasChanged(Field key, String comparison, boolean checkIfPresent) {
		Object value = null;
		try {
			value = key.get(this);
		} catch (IllegalArgumentException | IllegalAccessException e) {
		}
		if (checkIfPresent && !reqIsPresent(key) && value != null)
			return true;
		else if (!checkIfPresent && (!reqIsPresent(key) || value == null)) {
			return false;
		}
		String in = propertiesMemory.get(key.getName());
		if (in == null)
			return comparison != null;
		boolean changed = !propertiesMemory.get(key.getName()).equals(comparison);
		return changed;
	}

	/**
	 * Check if a field is present (internal)
	 * 
	 * @param key Field to check
	 * @return True if the field is present, false otherwise
	 */
	protected boolean isPresent(Field key) {
		return propertiesMemory.containsKey(key.getName());
	}

	/**
	 * Check if a field is present, returns true if optional and not present
	 * (internal)
	 * 
	 * @param key Field to check
	 * @return True if the field is present, false otherwise
	 */
	protected boolean reqIsPresent(Field key) {
		if (isPresent(key))
			return true;
		else if (isOptional(key))
			return true;
		else
			return false;
	}

	protected static <T extends Configuration<T>> T instantiateFromSerializer(Class<T> input)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		return (T) input.getConstructor().newInstance();
	}
}
