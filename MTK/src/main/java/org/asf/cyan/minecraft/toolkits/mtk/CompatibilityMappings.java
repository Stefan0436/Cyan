package org.asf.cyan.minecraft.toolkits.mtk;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.asf.aos.util.service.extra.slib.util.ArrayUtil;
import org.asf.cyan.fluid.remapping.MAPTYPE;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.fluid.remapping.SimpleMappings;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;

class CompatibilityMappings extends SimpleMappings {
	protected ArrayList<String> ignoredTypes = new ArrayList<String>();

	protected Mapping<?> createMapping(String in, String out, MAPTYPE type, String returnType, String... argumentTypes)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		@SuppressWarnings("unchecked")
		Mapping<?> m = (Mapping<?>) Mapping.instantiateFromSerializer(Mapping.class);
		m.name = in;
		m.obfuscated = out;
		m.mappingType = type;
		m.type = returnType;
		m.argumentTypes = argumentTypes;
		return m;
	}

	protected Mapping<?> setMappings(Mapping<?> input, Mapping<?>... add) {
		input.mappings = add;
		return input;
	}

	public void combine(String identifier, Mapping<?> mappings, Mapping<?> combine)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		combine(identifier, mappings, combine, false);
	}

	@SuppressWarnings("unchecked")
	public void combine(String identifier, Mapping<?> mappings, Mapping<?> combine, boolean alwaysAllowRemap)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		MinecraftToolkit.infoLog("Creating compatibility mappings...");
		ArrayList<Mapping<?>> mappingsLst = new ArrayList<Mapping<?>>();
		for (Mapping<?> mapping : mappings.mappings) {
			if (ignoredTypes.contains(mapping.name))
				continue;
			Mapping<?> newMapping = mapping;
			Mapping<?> tmp = combine.mapClassToMapping(newMapping.obfuscated, t -> true, true);
			if (tmp != null)
				newMapping.obfuscated = tmp.name;
			mappingsLst.add(newMapping);
		}

		this.mappings = mappingsLst.toArray(t -> new Mapping<?>[t]);
		ArrayList<Mapping<?>> tempMappings = (ArrayList<Mapping<?>>) mappingsLst.clone();
		for (Mapping<?> m : tempMappings) {
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
							Mapping<?> map = mapClassToMapping(type, t2 -> true, false);
							if (map != null)
								types[ind++] = map.obfuscated + tSuffix;
							else
								ind++;
						}
						t.argumentTypes = types;
					}
					Mapping<?> map2 = mapClassToMapping(t.type, t2 -> true, false);
					if (map2 != null)
						t.type = map2.obfuscated;
				}
			}
		}

		BigInteger missingMappings = BigInteger.ZERO;
		ArrayList<String> missingMappingsLst = new ArrayList<String>();
		int index = 0;
		for (Mapping<?> mapping : mappingsLst) {
			Mapping<?> tMap = tempMappings.get(index++);
			int ind = 0;
			Mapping<?>[] members = mapping.mappings.clone();
			ArrayList<Mapping<?>> newMembers = new ArrayList<Mapping<?>>();
			for (Mapping<?> m : members) {
				if (m.mappingType == MAPTYPE.PROPERTY) {
					boolean remap = true;
					String map = mapProperty(combine, mapping.obfuscated, m.obfuscated, true);
					if (!map.equals(m.obfuscated)) {
						m.obfuscated = map;
					} else {
						remap = false;
					}
					if (remap || alwaysAllowRemap)
						newMembers.add(m);
				} else if (m.mappingType == MAPTYPE.METHOD) {
					Mapping<?> argMap = tMap.mappings[ind];
					boolean remap = true;
					String map = mapMethod(combine, mapping.obfuscated, m.obfuscated, true, argMap.argumentTypes);
					if (map.equals(m.obfuscated) && !map.equals("<init>") && !map.equals("<clinit>")
							&& !argMap.name.equals(argMap.obfuscated)) {
						String typeStr = "";
						for (String t : m.argumentTypes) {
							if (typeStr.equals("")) {
								typeStr = t;
							} else
								typeStr += ", " + t;
						}

						missingMappingsLst.add(m.type + " " + mapping.name + "." + m.name + " (" + typeStr
								+ "), obfuscated: " + mapping.obfuscated + "." + map);
						missingMappings = missingMappings.add(BigInteger.ONE);

						remap = false;
					}
					if (remap || alwaysAllowRemap) {
						int i = 0;
						for (String type : m.argumentTypes) {
							Mapping<?> tmap = mapClassToMapping(type, t2 -> true, true);
							if (tmap != null) {
								m.argumentTypes[i++] = tmap.name;
							} else
								i++;
						}
						newMembers.add(m);
					}
					m.obfuscated = map;
				}
				mapping.mappings = newMembers.toArray(t -> new Mapping<?>[t]);
				mapping.mappings[ind++] = m;
			}
		}
		this.mappings = mappingsLst.toArray(t -> new Mapping<?>[t]);
	}

	public void applyInconsistencyMappings(MinecraftVersionInfo game, String loader, String loaderVersion)
			throws IOException {
		String majorVersion = game.getVersion();
		if (majorVersion.split(".").length >= 3) {
			majorVersion = majorVersion.substring(0, majorVersion.lastIndexOf("."));
		}

		String inconsistencyFile = "mappings/inconsistencies/inconsistencies-" + loader + "-" + game.getVersion() + "-"
				+ loaderVersion + ".ccfg";
		InputStream strm = Thread.currentThread().getContextClassLoader().getResourceAsStream(inconsistencyFile);
		if (strm == null) {
			inconsistencyFile = "mappings/inconsistencies/inconsistencies-" + loader + "-" + majorVersion + "-"
					+ loaderVersion + ".ccfg";
			strm = Thread.currentThread().getContextClassLoader().getResourceAsStream(inconsistencyFile);
		}
		if (strm == null) {
			inconsistencyFile = "mappings/inconsistencies/inconsistencies-" + loader + "-fallback-" + game.getVersion()
					+ ".ccfg";
			strm = Thread.currentThread().getContextClassLoader().getResourceAsStream(inconsistencyFile);
		}
		if (strm == null) {
			inconsistencyFile = "mappings/inconsistencies/inconsistencies-" + loader + "-fallback-" + majorVersion
					+ ".ccfg";
			strm = Thread.currentThread().getContextClassLoader().getResourceAsStream(inconsistencyFile);
		}
		if (strm != null) {
			LogManager.getLogger(CompatibilityMappings.class)
					.info("Applying inconsistency mappings patch: <mtk-jar>/" + inconsistencyFile);
			SimpleMappings mappings = new SimpleMappings().readAll(new String(strm.readAllBytes()));
			strm.close();
			applyMappingsPatch(this, mappings, true);
		}
	}

	protected static void applyMappingsPatch(Mapping<?> target, Mapping<?> patchMappings, boolean overwrite) {
		for (Mapping<?> map : patchMappings.mappings) {
			if (map.mappingType == MAPTYPE.CLASS) {
				Mapping<?> patch = map;
				for (Mapping<?> ex : target.mappings) {
					if (ex.mappingType == MAPTYPE.CLASS
							&& (ex.name.equals(map.name) || ex.obfuscated.equals(map.obfuscated))) {
						if (overwrite) {
							ex.name = map.name;
							ex.obfuscated = map.obfuscated;
						}
						map = ex;
						break;
					}
				}
				if (map == patch) {
					target.mappings = ArrayUtil.append(target.mappings, new Mapping[] { map });
					continue;
				}

				for (Mapping<?> member : patch.mappings) {
					if (member.mappingType == MAPTYPE.METHOD) {
						Mapping<?> ex = null;
						for (Mapping<?> ex2 : map.mappings) {
							if (ex2.mappingType == member.mappingType
									&& (ex2.name.equals(member.name) || ex2.obfuscated.equals(member.obfuscated))
									&& Arrays.equals(member.argumentTypes, ex2.argumentTypes)) {
								ex = ex2;
								break;
							}
						}
						if (ex == null) {
							map.mappings = ArrayUtil.append(map.mappings, new Mapping[] { member });
							continue;
						}
						if (!overwrite)
							continue;
						ex.name = member.name;
						ex.obfuscated = member.obfuscated;
						ex.argumentTypes = member.argumentTypes;
						ex.type = member.type;
					} else if (member.mappingType == MAPTYPE.PROPERTY) {
						Mapping<?> ex = null;
						for (Mapping<?> ex2 : map.mappings) {
							if (ex2.mappingType == member.mappingType
									&& (ex2.name.equals(member.name) || ex2.obfuscated.equals(member.obfuscated))) {
								ex = ex2;
								break;
							}
						}
						if (ex == null) {
							map.mappings = ArrayUtil.append(map.mappings, new Mapping[] { member });
							continue;
						}
						if (!overwrite)
							continue;
						ex.name = member.name;
						ex.obfuscated = member.obfuscated;
						ex.type = member.type;
					}
				}
			}
		}
	}

	// Forked methods from FLUID
	public static String mapProperty(Mapping<?> mappings, String classPath, String propertyName, boolean obfuscated) {
		final String pName = propertyName;
		Mapping<?> map = mappings.mapClassToMapping(classPath, t -> Stream.of(t.mappings).anyMatch(
				t2 -> t2.mappingType == MAPTYPE.PROPERTY && (!obfuscated ? t2.name : t2.obfuscated).equals(pName)),
				false);
		if (map != null) {
			map = Stream.of(map.mappings).filter(
					t2 -> t2.mappingType == MAPTYPE.PROPERTY && (!obfuscated ? t2.name : t2.obfuscated).equals(pName))
					.findFirst().get();
			if (!obfuscated)
				propertyName = map.obfuscated;
			else
				propertyName = map.name;
		}
		return propertyName;
	}

	static String mapMethod(Mapping<?> mappings, String classPath, String methodName, boolean obfuscated,
			String... methodParameters) {
		return mapMethod(mappings, classPath, methodName, obfuscated, false, methodParameters);
	}

	static String mapMethod(Mapping<?> mappings, String classPath, String methodName, boolean obfuscated,
			boolean getPath, String... methodParameters) {
		final String mName = methodName;
		Mapping<?> map = mappings.mapClassToMapping(classPath,
				t -> Stream.of(t.mappings)
						.anyMatch(t2 -> t2.mappingType.equals(MAPTYPE.METHOD)
								&& (!obfuscated ? t2.name : t2.obfuscated).equals(mName)
								&& Arrays.equals(t2.argumentTypes, methodParameters)),
				false);
		if (map != null) {
			classPath = map.obfuscated;
			map = Stream.of(map.mappings)
					.filter(t2 -> t2.mappingType == MAPTYPE.METHOD
							&& (!obfuscated ? t2.name : t2.obfuscated).equals(mName)
							&& Arrays.equals(t2.argumentTypes, methodParameters))
					.findFirst().get();
			if (!obfuscated)
				methodName = map.obfuscated;
			else
				methodName = map.name;
		}
		if (getPath)
			return classPath + "." + methodName;
		else
			return methodName;
	}
}
