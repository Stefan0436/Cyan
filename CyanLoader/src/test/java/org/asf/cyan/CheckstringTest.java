package org.asf.cyan;

import static org.junit.Assert.assertTrue;

import org.asf.cyan.api.versioning.Version;
import org.junit.Test;

public class CheckstringTest {
	@Test
	public void testAny() {
		assertTrue(CyanLoader.validateCheckString("*", Version.fromString("1.0.0.0")));
	}
	
	@Test
	public void testLess() {
		assertTrue(CyanLoader.validateCheckString("<1.0.0.1", Version.fromString("1.0.0.0")));
	}

	@Test
	public void testGeater() {
		assertTrue(CyanLoader.validateCheckString(">1", Version.fromString("1.0.0.0")));
	}

	@Test
	public void testGeaterOrEqual() {
		assertTrue(CyanLoader.validateCheckString(">= 1.0.0.0", Version.fromString("1.0.0.0")));
	}

	@Test
	public void testLessOrEqual() {
		assertTrue(CyanLoader.validateCheckString("<= 1.0.0.0", Version.fromString("1.0.0.0")));
	}

	@Test
	public void testAnd() {
		assertTrue(CyanLoader.validateCheckString("<= 1.0.0.0 & =~ 1\\.0\\..*", Version.fromString("1.0.0.0")));
	}

	@Test
	public void testOr() {
		assertTrue(CyanLoader.validateCheckString("<= 1.0.0.0 | 1.0.0", Version.fromString("1.0.0.0")));
		assertTrue(CyanLoader.validateCheckString("<= 1.0.0.0 | 1.0.0", Version.fromString("1.0.0")));
	}

	@Test
	public void testAndOr() {
		assertTrue(!CyanLoader.validateCheckString("> 1.0.0.0 | 1.0.0", Version.fromString("1.0.0.0")));
		assertTrue(CyanLoader.validateCheckString("> 1.0.0.0 | > 1.0.0 & < 2.0.0", Version.fromString("1.0.1")));
	}
}
