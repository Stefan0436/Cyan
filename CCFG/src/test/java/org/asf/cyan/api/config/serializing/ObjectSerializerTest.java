package org.asf.cyan.api.config.serializing;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.asf.aos.util.service.extra.slib.util.ArrayUtil;
import org.asf.cyan.api.config.TestingConfig;
import org.junit.Test;

public class ObjectSerializerTest {
	Random rnd = new Random();

	@Test
	public void serializeInteger() throws Exception {
		int integer = rnd.nextInt();
		String serialized = ObjectSerializer.serialize(integer);
		assertTrue(ObjectSerializer.deserialize(serialized, int.class) == integer);
	}

	@Test
	public void serializeShort() throws Exception {
		short num = (short) rnd.nextInt(Short.MAX_VALUE);
		String serialized = ObjectSerializer.serialize(num);
		assertTrue(ObjectSerializer.deserialize(serialized, short.class) == num);
	}

	@Test
	public void tserializeLong() throws Exception {
		long num = rnd.nextLong();
		String serialized = ObjectSerializer.serialize(num);
		assertTrue(ObjectSerializer.deserialize(serialized, long.class) == num);
	}

	@Test
	public void serializeDouble() throws Exception {
		double num = rnd.nextDouble();
		String serialized = ObjectSerializer.serialize(num);
		assertTrue(ObjectSerializer.deserialize(serialized, double.class) == num);
	}

	@Test
	public void serializeFloat() throws Exception {
		float num = rnd.nextFloat();
		String serialized = ObjectSerializer.serialize(num);
		assertTrue(ObjectSerializer.deserialize(serialized, float.class) == num);
	}

	@Test
	public void serializeBoolean() throws Exception {
		boolean val = rnd.nextBoolean();
		String serialized = ObjectSerializer.serialize(val);
		assertTrue(ObjectSerializer.deserialize(serialized, boolean.class) == val);
	}

	@Test
	public void serializeBytes() throws Exception {
		byte[] val = new byte[Integer.MAX_VALUE / 5000];
		rnd.nextBytes(val);
		String serialized = ObjectSerializer.serialize(val);
		assertTrue(Arrays.equals(ObjectSerializer.deserialize(serialized, byte[].class), val));
	}

	String genText(int max) {
		int length = rnd.nextInt(max);
		while (length < 3)
			length = rnd.nextInt(max);
		String val = "";
		for (int i = 0; i < length; i++) {
			int chr = rnd.nextInt(127);
			while (chr < 32)
				chr = rnd.nextInt(127);
			val += (char) chr;
		}
		return val;
	}

	@Test
	public void serializeText() throws Exception {
		String val = genText(Integer.MAX_VALUE / 5000);
		String serialized = ObjectSerializer.serialize(val);
		String deserialized = ObjectSerializer.deserialize(serialized, String.class);
		if (!deserialized.equals(val)) {
			System.err.println("Failure:\n" + val + "\nshould be equal to:\n" + deserialized);
			int i = 0;
			for (char ch : val.toCharArray()) {
				char ch2 = deserialized.charAt(i++);
				if (ch != ch2) {
					System.err.println((int) ch2 + " was not " + (int) ch);
					break;
				}
			}
		}
		assertTrue(deserialized.equals(val));
	}

	// Serializing maps needs to be done with field reflection, else it doesn't work
	Map<String, String> testmp = new HashMap<String, String>();

	@Test
	public void serializeMap() throws IOException, NoSuchFieldException, SecurityException {
		int length = rnd.nextInt(1000);
		char[] allowed = ArrayUtil.castWrapperArrayToPrimitive(ArrayUtil.rangingNumeric('a', 'z', true, true),
				new char[0]);
		for (int i = 0; i < length; i++) {
			String key = "";
			int l = rnd.nextInt(100);
			while (l < 1)
				l = rnd.nextInt(100);
			for (int i2 = 0; i2 < l; i2++) {
				key += allowed[rnd.nextInt(allowed.length)];
			}
			testmp.put(key, genText(100));
		}

		String serializedmp = ObjectSerializer.serialize(testmp);
		Map<String, String> test2 = ObjectSerializer.deserialize(serializedmp, getClass().getDeclaredField("testmp"),
				this);

		assertTrue(test2.size() == testmp.size());
		for (String key : test2.keySet()) {
			if (!test2.get(key).equals(testmp.get(key)))
				System.err.println("Test failed: " + test2.get(key) + " should be equal to " + testmp.get(key));
			assertTrue(test2.get(key).equals(testmp.get(key)));
		}
	}

	@Test
	public void testCharacterEscaping() throws IOException {
		String test1 = "'\000\001hi\r\nhi\nhello\r''\\'test\\'\\r\\\\r\'test2\'\\rtest\ttabs\\";
		String out1 = ObjectSerializer.serialize(test1);
		System.out.println(out1);
		String out2 = ObjectSerializer.deserialize(out1, String.class);
		System.out.println(out2);

		String out3 = ObjectSerializer.toCCFGEntry("test", out1, String.class, new CCFGGetPropAction<String>() {

			@Override
			public String processPrefix(String key) {
				return null;
			}

			@Override
			public String processSuffix(String key) {
				return null;
			}

			@Override
			public Object getProp(String key) {
				return null;
			}

			@Override
			public String[] keys() {
				return null;
			}

			@Override
			public void error(IOException exception) {
			}

		}, true, false, false);

		CCFGStringPutAction getter = new CCFGStringPutAction();
		ObjectSerializer.parse(out3, getter);
		String out4 = getter.retrieveValue();

		assertTrue(out2.equals(test1));
		assertTrue(out4.equals(test1));
	}

	class CCFGStringPutAction extends CCFGPutPropAction {

		private String value = "";

		@Override
		public void run(String key, String txt) {
			try {
				value = ObjectSerializer.deserialize(txt, String.class);
			} catch (IOException e) {
			}
		}

		public String retrieveValue() {
			return value;
		}

	}

	@Test
	public void testDeserializeTestConfArray() throws IOException {
		String v1 = genText(500);
		String v2 = genText(300);
		int i1 = rnd.nextInt();
		int i2 = rnd.nextInt();
		String cfg = "{\n" + "    testStr> '" + ObjectSerializer.serialize(v1) + "'\n" + "} {\n" + "    testStr> '"
				+ ObjectSerializer.serialize(v2) + "'\n" + "} {\n" + "    test4> " + i1 + "\n" + "} {\n" + "    test4> "
				+ i2 + "\n" + "}";
		TestingConfig[] tests = ObjectSerializer.deserialize(cfg, TestingConfig[].class);
		if (!tests[0].testStr.equals(v1)) {
			System.err.println("The following value was incorrectly deserialized:\n" + tests[0].testStr + "\n" + v1);
		}

		assertTrue(tests[0].testStr.equals(v1));
		if (!tests[1].testStr.equals(v2)) {
			System.err.println("The following value was incorrectly deserialized:\n" + tests[1].testStr + "\n" + v2);
		}
		assertTrue(tests[1].testStr.equals(v2));
		assertTrue(tests[2].test4 == i1);
		assertTrue(tests[3].test4 == i2);
	}

}
