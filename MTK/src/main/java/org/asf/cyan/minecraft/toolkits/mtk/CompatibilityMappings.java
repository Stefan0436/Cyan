package org.asf.cyan.minecraft.toolkits.mtk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.asf.aos.util.service.extra.slib.util.ArrayUtil;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.fluid.remapping.MAPTYPE;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.fluid.remapping.SimpleMappings;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;

class CompatibilityMappings extends SimpleMappings {
	protected ArrayList<String> ignoredTypes = new ArrayList<String>();
	private int build = 0;

	protected void setBuild(int build) {
		this.build = build;
	}

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

	protected boolean loadWhenPossible(String prefix, String suffix, MinecraftVersionInfo version, GameSide side)
			throws IOException {
		prefix = "compatibility-" + prefix;
		if (MinecraftMappingsToolkit.areMappingsAvailable(suffix, prefix, version, side)) {
			MinecraftToolkit.infoLog("Loading compatibility mappings...");
			File mappingsFile = MinecraftMappingsToolkit.getMappingsFile(suffix, prefix, version, side);
			readAll(Files.readString(mappingsFile.toPath()));
			try {
				int lastBuild = Integer.valueOf(this.mappingsVersion);
				if (lastBuild != build) {
					MinecraftToolkit.infoLog("Updating compatibility mappings...");
				}
				return build == lastBuild;
			} catch (NumberFormatException e) {
			}
		}
		return false;
	}

	protected void saveToDisk(String prefix, String suffix, MinecraftVersionInfo version, GameSide side)
			throws IOException {
		prefix = "compatibility-" + prefix;
		MinecraftToolkit.infoLog("Saving compatibility mappings...");
		File mappingsFile = MinecraftMappingsToolkit.getMappingsFile(suffix, prefix, version, side);
		mappingsVersion = Integer.toString(build);
		Files.write(mappingsFile.toPath(), toString().getBytes());
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

	public void combine(String identifier, Mapping<?> mappings, Mapping<?> combine, boolean alwaysAllowRemap)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		combine(identifier, mappings, combine, alwaysAllowRemap, false);
	}

	@SuppressWarnings("unchecked")
	public void combine(String identifier, Mapping<?> mappings, Mapping<?> combine, boolean alwaysAllowRemap,
			boolean silent) throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		if (!silent)
			MinecraftToolkit.infoLog("Computing compatibility mappings...");
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
					Mapping<?> mp = mapPropertyMapping(combine, mapping.obfuscated, m.obfuscated, true);
					if (!map.equals(m.obfuscated)) {
						Mapping<?> tmap = mapClassToMapping(m.type, t2 -> true, true);
						if (tmap != null)
							m.type = tmap.name;
						m.obfuscated = map;
					} else {
						remap = false;
					}
					if (remap || alwaysAllowRemap)
						if (!m.name.equals(m.obfuscated))
							newMembers.add(m);
						else if (mp != null) {
							m.obfuscated = mp.name;

							if (!m.name.equals(m.obfuscated))
								newMembers.add(m);
						}
				} else if (m.mappingType == MAPTYPE.METHOD) {
					Mapping<?> argMap = tMap.mappings[ind];
					boolean remap = true;
					String map = mapMethod(combine, mapping.obfuscated, m.obfuscated, true, argMap.argumentTypes);
					Mapping<?> mp = mapMethodMapping(combine, mapping.obfuscated, m.obfuscated, true,
							argMap.argumentTypes);
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
						Mapping<?> tmap = mapClassToMapping(m.type, t2 -> true, true);
						if (tmap != null)
							m.type = tmap.name;
						if (!m.name.equals(m.obfuscated))
							newMembers.add(m);
						else {
							if (mp != null)
								m.obfuscated = mp.name;

							if (!m.name.equals(m.obfuscated))
								newMembers.add(m);
						}
					}
					m.obfuscated = map;
				}
				ind++;
			}
			mapping.mappings = newMembers.toArray(t -> new Mapping<?>[t]);
		}
		this.mappings = mappingsLst.toArray(t -> new Mapping<?>[t]);
	}

	private InputStream getResourceStream(String resource) {
		Object[] o = getResourceData(resource);
		if (o == null)
			return null;
		else
			return (InputStream) o[0];
	}

	private Object[] getResourceData(String resource) {
		String base = getClass().getProtectionDomain().getCodeSource().getLocation().toString();
		if (base.toString().startsWith("jar:"))
			base = base.substring(0, base.lastIndexOf("!")) + "!";
		else if (base.endsWith("/" + getClass().getTypeName().replace(".", "/") + ".class")) {
			base = base.substring(0,
					base.length() - ("/" + getClass().getTypeName().replace(".", "/") + ".class").length());
		}
		if (base.endsWith(".jar") || base.endsWith(".zip"))
			base = "jar:" + base + "!";
		try {
			URL u = new URL(base + "/" + resource);
			return new Object[] { u.openStream(), u };
		} catch (IOException e) {
			return null;
		}
	}

//
//	private URL getResourceURL(String resource) {
//		Object[] o = getResourceData(resource);
//		if (o == null)
//			return null;
//		try {
//			((InputStream) o[0]).close();
//		} catch (IOException e) {
//		}
//		return (URL) o[1];
//	}
//
	public void applyInconsistencyMappings(MinecraftVersionInfo game, String loader, String loaderVersion)
			throws IOException {
		String majorVersion = game.getVersion();
		if (majorVersion.split(".").length >= 3) {
			majorVersion = majorVersion.substring(0, majorVersion.lastIndexOf("."));
		}

		Object[] strmd = getMappingsStrm(loader, game, loaderVersion, majorVersion, this::getResourceStream);
		if (strmd == null)
			strmd = getMappingsStrm(loader, game, loaderVersion, majorVersion,
					str -> Thread.currentThread().getContextClassLoader().getResourceAsStream(str));
		if (strmd == null)
			strmd = getMappingsStrm(loader, game, loaderVersion, majorVersion,
					str -> getClass().getResourceAsStream(str));
		if (strmd != null) {
			InputStream strm = (InputStream) strmd[0];

			LogManager.getLogger(CompatibilityMappings.class)
					.info("Applying inconsistency mappings patch: <mtk-jar>/" + strmd[1]);
			SimpleMappings mappings = new SimpleMappings().readAll(new String(strm.readAllBytes()));
			strm.close();
			applyMappingsPatch(this, mappings, true);
		}
	}

	private Object[] getMappingsStrm(String loader, MinecraftVersionInfo game, String loaderVersion,
			String majorVersion, Function<String, InputStream> streamSupplier) {
		String inconsistencyFile = "mappings/inconsistencies/inconsistencies-" + loader + "-" + game.getVersion() + "-"
				+ loaderVersion + ".ccfg";
		InputStream strm = getResourceStream(inconsistencyFile);
		if (strm == null) {
			inconsistencyFile = "mappings/inconsistencies/inconsistencies-" + loader + "-" + majorVersion + "-"
					+ loaderVersion + ".ccfg";
			strm = getResourceStream(inconsistencyFile);
		}
		if (strm == null) {
			inconsistencyFile = "mappings/inconsistencies/inconsistencies-" + loader + "-fallback-" + game.getVersion()
					+ ".ccfg";
			strm = getResourceStream(inconsistencyFile);
		}
		if (strm == null) {
			inconsistencyFile = "mappings/inconsistencies/inconsistencies-" + loader + "-fallback-" + majorVersion
					+ ".ccfg";
			strm = getResourceStream(inconsistencyFile);
		}
		if (strm != null)
			return new Object[] { strm, inconsistencyFile };
		else
			return null;
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

	public static Mapping<?> mapPropertyMapping(Mapping<?> mappings, String classPath, String propertyName,
			boolean obfuscated) {
		final String pName = propertyName;
		Mapping<?> map = mappings.mapClassToMapping(classPath, t -> Stream.of(t.mappings).anyMatch(
				t2 -> t2.mappingType == MAPTYPE.PROPERTY && (!obfuscated ? t2.name : t2.obfuscated).equals(pName)),
				false);
		if (map != null) {
			map = Stream.of(map.mappings).filter(
					t2 -> t2.mappingType == MAPTYPE.PROPERTY && (!obfuscated ? t2.name : t2.obfuscated).equals(pName))
					.findFirst().get();
			return map;
		}
		return null;
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

	static Mapping<?> mapMethodMapping(Mapping<?> mappings, String classPath, String methodName, boolean obfuscated,
			String... methodParameters) {
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
			return map;
		}
		return null;
	}
}
