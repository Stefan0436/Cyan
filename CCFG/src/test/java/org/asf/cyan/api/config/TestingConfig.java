package org.asf.cyan.api.config;

import java.util.HashMap;

import org.asf.cyan.api.config.annotations.Comment;
import org.asf.cyan.api.config.annotations.OptionalEntry;

@Comment({ "Testing configuration file,", "multi-line comment test.", "", "Our header" })
public class TestingConfig extends Configuration<TestingConfig> {

	@Override
	public String filename() {
		return "test";
	}

	@Override
	public String folder() {
		return "test1";
	}
	
	public String[] tests = new String[] {"test1", "test2"};
	public String[] tests2 = new String[] {};

	@Comment(value = "Testing", afterValue = true)
	@Comment("Testing parameter")
	public String testStr = "default";

	@Comment(value = "Testing", afterValue = true)
	@Comment("Testing parameter #2")
	public String testStr2 = "test\n - test";

	@OptionalEntry
	@Comment("This is an optional entry")
	@Comment("Second comment")
	public String optionalTest = "some optional value";
	
	@Comment("Another test, this time, sub-categories")
	public SubConfigTest test3 = new SubConfigTest();
	
	@Comment("Test integer")
	public int test4 = 0;
	
	@Comment("Should only be added if it has a value")
	public String defaultEmpty = null;
	
	public HashMap<String,String> testMap1 = new HashMap<String,String>();
	public HashMap<String,Integer> testMap2 = new HashMap<String,Integer>();
}
