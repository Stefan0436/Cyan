package org.asf.cyan.api.versioning;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VersionTest {

	@Test
	public void fromStringTest() {
		Version ver = Version.fromString("1.0.0.0");
		Version ver2 = Version.fromString("BETA-1.0.0.B2");
		Version ver3 = Version.fromString("1.0.0.A3");
		Version ver4 = Version.fromString("1.0.0.AA3");
		Version ver5 = Version.fromString("1.0.0.0-SNAPSHOT-1");

		assertTrue(ver.toString().equals("1.0.0.0"));
		assertTrue(ver2.toString().equals("BETA-1.0.0.B2"));
		assertTrue(ver3.toString().equals("1.0.0.A3"));
		assertTrue(ver4.toString().equals("1.0.0.AA3"));
		assertTrue(ver5.toString().equals("1.0.0.0-SNAPSHOT-1"));
	}

	@Test
	public void greaterThanTest() {
		Version ver = Version.fromString("1.0.0.0");
		Version ver2 = Version.fromString("BETA-1.0.0.B2");
		Version ver3 = Version.fromString("1.0.0.A3");
		Version ver4 = Version.fromString("1.0.0.AA3"); // matches ver3 because the second letter is discarted
		Version ver5 = Version.fromString("1.0.0.0-SNAPSHOT-1");
		Version ver6 = Version.fromString("1.0.0.2");
		Version ver7 = Version.fromString("BETA-1.0.0.B3");
		Version ver8 = Version.fromString("1.15.2");
		Version ver9 = Version.fromString("1.16.5");
		
		Version ver10 = Version.fromString("5.1.4");
		Version ver11 = Version.fromString("5.0");

		assertTrue(ver10.isGreaterThan(ver11));
		assertTrue(ver9.isGreaterThan(ver8));
		assertTrue(ver5.isGreaterThan(ver));
		assertTrue(ver6.isGreaterThan(ver5));
		assertTrue(ver2.isGreaterThan(ver));
		assertTrue(!ver3.isLessThan(ver7));
		assertTrue(ver.isLessThan(ver6));
		assertTrue(!ver4.isGreaterThan(ver3));
	}

	@Test
	public void lessThanTest() {
		Version ver = Version.fromString("1.0.0.0");
		Version ver2 = Version.fromString("BETA-1.0.0.B2");
		Version ver3 = Version.fromString("1.0.0.A3");
		Version ver5 = Version.fromString("1.0.0.0-SNAPSHOT-1");
		Version ver6 = Version.fromString("1.0.0.2");
		Version ver7 = Version.fromString("BETA-1.0.0.B3");
		Version ver8 = Version.fromString("1.15.2");
		Version ver9 = Version.fromString("1.16.5");
		Version ver10 = Version.fromString("1.0.0.B1");

		Version ver11 = Version.fromString("5.1.4");
		Version ver12 = Version.fromString("5.0");

		assertTrue(ver12.isLessThan(ver11));
		assertTrue(ver8.isLessThan(ver9));
		assertTrue(ver.isLessThan(ver5));
		assertTrue(ver5.isLessThan(ver6));
		assertTrue(ver2.isLessThan(ver7));
		assertTrue(!ver3.isLessThan(ver10));
		assertTrue(ver.isLessThan(ver6));
	}
}
