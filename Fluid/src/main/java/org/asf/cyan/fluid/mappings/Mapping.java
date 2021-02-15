package org.asf.cyan.fluid.mappings;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.asf.cyan.api.config.CCFGConfigGenerator;
import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.api.config.annotations.Exclude;
import org.asf.cyan.api.config.annotations.OptionalEntry;
import org.asf.cyan.api.config.serializing.ObjectSerializer;

public class Mapping<T extends Configuration<T>> extends Configuration<T> {

	static Map<Character, String> descriptors = Map.of('V', "void", 'Z', "boolean", 'I', "int", 'J', "long", 'D',
			"double", 'F', "float", 'S', "short", 'C', "char", 'B', "byte");

	@Exclude
	public static int maxProgress = 0;

	@Exclude
	public static int progress = 0;

	protected Mapping() {
		super();
	}

	@SuppressWarnings("unchecked")
	protected static <T extends Configuration<T>> T instanciateFromSerialzer(Class<T> input)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		if (!Mapping.class.isAssignableFrom(input))
			return Configuration.instanciateFromSerialzer(input);
		return (T) new Mapping<T>();
	}

	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
	}

	public MAPTYPE mappingType = MAPTYPE.TOPLEVEL;

	@OptionalEntry
	public String type = null;

	@OptionalEntry
	public String[] argumentTypes = new String[0];

	@OptionalEntry
	public String name = null;

	@OptionalEntry
	public String obfuscated = null;

	@OptionalEntry
	public Mapping<?>[] mappings = new Mapping<?>[0];

	public MAPTYPE getMappingType() {
		return mappingType;
	}

	/**
	 * Temporary CCFG Generator for the FLUID API, until the CCFG API is properly
	 * implemented, use this (FIXME)
	 * 
	 * @return CCFG-configuration
	 */
	@SuppressWarnings("unchecked")
	@Override
	public String toString() {
		return ObjectSerializer.getCCFGString(this, new CCFGConfigGenerator<T>((T) this, true));
	}

	/**
	 * Parse (Mojang) ProGuard obfuscation mappings into this configuration
	 * 
	 * @param mappings Jar mappings input text
	 * @return Self
	 */
	@SuppressWarnings("unchecked")
	public <M extends Mapping<M>> T parseProGuardMappings(String mappings) {
		mappingType = MAPTYPE.TOPLEVEL;
		name = null;
		type = null;
		obfuscated = null;

		mappings = mappings.replaceAll("\r", "");
		mappings = mappings.replaceAll("\t", "    ");
		ArrayList<Mapping<M>> mappingsLst = new ArrayList<Mapping<M>>();
		ArrayList<Mapping<M>> mappingsLstFmp = new ArrayList<Mapping<M>>();
		Mapping<M> mp = null;
		String[] lines = mappings.split("\n");
		maxProgress = lines.length;
		for (String line : lines) {
			progress++;
			if (line.startsWith("#"))
				continue;
			if (line.startsWith("    ")) {
				mappingsLstFmp.add(new Mapping<T>().parseProGuardEntry(line.substring(4)));
			} else {
				if (mp != null) {
					mp.mappings = mappingsLstFmp.toArray(t -> new Mapping[t]);
					mappingsLst.add(mp);
					mappingsLstFmp.clear();
					mp = null;
				}
				mp = new Mapping<M>().parseProGuardEntry(line);
			}
		}
		if (mp != null) {
			mp.mappings = mappingsLstFmp.toArray(t -> new Mapping[t]);
			mappingsLst.add(mp);
			mappingsLstFmp.clear();
			mp = null;
		}
		progress = maxProgress;

		this.mappings = mappingsLst.toArray(t -> new Mapping[t]);
		progress = 0;
		maxProgress = 0;
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	private <M extends Mapping<M>> M parseProGuardEntry(String entry) {
		mappingType = MAPTYPE.PROPERTY;
		if (entry.endsWith(":")) {
			entry = entry.substring(0, entry.length() - 1);
			mappingType = MAPTYPE.CLASS;
		}
		String output = entry.substring(entry.indexOf(" -> ") + 4);
		String input = entry.substring(0, entry.indexOf(" -> "));
		if (input.matches("^([0-9]+:[0-9]+:)?[A-Za-z0-9.$_\\[\\]]+ .*\\(.*\\).*$")) {
			mappingType = MAPTYPE.METHOD;
			Matcher m = Pattern.compile("^([0-9]+:[0-9]+:)?([A-Za-z0-9.$_\\[\\]]+) (.*)").matcher(input);
			m.matches();
			if (m.group(1) != null)
				input = input.substring(m.group(1).length());
			String arguments = input.substring(input.indexOf("(") + 1);
			arguments = arguments.substring(0, arguments.lastIndexOf(")"));

			if (!arguments.isEmpty()) {
				arguments = arguments.replaceAll(", ", ",");
				argumentTypes = arguments.split(",");
			}

			input = input.substring(0, input.indexOf("("));
		}

		switch (mappingType) {
		case CLASS:
			name = input;
			obfuscated = output;
			break;
		default:
			name = input.substring(input.indexOf(" ") + 1);
			type = input.substring(0, input.indexOf(" "));
			obfuscated = output;
		}

		return (M) this;
	}

	/**
	 * Parse TSRG mappings into this configuration, WARNING: NO TYPE SUPPORT FOR
	 * PROPERTIES
	 * 
	 * @param mappings Jar mappings input text
	 * @return Self
	 */
	@SuppressWarnings("unchecked")
	public <M extends Mapping<M>> T parseTSRGMappings(String mappings) {
		mappingType = MAPTYPE.TOPLEVEL;
		name = null;
		type = null;
		obfuscated = null;

		mappings = mappings.replaceAll("\r", "");
		mappings = mappings.replaceAll("\t", "    ");
		ArrayList<Mapping<M>> mappingsLst = new ArrayList<Mapping<M>>();
		ArrayList<Mapping<M>> mappingsLstFmp = new ArrayList<Mapping<M>>();
		Mapping<M> mp = null;
		String[] lines = mappings.split("\n");
		maxProgress = lines.length;
		for (String line : lines) {
			progress++;
			if (line.startsWith("#"))
				continue;
			if (line.startsWith("    ")) {
				mappingsLstFmp.add(new Mapping<T>().parseTSRGEntry(line.substring(4), false));
			} else {
				if (mp != null) {
					mp.mappings = mappingsLstFmp.toArray(t -> new Mapping[t]);
					mappingsLst.add(mp);
					mappingsLstFmp.clear();
					mp = null;
				}
				mp = new Mapping<M>().parseTSRGEntry(line, true);
			}
		}
		if (mp != null) {
			mp.mappings = mappingsLstFmp.toArray(t -> new Mapping[t]);
			mappingsLst.add(mp);
			mappingsLstFmp.clear();
			mp = null;
		}
		progress = maxProgress;

		this.mappings = mappingsLst.toArray(t -> new Mapping[t]);
		for (Mapping<?> m : mappingsLst) {
			for (Mapping<?> t : m.mappings) {
				if (t.mappingType.equals(MAPTYPE.METHOD)) {
					String[] types = t.argumentTypes;
					if (types.length != 0) {
						int ind = 0;
						for (String type : types) {
							String tSuffix = "";
							if (type.contains("[]")) {
								tSuffix = type.substring(type.indexOf("["));
								type = type.substring(0, type.indexOf("["));
							}
							Mapping<?> map = mapClassToMapping(type, t2 -> true, true);
							if (map != null)
								types[ind++] = map.name + tSuffix;
							else
								ind++;
						}
						t.argumentTypes = types;
					}
					Mapping<?> map2 = mapClassToMapping(t.type, t2 -> true, true);
					if (map2 != null)
						t.type = map2.name;
				}
			}
		}

		this.mappings = mappingsLst.toArray(t -> new Mapping[t]);
		progress = 0;
		maxProgress = 0;
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	private <M extends Mapping<M>> M parseTSRGEntry(String entry, boolean isClass) {
		mappingType = (isClass ? MAPTYPE.CLASS : MAPTYPE.PROPERTY);
		String output = entry.substring(entry.indexOf(" ") + 1);
		String input = entry.substring(0, entry.indexOf(" "));
		if (output.matches("^\\(.*\\).* [A-Za-z0-9.$_\\[\\]]+$")) {
			mappingType = MAPTYPE.METHOD;
			Matcher m = Pattern.compile("^\\((.*)\\)(.*) ([A-Za-z0-9.$_\\[\\]]+)$").matcher(output);
			m.matches();
			String arguments = m.group(1);
			ArrayList<String> argumentTypes = new ArrayList<String>();

			int arrays = 0;
			int l = arguments.length();
			boolean parseName = false;
			StringBuilder arg = new StringBuilder();
			for (int i = 0; i < l; i++) {
				char ch = arguments.charAt(i);
				if (ch == 'L' && !parseName) {
					parseName = true;
				} else if (!parseName && ch == '[') {
					arrays++;
				} else if (!parseName) {
					arg.append(descriptors.get(ch));
					if (arrays != 0) {
						for (int i2 = 0; i2 < arrays; i2++) {
							arg.append("[]");
						}
						arrays = 0;
					}
					argumentTypes.add(arg.toString());
					arg = new StringBuilder();
				} else {
					if (ch == '/')
						arg.append('.');
					else if (ch != ';') {
						arg.append(ch);
					} else {
						if (arrays != 0) {
							for (int i2 = 0; i2 < arrays; i2++) {
								arg.append("[]");
							}
							arrays = 0;
						}
						argumentTypes.add(arg.toString());
						arg = new StringBuilder();
						parseName = false;
					}
				}
			}

			String returnT = m.group(2);
			arg = new StringBuilder();
			l = returnT.length();
			for (int i = 0; i < l; i++) {
				char ch = returnT.charAt(i);
				if (ch == 'L' && !parseName) {
					parseName = true;
				} else if (!parseName && ch == '[') {
					arrays++;
				} else if (!parseName) {
					arg.append(descriptors.get(ch));
					if (arrays != 0) {
						for (int i2 = 0; i2 < arrays; i2++) {
							arg.append("[]");
						}
						arrays = 0;
					}
					returnT = arg.toString();
					break;
				} else {
					if (ch == '/')
						arg.append('.');
					else if (ch != ';') {
						arg.append(ch);
					} else {
						if (arrays != 0) {
							for (int i2 = 0; i2 < arrays; i2++) {
								arg.append("[]");
							}
							arrays = 0;
						}
						returnT = arg.toString();
						break;
					}
				}
			}
			arg = null;

			output = m.group(3);
			type = returnT;
			this.argumentTypes = argumentTypes.toArray(t -> new String[t]);
		}

		name = output;
		obfuscated = input;

		if (mappingType.equals(MAPTYPE.CLASS) && name.contains("/"))
			name = name.replaceAll("/", ".");

		return (M) this;
	}
	
	public Mapping<?> mapClassToMapping(String input, Function<Mapping<?>, Boolean> fn, boolean obfuscated) {
		for (Mapping<?> map : mappings) {
			if ((obfuscated ? map.obfuscated : map.name).equals(input) && map.mappingType == MAPTYPE.CLASS) {
				if (fn.apply(map))
					return map;
			}
		}
		return null;
	}
}
