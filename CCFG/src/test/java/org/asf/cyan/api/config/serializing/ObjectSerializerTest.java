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
			val += (char) rnd.nextInt(Character.MAX_VALUE);
		}
		return val;
	}

	@Test
	public void serializeText() throws Exception {
		String val = genText(Integer.MAX_VALUE / 5000);
		String serialized = ObjectSerializer.serialize(val);
		assertTrue(ObjectSerializer.deserialize(serialized, String.class).equals(val));
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
			assertTrue(test2.get(key).equals(testmp.get(key)));
		}
	}
	
	@Test
	public void testCharacterEscaping() throws IOException {
		String test1 = "hi\r\nhi\nhello\'test\'\\r\\\\r\'test2\'\\rtest";
		String out1 = ObjectSerializer.serialize(test1);
		System.out.println(out1);
		String out2 = ObjectSerializer.deserialize(out1, String.class);
		assertTrue(out2.equals(test1));
	}

	@Test
	public void testDeserializeTestConfArray() throws IOException {
		String v1 = genText(500);
		String v2 = genText(300);
		int i1 = rnd.nextInt();
		int i2 = rnd.nextInt();
		String cfg = "{\n" + "    testStr> '" + v1 + "'\n" + "} {\n" + "    testStr> '" + v2 + "'\n" + "} {\n"
				+ "    test4> " + i1 + "\n" + "} {\n" + "    test4> " + i2 + "\n" + "}";
		TestingConfig[] tests = ObjectSerializer.deserialize(cfg, TestingConfig[].class);
		assertTrue(tests[0].testStr.equals(v1));
		assertTrue(tests[1].testStr.equals(v2));
		assertTrue(tests[2].test4 == i1);
		assertTrue(tests[3].test4 == i2);
	}

}
