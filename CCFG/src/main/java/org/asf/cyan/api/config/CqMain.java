package org.asf.cyan.api.config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Stream;

import org.asf.cyan.api.config.serializing.ObjectSerializer;

/**
 * 
 * CQ - CCFG Interface for the command line
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class CqMain {
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException,
			ClassNotFoundException, NoSuchFieldException {
		if (args.length < 3) {
			error();
		} else {
			ArrayList<URL> urls = new ArrayList<URL>();
			String path = "";
			String cls = "";
			String input = "";

			boolean raw = false;
			boolean view = false;
			String output = "-";

			HashMap<String, String> propChanges = new HashMap<String, String>();
			boolean outputCCFG = false;
			boolean noparse = false;
			boolean option = false;
			String key = "";

			int argindex = 0;
			for (int i = 0; i < args.length; i++) {
				String arg = args[i];
				if (!noparse) {
					if (!option) {
						if (arg.startsWith("--") && !arg.equals("--")) {
							key = arg.substring(2);
							if (!key.equals("raw") && !key.equals("viewmode") && !key.equals("ccfg-output")
									&& !key.equals("ccfg")) {
								option = true;
								continue;
							} else {
								if (key.equals("raw")) {
									raw = true;
									continue;
								} else if (key.equals("viewmode")) {
									view = true;
									continue;
								} else if (key.equals("ccfg-output") || key.equals("ccfg")) {
									outputCCFG = true;
									continue;
								}
							}
						} else if (arg.equals("--")) {
							noparse = true;
						} else if (arg.equals("-s")) {
							option = true;
							key = "set";
							continue;
						}
					} else {
						if (key.equals("output")) {
							output = arg;
						} else if (key.equals("source-jar")) {
							urls.add(new File(arg).toURI().toURL());
						} else if (key.equals("set")) {
							if (i + 1 < args.length) {
								propChanges.put(arg, args[i + 1]);
								i++;
							} else {
								error();
							}
							key = "";
						}
						option = false;
						continue;
					}
				}

				if (argindex == 0) {
					cls = arg;
				} else if (argindex == 1) {
					input = arg;
				}

				argindex++;
				if (argindex <= 2) {
					continue;
				}

				if (!path.isEmpty())
					path += " ";
				path += arg;
			}

			if (cls.isEmpty() || input.isEmpty())
				error();

			if (input.equals("-")) {
				input = "";
				Scanner sc = new Scanner(System.in);
				while (sc.hasNext()) {
					input += sc.nextLine() + System.lineSeparator();
				}
				sc.close();
			} else {
				if (!input.contains("\n") && !input.contains("\\n")) {
					input = Files.readString(Path.of(input));
				} else if (input.contains("\\n")) {
					input = input.replaceAll("\\\\n", "\n");
				}
			}

			URLClassLoader clloader = new URLClassLoader(urls.toArray(t -> new URL[t]), CqMain.class.getClassLoader());
			Class<?> ccfgcls = Class.forName(cls, true, clloader);

			Configuration<?> ccfg = ObjectSerializer.deserialize(input, (Class<? extends Configuration>) ccfgcls);
			boolean getSelf = false;
			if (path.equals(".")) {
				getSelf = true;
			}

			if (path.startsWith("."))
				path = path.substring(1);

			boolean array = false;
			boolean map = false;

			Object arr = null;
			Map<?, ?> mp = null;

			ArrayList<String> pathEntries = parsePath(path, getSelf);
			int index = 0;
			int length = pathEntries.size();
			for (String pathEntry : pathEntries) {
				if (index + 1 == length) {
					Object value;
					if (array) {
						value = Array.get(arr, Integer.valueOf(pathEntry));
					} else if (!map) {
						if (pathEntry.isEmpty()) {
							value = ccfg;
						} else {
							value = ccfg.getClass().getField(pathEntry).get(ccfg);
						}
					} else {
						if (pathEntry.isEmpty()) {
							value = mp;
						} else {
							value = mp.get(pathEntry);
						}
					}

					for (String setKey : propChanges.keySet()) {
						String setVal = propChanges.get(setKey);
						boolean setSelf = false;
						if (setKey.equals("."))
							setSelf = true;

						if (setKey.startsWith("."))
							setKey = setKey.substring(1);
						ArrayList<String> setPath = parsePath(setKey, setSelf);

						Object setting = value;
						if (setting instanceof Map<?, ?> || value instanceof Configuration<?>) {
							int ind = 0;
							for (String pth : setPath) {
								if (setting instanceof Map<?, ?>) {
									if (!((Map) setting).containsKey(pth) || ind + 1 == setPath.size()) {
										String targetKey = pth;
										if (ind != setPath.size()) {
											for (int i = ind + 1; i < setPath.size(); i++) {
												targetKey += "." + setPath.get(i);
											}
										}

										Class<?> type = String.class;
										if (((Map) setting).containsKey(pth)) {
											type = ((Map) setting).get(pth).getClass();
										}

										if (type.getTypeName().equals(String.class.getTypeName()))
											((Map) setting).put(targetKey, setVal);
										else
											((Map) setting).put(targetKey, ObjectSerializer.deserialize(setVal, type));

										break;
									} else {
										setting = ((Map) setting).get(pth);
									}
								} else if (setting instanceof Configuration<?>) {
									if (ind + 1 == setPath.size()) {
										Field f = setting.getClass().getField(pth);
										if (f.getType().getTypeName().equals(String.class.getTypeName()))
											f.set(setVal, setting);
										else
											((Configuration) setting).setProp(f, setVal, false);
									} else {
										if (Stream.of(setting.getClass().getFields())
												.anyMatch(t -> t.getName().equals(pth))) {
											setting = setting.getClass().getField(pth).get(setting);
										} else {
											error();
										}
									}
								} else {
									error();
								}
								ind++;
							}
						} else if (setSelf) {
							value = ObjectSerializer.deserialize(setVal, value.getClass());
						}
					}

					String outputTxt;
					if (outputCCFG) {
						outputTxt = ObjectSerializer.serialize(value);
					} else
						outputTxt = serialize(value, raw, 0, view);
					if (output.equals("-")) {
						if (raw) {
							System.out.print(outputTxt);
						} else {
							System.out.println(outputTxt);
						}
					} else {
						Files.writeString(Path.of(output), outputTxt + (!raw ? System.lineSeparator() : ""));
					}
				} else {
					Object value = null;
					if (map) {
						value = mp.get(pathEntry);
					} else {
						value = ccfg.getClass().getField(pathEntry).get(ccfg);
					}
					if (value instanceof Map) {
						map = true;
						mp = (Map<?, ?>) value;
					} else if (value.getClass().isArray()) {
						array = true;
						arr = value;
					} else {
						map = false;
						ccfg = (Configuration<?>) ccfg.getClass().getField(pathEntry).get(ccfg);
					}
				}
				index++;
			}
		}

	}

	private static ArrayList<String> parsePath(String path, boolean getSelf) {
		ArrayList<String> pathEntries = new ArrayList<String>();
		if (getSelf) {
			pathEntries.add("");
		} else {
			boolean escape = false;
			boolean quote = false;

			StringBuilder buffer = new StringBuilder();
			for (char ch : path.toCharArray()) {
				if (!escape) {
					if (ch == '\\') {
						escape = true;
						continue;
					} else if (ch == '\"') {
						quote = !quote;
						continue;
					}

					if (!quote) {
						if (ch == '.') {
							pathEntries.add(buffer.toString());
							buffer = new StringBuilder();
						} else {
							buffer.append(ch);
						}
					} else {
						buffer.append(ch);
					}
				} else {
					buffer.append(ch);
					escape = false;
				}
			}

			if (!buffer.toString().isEmpty()) {
				pathEntries.add(buffer.toString());
			}
		}
		return pathEntries;
	}

	private static void error() {
		System.err.println("Usage: cq <config class> <input> [options] [entry path]");
		System.err.println("Options:");
		System.err.println("    --raw                   - output bash");
		System.err
				.println("    --viewmode              - outputs values in human-readable format (specific keys only)");
		System.err.println("    --output <output file>  - sets output");
		System.err.println("    --source-jar <jar path> - adds source jars for the config class");
		System.err.println("    --set -s <key> <value>  - sets properties");
		System.err.println("    --ccfg-output           - outputs CCFG");
		System.err.println("");
		System.err.println("Entry path format examples:");
		System.err.println("[key], [key].[subkey], [key].[array-index] or [key].[mapkey]");
		System.exit(-1);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static String serialize(Object value, boolean raw, int indent, boolean view) {
		String outputTxt = "";
		if (value instanceof Configuration<?>) {
			CCFGConfigGenerator serializer = new CCFGConfigGenerator((Configuration) value, true, null);
			StringBuilder out = new StringBuilder();
			if (!raw) {
				out.append("{");
			} else {
				if (indent != 0) {
					out.append("\"");
				}
				out.append("(");
			}

			if (!raw) {
				out.append(System.lineSeparator());
			} else {
				if (serializer.keys().length != 0)
					out.append(" ");
			}

			String[] keys = serializer.keys();
			int index = 0;
			for (String k : keys) {
				Object v = serializer.getProp(k);

				if (!raw) {
					for (int i = 0; i < indent; i++) {
						out.append(" ");
					}
					out.append("  ");
				}

				if (raw) {
					if (indent == 0)
						out.append("[\"");
					else
						out.append("[\\\"");
				}
				String key = serialize(k, raw, indent + 2, false);
				if (indent != 0 && raw) {
					key = key.replace("\\\"", "\\\\\"").replace("\\n", "\\\\n");
					key = key.replace("\"", "\\\"").replace("\n", "\\n");
				}
				out.append(key);

				if (!raw) {
					out.append(": ");
				} else {
					if (indent == 0)
						out.append("\"]=");
					else
						out.append("\\\"]=");
				}
				String val = serialize(v, raw, indent + 2, false);
				if (indent != 0 && raw) {
					val = val.replace("\\\"", "\\\\\"");
					val = val.replace("\"", "\\\"");
				}
				out.append(val);
				if (val.isEmpty() && raw)
					out.append("\"\"");

				if (!raw) {
					if (index + 1 != keys.length) {
						out.append(",");
					}
					out.append(System.lineSeparator());
				} else {
					out.append(" ");
				}

				index++;
			}

			if (!raw) {
				for (int i2 = 0; i2 < indent; i2++) {
					out.append(" ");
				}
				out.append("}");
			} else {
				out.append(")");
				if (indent != 0) {
					out.append("\"");
				}
			}

			outputTxt = out.toString();
		} else if (value instanceof Map<?, ?>) {
			StringBuilder out = new StringBuilder();
			if (!raw) {
				out.append("{");
			} else {
				if (indent != 0) {
					out.append("\"");
				}
				out.append("(");
			}

			if (!raw) {
				out.append(System.lineSeparator());
			} else {
				if (((Map<?, ?>) value).size() != 0)
					out.append(" ");
			}

			Set<Object> keys = (Set<Object>) ((Map<?, ?>) value).keySet();
			int index = 0;
			for (Object k : keys) {
				Object v = ((Map<?, ?>) value).get(k);

				if (!raw) {
					for (int i = 0; i < indent; i++) {
						out.append(" ");
					}
					out.append("  ");
				}

				if (raw) {
					if (indent == 0)
						out.append("[\"");
					else
						out.append("[\\\"");
				}
				String key = serialize(k, raw, indent + 2, false);
				if (indent != 0 && raw) {
					key = key.replace("\\\"", "\\\\\"").replace("\\n", "\\\\n");
					key = key.replace("\"", "\\\"").replace("\n", "\\n");
				}
				out.append(key);
				if (!raw) {
					out.append(": ");
				} else {
					if (indent == 0)
						out.append("\"]=");
					else
						out.append("\\\"]=");
				}
				String val = serialize(v, raw, indent + 2, false);
				if (indent != 0 && raw) {
					val = val.replace("\\\"", "\\\\\"");
					val = val.replace("\"", "\\\"");
				}
				out.append(val);
				if (val.isEmpty() && raw)
					out.append("\"\"");

				if (!raw) {
					if (index + 1 != keys.size()) {
						out.append(",");
					}
					out.append(System.lineSeparator());
				} else {
					out.append(" ");
				}

				index++;
			}

			if (!raw) {
				for (int i2 = 0; i2 < indent; i2++) {
					out.append(" ");
				}
				out.append("}");
			} else {
				out.append(")");
				if (indent != 0) {
					out.append("\"");
				}
			}

			outputTxt = out.toString();
		} else if (value.getClass().isArray()) {
			StringBuilder out = new StringBuilder();
			if (!raw) {
				out.append("[");
			} else {
				if (indent != 0) {
					out.append("\"");
				}
				out.append("(");
			}
			boolean first = true;
			for (int i = 0; i < Array.getLength(value); i++) {
				if (!raw) {
					out.append(System.lineSeparator());
					for (int i2 = 0; i2 < indent; i2++) {
						out.append(" ");
					}
					out.append("  ");
				} else if (first) {
					out.append(" ");
				}

				first = false;

				out.append(serialize(Array.get(value, i), raw, indent + 2, false));
				if (raw) {
					out.append(" ");
				} else {
					if (i + 1 != Array.getLength(value)) {
						out.append(",");
					} else {
						out.append(System.lineSeparator());
					}
				}
			}
			if (!raw) {
				if (Array.getLength(value) != 0) {
					for (int i2 = 0; i2 < indent; i2++) {
						out.append(" ");
					}
				}
				out.append("]");
			} else {
				out.append(")");
				if (indent != 0) {
					out.append("\"");
				}
			}
			outputTxt = out.toString();
		} else {
			StringBuilder out = new StringBuilder();
			if (!view && (!raw || value.toString().contains(" ") || value.toString().contains("\n")
					|| value.toString().contains("\r")))
				out.append("\"");
			String val = (!view && !raw ? value.toString().replace("\\", "\\\\").replace("\n", "\\n") : value.toString());
			if (!view && (indent == 0 || !raw || (raw && indent != 0))) {
				val = val.replace("\\\"", "\\\\\"");
				val = val.replace("\"", "\\\"");
			}
			out.append(val);
			if (!view && (!raw || value.toString().contains(" ") || value.toString().contains(" ")
					|| value.toString().contains("\n") || value.toString().contains("\r")))
				out.append("\"");
			outputTxt = out.toString();
		}
		return outputTxt;
	}

}
