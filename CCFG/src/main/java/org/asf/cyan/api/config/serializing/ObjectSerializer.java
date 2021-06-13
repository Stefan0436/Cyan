package org.asf.cyan.api.config.serializing;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.asf.aos.util.service.extra.slib.util.PrimitiveClassUtil;
import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.api.config.serializing.internal.Replacer;
import org.asf.cyan.api.config.serializing.internal.Splitter;

import java.util.ArrayList;

/**
 * 
 * CCFG Serialization Engine
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ObjectSerializer {

	static String[] escapeChars = new String[] { "\\", "'" };
	static {
		ArrayList<String> chars = new ArrayList<String>();
		Stream.of(escapeChars).forEach(t -> chars.add(t));
		for (int ch = 0; ch <= Character.MAX_VALUE; ch++) {
			if (Character.isISOControl(ch)) {
				String str = Integer.toOctalString(ch);
				while (str.length() < 3)
					str = "0" + str;
				chars.add(str);
			}
		}
		escapeChars = chars.toArray(t -> new String[t]);
	}

	@SuppressWarnings("rawtypes")
	private static class MapPutPropAction extends CCFGPutPropAction {
		Map<String, Object> mp = null;
		ArrayList<IOException> exs = new ArrayList<IOException>();
		ParameterizedType type;
		ClassLoader classLoader;

		@SuppressWarnings("unchecked")
		public MapPutPropAction(Class<?> cls, ParameterizedType maptype, ClassLoader classLoader) throws IOException {
			this.classLoader = classLoader;
			type = maptype;
			try {
				mp = (Map<String, Object>) cls.getConstructor().newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new IOException("Unable to instantiate map, parameterless constructor unavailable.", e);
			}
		}

		@Override
		public boolean isValidEntry(String line) {
			return line.matches("^[^\r\n\t'{}\\[\\]]*> (.|\\R)*( *)$");
		}

		public Object[] processEntry(String line) {
			Object[] data = new Object[6]; // 0 = key, 1 = line out, 2(int) = brquote, 3(boolean) = wasBrQuote,
											// 4(boolean) = indent, 5(boolean) = skip_current

			Matcher m = Pattern.compile("^([^\r\n\t'{}\\[\\]]*)> (.|\\R)*( *)$").matcher(line);
			m.matches();
			data[0] = m.group(1);
			String dat = m.group(2);
			boolean isCategory = dat.startsWith("{");

			if (isCategory) {
				data[2] = 1;
				data[3] = true;
				data[4] = true;
				data[5] = true;
			} else {
				line = line.substring(data[0].toString().length() + 2);
			}
			data[1] = line;

			return data;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void run(String key, String txt) {
			try {
				if (type.getActualTypeArguments()[1] instanceof ParameterizedType) {
					ParameterizedType paramtype = (ParameterizedType) type.getActualTypeArguments()[1];
					Class<?> cls = classLoader.loadClass(paramtype.getRawType().getTypeName());

					if (Map.class.isAssignableFrom(cls)) {
						MapPutPropAction a = new MapPutPropAction(cls, paramtype, classLoader);
						parse(txt, a);
						for (IOException ex : a.exs) {
							throw ex;
						}
						mp.put(key, a.mp);
					} else {
						Object o = deserialize(txt, cls);
						mp.put(key, o);
					}

					return;
				}
				Class cls = classLoader.loadClass(type.getActualTypeArguments()[1].getTypeName());
				Object o = deserialize(txt, cls);
				mp.put(key, o);
			} catch (IOException e) {
				exs.add(e);
			} catch (ClassNotFoundException e) {
				// Should not happen
			}
		}
	}

	/**
	 * De-serialize CCFG-formatted element string
	 * 
	 * @param <T>   Output type
	 * @param input Input string
	 * @param cls   Output class
	 * @return De-serialized object
	 * @throws IOException if the object is not supported
	 */
	@SuppressWarnings("unchecked")
	public static <T> T deserialize(String input, Class<T> cls) throws IOException {
		if (PrimitiveClassUtil.isSupportedPrimitive(cls)) {
			return (T) PrimitiveClassUtil.getPrimitive(deserialize(input, PrimitiveClassUtil.getWrapperClass(cls)));
		}

		switch (cls.getTypeName()) {
		case "java.lang.String":

			for (String ch : escapeChars) {
				input = input.replace("\\" + ch, ch);
			}
			input = input.replaceAll("([^\\\\])\\\\r", "$1\r");
			input = input.replaceAll("\\\\r", "\\r");
			input = input.replaceAll("([^\\\\])\\\\t", "$1\t");
			input = input.replaceAll("\\\\t", "\t");

			return (T) input;
		case "java.net.URL":
			return (T) new URL(input);
		case "java.lang.Boolean":
			switch (input.toLowerCase()) {
			case "true":
				return (T) Boolean.TRUE;
			case "false":
				return (T) Boolean.FALSE;
			default:
				throw new IOException("Invalid value");
			}
		case "java.lang.Character":
			if (input.length() != 1)
				throw new IOException("Invalid value, not a single character");
			return (T) Character.valueOf(input.charAt(0));
		case "java.math.BigInteger":
			try {
				return (T) new BigInteger(input);
			} catch (NumberFormatException e) {
				throw new IOException(e);
			}
		case "java.math.BigDecimal":
			try {
				return (T) new BigDecimal(input);
			} catch (NumberFormatException e) {
				throw new IOException(e);
			}
		case "java.util.UUID":
			return (T) UUID.fromString(input);
		default:
			if (Configuration.class.isAssignableFrom(cls)) {
				try {
					Method meth = null;
					try {
						meth = cls.getDeclaredMethod("instantiateFromSerialzer", Class.class);
					} catch (NoSuchMethodException e) {
						try {
							meth = Configuration.class.getDeclaredMethod("instantiateFromSerialzer", Class.class);
						} catch (NoSuchMethodException e1) {
						}
					}
					meth.setAccessible(true);
					Configuration<?> outp = (Configuration<?>) meth.invoke(null, cls);
					return (T) outp.readAll(input);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
						| SecurityException e) {
					throw new IOException("Could not instantiate the configuration", e);
				}
			} else if (PrimitiveClassUtil.isSupportedWrapper(cls)) {
				try {
					Method meth = cls.getMethod("valueOf", String.class);
					meth.setAccessible(true);
					return (T) meth.invoke(null, input);
				} catch (IllegalArgumentException | NoSuchMethodException | SecurityException | IllegalAccessException
						| InvocationTargetException e) {
					throw new IOException(e);
				}
			} else if (cls.isEnum()) {
				final String info = input;
				if (Stream.of(cls.getFields()).anyMatch(t -> t.getName().equals(info))) {
					try {
						return (T) Stream.of(cls.getFields()).filter(t -> t.getName().equals(info)).findFirst().get()
								.get(null);
					} catch (IllegalArgumentException | IllegalAccessException | SecurityException e) {
						throw new IOException(e);
					}
				} else
					throw new IOException("No ENUM value " + input + " in class " + cls.getTypeName());
			} else if (cls.isArray()) {
				input = Replacer.removeChar(input, '\r');
				ArrayList<String> inputLst = new ArrayList<String>();
				StringBuilder str = null;
				boolean indent = false;
				boolean escape = false;
				boolean quote = false;
				int arrayc = 0;
				int brquote = 0;
				for (int characterNum = 0; characterNum < input.length(); characterNum++) {
					char character = input.charAt(characterNum);
					switch (character) {
					case ' ':
						if (!quote && str != null && arrayc == 0 && brquote == 0) {
							inputLst.add(str.toString());
							str = null;
						} else if (str != null) {
							str.append(character);
							escape = false;
						}
						break;
					case '\'':
						if (str == null)
							str = new StringBuilder();
						if (!escape && arrayc == 0 && brquote == 0) {
							quote = !quote;
						} else {
							str.append(character);
							escape = false;
						}
						break;
					case '\\':
						if (str == null)
							str = new StringBuilder();
						if (!escape && arrayc == 0 && brquote == 0) {
							escape = true;
						} else {
							str.append(character);
							escape = false;
						}
						break;
					case '{':
						if (str == null)
							str = new StringBuilder();
						if (brquote != 0)
							str.append(character);
						if (!quote) {
							brquote++;
							indent = true;
						}
						break;
					case '}':
						if (str == null)
							str = new StringBuilder();
						if (!quote) {
							brquote--;
						} else if (brquote == 0 && !quote)
							indent = false;

						if (brquote != 0)
							str.append(character);
						break;
					case '[':
						if (str == null)
							str = new StringBuilder();

						str.append(character);
						if (brquote == 0 && !quote) {
							arrayc++;
						}
						break;
					case ']':
						if (str == null)
							str = new StringBuilder();
						if (brquote == 0 && !quote && arrayc != 0) {
							arrayc--;
						}
						if (arrayc != 0 || brquote != 0 || quote)
							str.append(character);
						break;
					case '\n':
						if (indent) {
							for (int i = 0; i < 4; i++) {
								if (characterNum + 1 < input.length() && input.charAt(characterNum + 1) == ' ')
									characterNum++;
							}
						}
						str.append(System.lineSeparator());
						break;
					default:
						if (str == null)
							str = new StringBuilder();
						str.append(character);
						escape = false;
					}
				}
				if (str != null && str.length() != 0)
					inputLst.add(str.toString());

				Object array = Array.newInstance(cls.getComponentType(), inputLst.size());
				int i = 0;
				for (String itm : inputLst)
					Array.set(array, i++, deserialize(itm, cls.getComponentType()));

				return (T) array;
			}
			throw new IOException("Unsupported object");
		}
	}

	/**
	 * De-serialize CCFG-formatted element string
	 * 
	 * @param <T>      Output type
	 * @param input    Input string
	 * @param field    Input field (for map support)
	 * @param accessor Accessing object (such as the configuration, must own the
	 *                 field)
	 * @return De-serialized object
	 * @throws IOException if the object is not supported
	 */
	@SuppressWarnings("unchecked")
	public static <T> T deserialize(String input, Field field, Object accessor) throws IOException {
		if (Map.class.isAssignableFrom(field.getType())) {
			try {
				Class<?> type = field.getType();
				try {
					type = field.get(accessor).getClass();
				} catch (Exception e) {
				}
				MapPutPropAction a = new MapPutPropAction(type, (ParameterizedType) field.getGenericType(),
						accessor.getClass().getClassLoader());
				parse(input, a);
				for (IOException ex : a.exs) {
					throw ex;
				}
				return (T) a.mp;
			} catch (IllegalArgumentException | IOException e) {
			}
		}
		return (T) deserialize(input, field.getType());
	}

	/**
	 * Serialize an object into a CCFG-formatted element string.
	 * 
	 * @param <T>   Input type
	 * @param input Input object
	 * @return CCFG-serialized element string representing the input object
	 * @throws IOException If the element is not supported
	 */
	public static <T> String serialize(T input) throws IOException {
		return serialize(input, "");
	}

	static <T> String serialize(T input, String indent) throws IOException {
		Class<?> cls = input.getClass();

		switch (cls.getTypeName()) {
		case "java.lang.String":
			String output = input.toString();
			for (String ch : escapeChars) {
				output = output.replace(ch, "\\" + ch);
			}
			output = output.replaceAll("\\\\\\\\r", "\\\\\\\\\\\\r");
			output = output.replaceAll("\r", "\\\\r");
			output = output.replaceAll("\\\\\\\\t", "\\\\\\\\\\\\t");
			output = output.replaceAll("\t", "\\\\t");

			return output;
		case "java.net.URL":
			return input.toString();
		case "java.lang.Boolean":
			return input.toString().toLowerCase();
		case "java.lang.Character":
		case "java.math.BigInteger":
		case "java.util.UUID":
			return input.toString();
		case "java.math.BigDecimal":
			return ((BigDecimal) input).toPlainString();
		default:
			if (Configuration.class.isAssignableFrom(cls)) {
				StringBuilder builder = new StringBuilder();
				boolean first = true;
				String[] lines = Splitter.split(((Configuration<?>) input).toString(), System.lineSeparator());
				for (int i = 0; i < lines.length; i++) {
					String line = lines[i];
					if (i + 1 == lines.length && line.isEmpty()) {
						break;
					}

					if (first) {
						builder.append(indent).append(line);
						first = false;
					} else
						builder.append(System.lineSeparator()).append(indent).append(line);
				}
				return builder.toString();
			} else if (PrimitiveClassUtil.isSupportedWrapper(cls))
				return input.toString();
			else if (cls.isEnum())
				return input.toString();
			else if (cls.isArray()) {
				boolean isMapLike = Map.class.isAssignableFrom(cls.getComponentType())
						|| Configuration.class.isAssignableFrom(cls.getComponentType());
				StringBuilder arrayTxt = new StringBuilder();
				arrayTxt.append(" ");
				int l = Array.getLength(input);

				String prefix = "";
				String suffix = "";

				if (String.class.isAssignableFrom(cls.getComponentType())) {
					prefix = "'";
					suffix = prefix;
				} else if (isMapLike) {
					prefix = "{" + System.lineSeparator();
					suffix = System.lineSeparator() + indent + "}";
					indent += "    ";
				}

				for (int i = 0; i < l; i++)
					arrayTxt.append(prefix).append(serialize(Array.get(input, i), indent)).append(suffix).append(" ");

				String out = arrayTxt.toString();
				if (out.equals(" "))
					out = "";

				return out;
			} else if (Map.class.isAssignableFrom(cls)) {
				Map<?, ?> mp = (Map<?, ?>) input;
				if (mp.size() > 0 && !(mp.keySet().toArray()[0] instanceof String))
					throw new IOException("Unsupported key object type");
				for (Object key : mp.keySet()) {
					if (!key.toString().matches("^[^\r\n\t'{}\\[\\]>]*$")) {
						throw new IOException(
								"Invalid key name in map, disallowed characters: (single quote), '\n', '\r', '\t', '{', '}', '[', ']', '>'");
					}
				}
				ArrayList<IOException> exs = new ArrayList<IOException>();
				String data = getCCFGString(mp, new CCFGGetPropAction<Map<?, ?>>(indent) {

					@Override
					public Object getProp(String key) {
						return mp.get(key);
					}

					@Override
					public String[] keys() {
						return mp.keySet().stream().map(t -> t.toString()).toArray(t -> new String[t]);
					}

					@Override
					public void error(IOException exception) {
						exs.add(exception);
					}

					@Override
					public String processPrefix(String key) {
						return null;
					}

					@Override
					public String processSuffix(String key) {
						return null;
					}

					@Override
					public String getLinePrefix() {
						return data[0].toString();
					}

				});
				for (IOException e : exs) {
					throw e;
				}
				return data;
			}
			throw new IOException("Unsupported object");
		}
	}

	/**
	 * Parse CCFG-formatted information
	 * 
	 * @param content Content
	 * @param action  Action
	 */
	public static void parse(String content, CCFGPutPropAction action) {
		content = Replacer.removeChar(content, '\r');
		String[] lines = Splitter.split(content, '\n');
		boolean indent = false;
		boolean quote = false;
		boolean wasBrQuote = false;
		int array = 0;
		int brquote = 0;
		StringBuilder txt = null;
		String key = "";

		for (int lineNum = 0; lineNum < lines.length; lineNum++) {
			if (txt != null)
				txt.append("\n");
			String line = lines[lineNum];
			if (indent)
				for (int i = 0; i < 4; i++)
					if (line.startsWith(" "))
						line = line.substring(1);

			if ((line.startsWith("#") && !quote) || (line.isBlank() && !quote && brquote == 0))
				continue;

			if (!quote && brquote == 0) {
				if (line != "" && action.isValidEntry(line)) {
					Object[] data = action.processEntry(line);
					key = data[0].toString();
					line = data[1].toString();
					if (data[2] != null)
						brquote = (int) data[2];
					if (data[3] != null)
						wasBrQuote = (boolean) data[3];
					if (data[4] != null)
						indent = (boolean) data[4];
					if (data[5] != null && (boolean) data[5])
						continue;
				}
			}

			boolean escape = false;
			for (int chNum = 0; chNum < line.length(); chNum++) {
				boolean stop = false;
				char ch = line.charAt(chNum);
				if (!escape) {
					if (txt == null)
						txt = new StringBuilder();
					switch (ch) {
					case '\\':
						if (brquote == 0 && (!quote || chNum + 1 < line.length() && line.charAt(chNum + 1) == '\'')
								&& array == 0) {
							escape = true;
						} else
							txt.append(ch);
						if (brquote != 0 && quote && chNum + 1 < line.length() && line.charAt(chNum + 1) == '\'') {
							quote = !quote;
						}
						break;
					case '\'':
						if (array == 0) {
							quote = !quote;
							if (brquote != 0)
								txt.append(ch);
							if (!quote && brquote == 0)
								stop = true;
						} else
							txt.append(ch);
						break;
					case '{':
						if (!quote && array == 0) {
							brquote++;
						}
						txt.append(ch);
						break;
					case '}':
						if (!quote && array == 0) {
							brquote--;
						}
						if (brquote != 0 || quote || array != 0)
							txt.append(ch);
						else
							indent = false;

						break;
					case '[':
						if (array != 0 || quote || brquote != 0)
							txt.append(ch);
						if (brquote == 0 && !quote) {
							array++;
						}
						break;
					case ']':
						if (brquote == 0 && !quote && array != 0) {
							array--;
						}
						if (array != 0 || quote || brquote != 0)
							txt.append(ch);
						break;
					case ' ':
						if (brquote == 0 && !quote && array == 0) {
							stop = true;
						} else
							txt.append(ch);
						break;
					default:
						txt.append(ch);
					}
				} else {
					txt.append(ch);
					escape = false;
				}

				if ((brquote == 0 && wasBrQuote) || stop)
					break;
			}

			if ((brquote != 0 && wasBrQuote) || array != 0)
				continue;

			if (!key.isEmpty() && (!quote || (brquote == 0 && wasBrQuote)) && txt != null) {
				action.run(key, txt.toString());
				txt = null;
				key = "";
				quote = false;
			}

			if (brquote == 0 && wasBrQuote)
				wasBrQuote = false;
		}

		if (!key.equals("") && txt != null) {
			action.run(key, txt.toString());
			txt = null;
			key = "";
			quote = false;
		}
	}

	/**
	 * Generate a CCFG configuration from the data variable
	 * 
	 * @param <T>    Input type
	 * @param data   Input object
	 * @param action Property processing action
	 * @return CCFG Configuration
	 */
	public static <T> String getCCFGString(T data, CCFGGetPropAction<T> action) {
		String output = action.processPrefix(null);
		String pref = action.getLinePrefix();
		StringBuilder builder = new StringBuilder();

		if (output == null)
			output = "";
		else {
			for (String line : Splitter.split(Replacer.removeChar(output, '\r'), '\n'))
				builder.append(pref).append(line).append(System.lineSeparator());

			builder.append(System.lineSeparator());
		}
		boolean wascommented = false;
		boolean first = true;
		boolean wasemtpy = false;
		String[] keys = action.keys();
		for (String key : keys) {
			try {
				Object d = action.getProp(key);
				String v = serialize(d);
				int length = Splitter.split(v, '\n').length;
				if (first) {
					builder.append(pref)
							.append(toCCFGEntry(key, v, d.getClass(), action, true, wascommented, wasemtpy));
					first = false;
				} else
					builder.append(System.lineSeparator()).append(pref)
							.append(toCCFGEntry(key, v, d.getClass(), action, false, wascommented, wasemtpy));
				String prefix = action.processPrefix(key);
				String suffix = action.processSuffix(key);
				wascommented = (prefix != null && !prefix.isEmpty()) || (suffix != null && !suffix.isEmpty());
				wasemtpy = false;

				if (Map.class.isAssignableFrom(d.getClass()) || Configuration.class.isAssignableFrom(d.getClass())) {
					wasemtpy = length == 0;
				}

				action.onAdd(key, v);
			} catch (IOException e) {
				action.error(e);
			}
		}

		String aSuffix = action.processSuffix(null);
		if (aSuffix != null) {
			String newOut = "";

			for (String line : Splitter.split(Replacer.removeChar(aSuffix, '\r'), '\n'))
				newOut += System.lineSeparator() + pref + line;

			builder.append(newOut);
		}

		return builder.toString();
	}

	/**
	 * Generate a CCFG entry
	 * 
	 * @param <T>              Input type
	 * @param key              Key name
	 * @param value            CCFG-serialized property value
	 * @param type             Property type
	 * @param action           Property processing action
	 * @param firstEntry       If the entry is the first entry
	 * @param lastWasCommented If the last entry was not commented
	 * @return CCFG-formatted entry string
	 */
	public static <T> String toCCFGEntry(String key, String value, Class<?> type, CCFGGetPropAction<T> action,
			boolean firstEntry, boolean lastWasCommented, boolean lastWasEmptyMap) {
		if (PrimitiveClassUtil.isSupportedPrimitive(type)) {
			return toCCFGEntry(key, value, PrimitiveClassUtil.getWrapper(type), action, firstEntry, lastWasCommented,
					lastWasEmptyMap);
		}
		value = Replacer.removeChar(value, '\r');
		String output = "";
		String aPrefix = action.processPrefix(key);
		String aSuffix = action.processSuffix(key);
		String okey = key;
		key = action.postProcessKey(key);
		output += key + "> ";
		String prefix = "";
		String suffix = "";
		if (aPrefix != null && !aPrefix.isEmpty()) {
			output = (!firstEntry && !lastWasCommented ? System.lineSeparator() : "") + aPrefix + System.lineSeparator()
					+ output;
		} else if (!firstEntry && !lastWasCommented
				&& (Map.class.isAssignableFrom(type) || Configuration.class.isAssignableFrom(type))) {
			output = System.lineSeparator() + output;
		}

		if (aSuffix == null)
			aSuffix = "";
		else
			aSuffix = " " + aSuffix;

		boolean indent = false;

		switch (type.getTypeName()) {
		case "java.lang.String":
			prefix = "'";
			suffix = "'" + aSuffix;
			break;
		default:
			if (PrimitiveClassUtil.isSupportedPrimitive(type)) {
				suffix += aSuffix;
			} else if (Configuration.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
				prefix = "{" + System.lineSeparator();
				suffix = System.lineSeparator() + "}" + aSuffix;
				indent = true;
			} else if (type.isArray()) {
				prefix = "[";
				suffix = "]" + aSuffix;
			}
		}

		if (aPrefix != null && !aPrefix.isEmpty()) {
			suffix += System.lineSeparator();
		}

		output += prefix;
		boolean first = true;
		String pref = System.lineSeparator() + (indent ? "    " : "");
		String[] values = Splitter.split(value, '\n');
		StringBuilder builder = new StringBuilder();
		builder.append(output);
		for (String line : values) {
			if (first) {
				builder.append((indent ? "    " : "")).append(line);
				first = false;
			} else
				builder.append(pref).append(line);
		}
		output = builder.toString();
		if (values.length == 0 && (Map.class.isAssignableFrom(type) || Configuration.class.isAssignableFrom(type))) {
			suffix = suffix.substring(1);
		}
		output += suffix;

		output = action.postProcess(okey, output);
		return output;
	}
}
