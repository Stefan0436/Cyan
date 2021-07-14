package org.asf.cyan.fluid.remapping;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

import org.asf.aos.util.service.extra.slib.util.ArrayUtil;

public class SimpleMappings extends Mapping<SimpleMappings> {

	private boolean allowSupertypeFinalOverride = false;

	public void setAllowSupertypeFinalOverride(boolean value) {
		allowSupertypeFinalOverride = value;
	}

	@Override
	public boolean allowSupertypeFinalOverride() {
		return allowSupertypeFinalOverride;
	}

	public SimpleMappings() {
	}

	/**
	 * Create a class mapping
	 */
	public SimpleMappings(String name, String obfuscated) {
		this.name = name;
		this.obfuscated = obfuscated;
		this.mappingType = MAPTYPE.CLASS;
	}

	/**
	 * Create a class mapping
	 */
	public SimpleMappings(String name, String obfuscated, Mapping<?>[] mappings) {
		this.name = name;
		this.obfuscated = obfuscated;
		this.mappingType = MAPTYPE.CLASS;
		this.mappings = mappings;
	}

	public SimpleMappings(Mapping<?>[] mappings) {
		this.mappings = mappings;
	}

	public SimpleMappings getClassMapping(String name) {
		for (Mapping<?> mapping : mappings) {
			if (mapping instanceof SimpleMappings && mapping.mappingType == MAPTYPE.CLASS
					&& mapping.name.equals(name)) {
				return (SimpleMappings) mapping;
			}
		}

		return null;
	}

	public String[] getObfuscatedClassNames() {		
		ArrayList<String> classes = new ArrayList<String>();
		
		for (Mapping<?> mapping : mappings) {
			if (mapping.mappingType == MAPTYPE.CLASS) {
				classes.add(mapping.obfuscated);
			}
		}

		return classes.toArray(t -> new String[t]);
	}

	public String[] getDeobfuscatedClassNames() {		
		ArrayList<String> classes = new ArrayList<String>();
		
		for (Mapping<?> mapping : mappings) {
			if (mapping.mappingType == MAPTYPE.CLASS) {
				classes.add(mapping.name);
			}
		}

		return classes.toArray(t -> new String[t]);
	}

	public String mapClassToObfuscation(String name) {
		for (Mapping<?> mapping : mappings) {
			if (mapping.mappingType == MAPTYPE.CLASS && mapping.name.equals(name)) {
				return mapping.obfuscated;
			}
		}

		return name;
	}

	public String mapClassToDeobfuscation(String obfus) {
		for (Mapping<?> mapping : mappings) {
			if (mapping.mappingType == MAPTYPE.CLASS && mapping.obfuscated.equals(obfus)) {
				return mapping.name;
			}
		}

		return obfus;
	}

	public SimpleMappings createClassMapping(String name, String obfuscated) {
		return createClassMapping(name, obfuscated, new Mapping<?>[0]);
	}

	public SimpleMappings createClassMapping(String name, String obfuscated, Mapping<?>[] childMappings) {
		SimpleMappings mappings = new SimpleMappings();
		mappings.name = name;
		mappings.obfuscated = obfuscated;
		mappings.mappingType = MAPTYPE.CLASS;
		this.mappings = ArrayUtil.append(this.mappings, new Mapping<?>[] { mappings });
		return mappings;
	}

	public void add(Mapping<?> map) {
		this.mappings = ArrayUtil.append(this.mappings, new Mapping<?>[] { map });
	}

	/**
	 * Create method mappings, can only be done with class mappings
	 */
	public SimpleMappings createMethod(String name, String obfuscated, String returnType, String... argumentTypes) {
		if (mappingType != MAPTYPE.CLASS) {
			throw new RuntimeException("Cannot add method to non-class mappings");
		}

		SimpleMappings mappings = new SimpleMappings();
		mappings.name = name;
		mappings.obfuscated = obfuscated;
		mappings.mappingType = MAPTYPE.METHOD;
		mappings.argumentTypes = argumentTypes;
		mappings.type = returnType;
		this.mappings = ArrayUtil.append(this.mappings, new Mapping<?>[] { mappings });
		return mappings;
	}

	/**
	 * Create field mappings, can only be done with class mappings
	 */
	public SimpleMappings createField(String name, String obfuscated, String type) {
		if (mappingType != MAPTYPE.CLASS) {
			throw new RuntimeException("Cannot add method to non-class mappings");
		}

		SimpleMappings mappings = new SimpleMappings();
		mappings.name = name;
		mappings.obfuscated = obfuscated;
		mappings.mappingType = MAPTYPE.PROPERTY;
		mappings.type = type;
		this.mappings = ArrayUtil.append(this.mappings, new Mapping<?>[] { mappings });
		return mappings;
	}

	/**
	 * Loads the given mappings file as SimpleMappings
	 * 
	 * @param path Mappings path to load
	 * @throws IOException If loading fails
	 */
	public SimpleMappings loadFile(String path) throws IOException {
		return loadFile(new File(path));
	}

	/**
	 * Loads the given mappings file as SimpleMappings
	 * 
	 * @param ccfg Mappings to load
	 * @throws IOException If loading fails
	 */
	public SimpleMappings loadFile(File ccfg) throws IOException {
		return readAll(new String(Files.readAllBytes(ccfg.toPath())));
	}
}
