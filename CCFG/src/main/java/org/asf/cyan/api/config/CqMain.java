package org.asf.cyan.api.config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.asf.cyan.api.config.serializing.internal.Splitter;

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
			System.err.println("Usage: cq <config class> <input> [options] [entry path]");
			System.err.println("Options:");
			System.err.println("    --raw                   - output bash");
			System.err.println("    --output <output file>  - sets output");
			System.err.println("    --source-jar <jar path> - adds source jars for the config class");
			System.err.println("");
			System.err.println("Entry path format examples:");
			System.err.println("[key], [key].[subkey], [key].[array-index] or [key].[mapkey]");
			System.exit(-1);
		} else {
			ArrayList<URL> urls = new ArrayList<URL>();
			String path = "";
			boolean raw = false;
			String output = "-";

			boolean noparse = false;
			boolean option = false;
			String key = "";
			for (int i = 2; i < args.length; i++) {
				String arg = args[i];
				if (!noparse) {
					if (!option) {
						if (arg.startsWith("--") && !arg.equals("--")) {
							key = arg.substring(2);
							if (!key.equals("raw")) {
								option = true;
								continue;
							} else {
								if (key.equals("raw")) {
									raw = true;
									continue;
								}
							}
						} else if (arg.equals("--")) {
							noparse = true;
						}
					} else {
						if (key.equals("output")) {
							output = arg;
						} else if (key.equals("source-jar")) {
							urls.add(new File(arg).toURI().toURL());
						}
						continue;
					}
				}

				if (!path.isEmpty())
					path += " ";
				path += arg;
			}

			String cls = args[0];
			String input = args[1];
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

			Configuration<?> ccfg = Configuration
					.instanciateFromSerialzer((Class<? extends Configuration>) Class.forName(cls, true, clloader));
			ccfg = ccfg.readAll(input);

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

			int index = 0;

			String entryPrefix = "";
			String[] pathEntries = Splitter.split(path, '.');
			if (getSelf) {
				pathEntries = new String[] { "" };
			}
			for (String pathEntry : pathEntries) {
				if (pathEntry.endsWith("\\")) {
					entryPrefix += pathEntry.substring(0, pathEntry.length() - 1) + ".";
					break;
				}

				pathEntry = entryPrefix + pathEntry;
				entryPrefix = "";

				if (index + 1 == pathEntries.length) {
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
					String outputTxt = serialize(value, raw, 0);
					if (output.equals("-")) {
						if (!raw) {
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static String serialize(Object value, boolean raw, int indent) {
		String outputTxt = "";
		if (value instanceof Configuration<?>) {
			CCFGConfigGenerator serializer = new CCFGConfigGenerator((Configuration) value, true);
			StringBuilder out = new StringBuilder();
			if (!raw) {
				out.append("{");
			} else {
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
					out.append("[");
				}
				out.append(serialize(k, raw, indent + 2));
				if (!raw) {
					out.append(": ");
				} else {
					out.append("]=");
				}
				out.append(serialize(v, raw, indent + 2));

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
			}

			outputTxt = out.toString();
		} else if (value instanceof Map<?, ?>) {
			StringBuilder out = new StringBuilder();
			if (!raw) {
				out.append("{");
			} else {
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
					out.append("[");
				}
				out.append(serialize(k, raw, indent + 2));
				if (!raw) {
					out.append(": ");
				} else {
					out.append("]=");
				}
				out.append(serialize(v, raw, indent + 2));

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
			}

			outputTxt = out.toString();
		} else if (value.getClass().isArray()) {
			StringBuilder out = new StringBuilder();
			if (!raw) {
				out.append("[");
			} else {
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

				out.append(serialize(Array.get(value, i), raw, indent + 2));
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
			}
			outputTxt = out.toString();
		} else {
			StringBuilder out = new StringBuilder();
			if (!raw || value.toString().contains(" "))
				out.append("\"");
			out.append(value.toString());
			if (!raw || value.toString().contains(" "))
				out.append("\"");
			outputTxt = out.toString();
		}
		return outputTxt;
	}

}
