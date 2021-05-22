package org.asf.cyan;

import static org.junit.Assert.assertTrue;

import org.asf.cyan.api.util.CheckString;
import org.asf.cyan.api.versioning.Version;
import org.junit.Test;

public class CheckstringTest {
	@Test
	public void testAny() {
		assertTrue(CheckString.validateCheckString("*", Version.fromString("1.0.0.0")));
	}

	@Test
	public void testLess() {
		assertTrue(CheckString.validateCheckString("<1.0.0.1", Version.fromString("1.0.0.0")));
	}

	@Test
	public void testGeater() {
		assertTrue(CheckString.validateCheckString(">1", Version.fromString("1.0.0.0")));
	}

	@Test
	public void testGeaterOrEqual() {
		assertTrue(CheckString.validateCheckString(">= 1.0.0.0", Version.fromString("1.0.0.0")));
	}

	@Test
	public void testLessOrEqual() {
		assertTrue(CheckString.validateCheckString("<= 1.0.0.0", Version.fromString("1.0.0.0")));
	}

	@Test
	public void testAnd() {
		assertTrue(CheckString.validateCheckString("<= 1.0.0.0 & ~= 1\\.0\\..*", Version.fromString("1.0.0.0")));
	}

	@Test
	public void testOr() {
		assertTrue(CheckString.validateCheckString("<= 1.0.0.0 | 1.0.0", Version.fromString("1.0.0.0")));
		assertTrue(CheckString.validateCheckString("<= 1.0.0.0 | 1.0.0", Version.fromString("1.0.0")));
	}

	@Test
	public void testAndOr() {
		assertTrue(!CheckString.validateCheckString("> 1.0.0.0 | 1.0.0", Version.fromString("1.0.0.0")));
		assertTrue(CheckString.validateCheckString("> 1.0.0.0 | > 1.0.0 & < 2.0.0", Version.fromString("1.0.1")));
	}
}
