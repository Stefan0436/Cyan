package org.asf.cyan.fluid;

import java.util.stream.Stream;

import org.asf.cyan.fluid.remapping.MAPTYPE;
import org.asf.cyan.fluid.remapping.Mapping;

public class TestMappings extends Mapping<TestMappings> {
	public TestMappings create() {
		this.mappings = new Mapping[] {
			createMapping("org.asf.cyan.fluid.SomeTest", "SomeTest", MAPTYPE.CLASS),
			createMapping("org.asf.cyan.fluid.Hello", "aaa", MAPTYPE.CLASS, new TestMappings[] { 
				createMethodMapping("testOne", "a"),
				createMethodMapping("getBrandTest", "b"),
				createMethodMapping("getBrandTest", "b", String.class),
				createMethodMapping("testEqualsInSelf", "c", String.class),
				createFieldMapping("testText", "a", String.class)
			}),
			createMapping("org.asf.cyan.fluid.Goodbye", "aab", MAPTYPE.CLASS, new TestMappings[] { 
				createMethodMapping("testTwo", "b"),
				createMethodMapping("createHelloWithText", "b", String.class),
				createMethodMapping("createDescriptors", "b", new String[] { "java.lang.String[]" })
			}),
			createMapping("org.asf.cyan.fluid.Tester", "aac", MAPTYPE.CLASS, new TestMappings[] {
				createMethodMapping("testIt", "a", String.class, String.class),
				createMethodMapping("testEquals", "b", String.class, String.class),
				createMethodMapping("testPrivateEquals", "b", Integer.class, String.class)
			}),
			createMapping("org.asf.cyan.fluid.Tester$test", "aac$a1", MAPTYPE.CLASS)
		};
		return this;
	}
	
	public static TestMappings createMapping(String name, String obfuscated, MAPTYPE type, TestMappings... mappings) {
		TestMappings map = new TestMappings();
		map.name = name;
		map.mappingType = type;
		map.obfuscated = obfuscated;
		map.mappings = mappings;
		return map;
	}
	public static TestMappings createMethodMapping(String name, String obfuscated, Class<?>... types) {
		TestMappings map = new TestMappings();
		map.name = name;
		map.mappingType = MAPTYPE.METHOD;
		map.obfuscated = obfuscated;
		map.argumentTypes = Stream.of(types).map(t->t.getTypeName()).toArray(t->new String[t]);
		return map;
	}
	public static TestMappings createFieldMapping(String name, String obfuscated, Class<?> type) {
		TestMappings map = new TestMappings();
		map.name = name;
		map.mappingType = MAPTYPE.PROPERTY;
		map.obfuscated = obfuscated;
		map.type = type.getTypeName();
		return map;
	}
	public static TestMappings createMethodMapping(String name, String obfuscated, String[] types) {
		TestMappings map = new TestMappings();
		map.name = name;
		map.mappingType = MAPTYPE.METHOD;
		map.obfuscated = obfuscated;
		map.argumentTypes = types;
		return map;
	}
}
