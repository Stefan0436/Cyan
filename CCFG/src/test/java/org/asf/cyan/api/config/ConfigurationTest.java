package org.asf.cyan.api.config;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.asf.aos.util.service.extra.slib.util.ArrayUtil;
import org.asf.cyan.api.config.gitmoduletest.GitConfiguration;
import org.asf.cyan.api.config.gitmoduletest.GitRepositoryConfig;
import org.asf.cyan.api.config.serializing.ObjectSerializer;
import org.junit.Test;

public class ConfigurationTest {
	TestingConfig test = new TestingConfig();
	public static Random rnd = new Random();

	public static String genText(int max) {
		int length = rnd.nextInt(max);
		while (length < 3)
			length = rnd.nextInt(max);
		String val = "";
		for (int i = 0; i < length; i++) {
			val += (char) rnd.nextInt(Character.MAX_VALUE);
		}
		return val;
	}

	@Test
	public void testWriteNew() throws IOException {
		TestingConfig.baseDir = "bin/test";
		TestingConfig test = new TestingConfig();
		if (test.exists())
			test.getFile().delete();

		test.writeAll();
		assertTrue(test.exists());
	}

	@Test
	public void recursiveMapTest() throws IOException {
		TestingConfig.baseDir = "bin/test";
		TestingConfig test = new TestingConfig();
		if (test.exists())
			test.getFile().delete();

		test.testMap3.put("test", new HashMap<String, String>(Map.of("one", "two")));

		test.writeAll();
		assertTrue(test.exists());

		test = new TestingConfig();
		test.readAll();

		assertTrue(test.testMap3.containsKey("test"));
		assertTrue(test.testMap3.get("test").get("one").equals("two"));
	}

	@Test
	public void mapInConfigTest() throws IOException {
		GitConfiguration.baseDir = "bin/test";
		GitConfiguration test = new GitConfiguration("bin/test");
		if (test.exists())
			test.getFile().delete();

		GitRepositoryConfig testSubConf = new GitRepositoryConfig();
		testSubConf.author = "ASF";
		testSubConf.accessGroup = "asf";
		test.repositories.put("test", testSubConf);

		test.writeAll();
		assertTrue(test.exists());

		test = new GitConfiguration("bin/test");
		test.readAll();

		assertTrue(test.repositories.containsKey("test"));
		assertTrue(test.repositories.get("test").author.equals("ASF"));
		assertTrue(test.repositories.get("test").accessGroup.equals("asf"));
	}

//
//	@Test
//	public void testWriteNewSetValue() throws IOException {
//		TestingConfig.baseDir = "bin/test";
//		TestingConfig test = new TestingConfig();
//		if (test.exists())
//			test.getFile().delete();
//		
//		test.writeAll();
//		
//		assertTrue(test.exists());
//		
//		test.defaultEmpty = "Hello";
//		test.writeAll();
//		
//		assertTrue(test.exists());
//		
//		test.defaultEmpty = "Hi";
//		test.writeAll();
//		
//		assertTrue(test.exists());
//		
//		test.defaultEmpty = "Hi";
//		test.writeAll();
//		
//		assertTrue(test.exists());
//	}

	@Test
	public void testReadAll() throws IOException {
		int length1 = rnd.nextInt(5000);
		while (length1 < 0)
			length1 = rnd.nextInt();
		int length2 = rnd.nextInt(5000);
		while (length2 < 0)
			length2 = rnd.nextInt();
		int length3 = rnd.nextInt(5000);
		while (length3 < 0)
			length3 = rnd.nextInt();

		Character[] allowed = ArrayUtil.rangingNumeric('b', 'z', true, true);
		String testVal1 = "";
		for (int i = 0; i < length1; i++) {
			testVal1 += allowed[rnd.nextInt(allowed.length)];
		}

		String testVal2 = "";
		for (int i = 0; i < length2; i++) {
			testVal2 += allowed[rnd.nextInt(allowed.length)];
		}

		String testVal3 = "";
		for (int i = 0; i < length3; i++) {
			testVal3 += allowed[rnd.nextInt(allowed.length)];
		}

		Map<String, String> testmp1 = new HashMap<String, String>();
		Map<String, Integer> testmp2 = new HashMap<String, Integer>();
		int lengthMap = rnd.nextInt(1000);
		char[] allowedMap = ArrayUtil.castWrapperArrayToPrimitive(ArrayUtil.rangingNumeric('a', 'z', true, true),
				new char[0]);
		for (int i = 0; i < lengthMap; i++) {
			String key = "";
			int l = rnd.nextInt(100);
			while (l < 1)
				l = rnd.nextInt(100);
			for (int i2 = 0; i2 < l; i2++) {
				key += allowedMap[rnd.nextInt(allowed.length)];
			}
			testmp1.put(key, genText(100));
			testmp2.put(key, rnd.nextInt());
		}

		String testmptxt1 = "{";
		for (String line : ObjectSerializer.serialize(testmp1).split(System.lineSeparator())) {
			testmptxt1 += "\n    " + line;
		}
		testmptxt1 += "\n}";
		String testmptxt2 = "{";
		for (String line : ObjectSerializer.serialize(testmp2).split(System.lineSeparator())) {
			testmptxt2 += "\n    " + line;
		}
		testmptxt2 += "\n}";

		String testConfig = "# test\n# test config\n\n" + "testStr> '" + testVal1 + "' # test\n" + "optionalTest> '"
				+ testVal2 + "'\n" + "test4> 4\n" + "\n" + "test3> {\n" + "    testSubConfig> '" + testVal3 + "'\n"
				+ "}\n" + "\n" + "testMap1> " + testmptxt1 + "\n" + "\n" + "testMap2> " + testmptxt2 + "\n";
		TestingConfig test = new TestingConfig().readAll(testConfig);
		for (String key : test.testMap1.keySet()) {
			if (!test.testMap1.get(key).equals(testmp1.get(key)))
				System.err
						.println("Test failed: " + test.testMap1.get(key) + " should be equal to " + testmp1.get(key));
			assertTrue(test.testMap1.get(key).equals(testmp1.get(key)));
		}
		for (String key : test.testMap2.keySet()) {
			if (!test.testMap2.get(key).equals(testmp2.get(key)))
				System.err
						.println("Test failed: " + test.testMap2.get(key) + " should be equal to " + testmp2.get(key));
			assertTrue(test.testMap2.get(key).equals(testmp2.get(key)));
		}

		assertTrue(test.test3.testSubConfig.equals(testVal3));
		assertTrue(test.testStr.equals(testVal1));
		assertTrue(test.optionalTest.equals(testVal2));
	}

	@Test
	public void testCCFGComments() throws NoSuchFieldException, SecurityException {
		String testVal = "Testing configuration file," + System.lineSeparator() + "multi-line comment test."
				+ System.lineSeparator() + System.lineSeparator() + "Our header";
		assertTrue(test.getConfigHeader().equals(testVal));
		assertTrue(test.getKeyHeaderComment(test.getClass().getField("testStr")).equals("Testing parameter"));
		assertTrue(test.getKeyFooterComment(test.getClass().getField("testStr")).equals("Testing"));
		assertTrue(test.getKeyHeaderComment(test.getClass().getField("optionalTest"))
				.equals("This is an optional entry" + System.lineSeparator() + "Second comment"));
	}
}
