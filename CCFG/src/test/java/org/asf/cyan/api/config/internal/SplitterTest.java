package org.asf.cyan.api.config.internal;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.asf.cyan.api.config.ConfigurationTest;
import org.asf.cyan.api.config.serializing.internal.Splitter;
import org.junit.Test;

public class SplitterTest {
	String teststr1 = "";
	ArrayList<String> testarr1 = new ArrayList<String>();
	String teststr2 = "";
	ArrayList<String> testarr2 = new ArrayList<String>();
	public SplitterTest() {
		System.out.println("Generate 1");
		int length = ConfigurationTest.rnd.nextInt(10000);
		for (int i = 0 ; i < length ; i++) {
			String testEntry = ConfigurationTest.genText(100);
			while (testEntry.contains("\r\n")) testEntry = ConfigurationTest.genText(100);
			if (teststr1.equals("")) teststr1 = testEntry;
			else teststr1 += "\r\n"+testEntry;
			testarr1.add(testEntry);
		}
		System.out.println("Generate 2");
		length = ConfigurationTest.rnd.nextInt(10000);
		for (int i = 0 ; i < length ; i++) {
			String testEntry = ConfigurationTest.genText(100);
			while (testEntry.contains("\n")) testEntry = ConfigurationTest.genText(100);
			if (teststr2.equals("")) teststr2 = testEntry;
			else teststr2 += "\n"+testEntry;
			testarr2.add(testEntry);
		}
	}

	@Test
	public void splitCharDelimTest() {		
		System.out.println("Start");
		String[] output = Splitter.split(teststr2, '\n');
		int l1 = testarr2.size();
		int l2 = output.length;
		assertTrue(l1 == l2);
		System.out.println(l1);
		int ind = 0;
		String[] s = testarr2.toArray(t->new String[t]);
		for (String str : s) {
			assertTrue(str.equals(output[ind++]));
		}
	}
	@Test
	public void splitStringDelimTest() {
		System.out.println("Start");
		String[] output = Splitter.split(teststr1, "\r\n");
		int l1 = testarr1.size();
		int l2 = output.length;
		System.out.println(l1);
		assertTrue(l1 == l2);
		int ind = 0;
		String[] s = testarr1.toArray(t->new String[t]);
		for (String str : s) {
			assertTrue(str.equals(output[ind++]));
		}
	}
}
