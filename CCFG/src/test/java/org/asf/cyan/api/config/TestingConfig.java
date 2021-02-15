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

	@Comment(value = "Testing", afterValue = true)
	@Comment("Testing parameter")
	public String testStr = "default";

	@OptionalEntry
	@Comment("This is an optional entry")
	@Comment("Second comment")
	public String optionalTest = "some optional value";
	
	@Comment("Another test, this time, sub-categories")
	public SubConfigTest test3 = new SubConfigTest();
	
	@Comment("Test integer")
	public int test4 = 0;
	
	public HashMap<String,String> testMap1 = new HashMap<String,String>();
	public HashMap<String,Integer> testMap2 = new HashMap<String,Integer>();
}
