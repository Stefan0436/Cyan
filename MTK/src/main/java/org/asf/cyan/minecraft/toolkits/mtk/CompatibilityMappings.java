package org.asf.cyan.minecraft.toolkits.mtk;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import org.asf.cyan.fluid.remapping.MAPTYPE;
import org.asf.cyan.fluid.remapping.Mapping;

class CompatibilityMappings extends VanillaMappings {
	protected Mapping<?> createMapping(String in, String out, MAPTYPE type, String returnType, String... argumentTypes)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		@SuppressWarnings("unchecked")
		Mapping<?> m = (Mapping<?>) Mapping.instanciateFromSerialzer(Mapping.class);
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
		combine(identifier, mappings, combine, false, true);
	}

	@SuppressWarnings("unchecked")
	public void combine(String identifier, Mapping<?> mappings, Mapping<?> combine, boolean alwaysAllowRemap, boolean warn)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		MinecraftToolkit.infoLog("Creating compatibility mappings...");
		ArrayList<Mapping<?>> mappingsLst = new ArrayList<Mapping<?>>();
		for (Mapping<?> mapping : mappings.mappings) {
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
			for (Mapping<?> m : mapping.mappings) {
				if (m.mappingType == MAPTYPE.PROPERTY) {
					m.obfuscated = mapProperty(combine, mapping.obfuscated, m.obfuscated, true);
				} else if (m.mappingType == MAPTYPE.METHOD) {
					Mapping<?> argMap = tMap.mappings[ind];
					boolean remap = alwaysAllowRemap;
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
						
						remap = true;
					}
					if (remap) {
						int i = 0;
						for (String type : m.argumentTypes) {
							Mapping<?> tmap = mapClassToMapping(type, t2 -> true, true);
							if (tmap != null) {
								m.argumentTypes[i++] = tmap.name;
							} else
								i++;
						}
					}
					m.obfuscated = map;

				}
				mapping.mappings[ind++] = m;
			}
		}
		if (!missingMappings.equals(BigInteger.ZERO) && warn) {
			StringBuilder builder = new StringBuilder();
			builder.append("Missing mappings for " + identifier + " support:").append(System.lineSeparator());
			for (String map : missingMappingsLst) {
				builder.append("Deobfuscated (Mojang): ").append(map).append(System.lineSeparator());
			}
			MinecraftToolkit.warnLog(builder.toString());
			MinecraftToolkit.warnLog("Missing " + missingMappings.toString()
					+ " mappings, this might be harmless depending on how many are missing, please note that some cyan features MIGHT not work.");
		}

		this.mappings = mappingsLst.toArray(t -> new Mapping<?>[t]);
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
