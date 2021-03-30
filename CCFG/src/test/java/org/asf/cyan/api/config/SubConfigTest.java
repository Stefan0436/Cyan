package org.asf.cyan.api.config;

import org.asf.cyan.api.config.annotations.Comment;

public class SubConfigTest extends Configuration<SubConfigTest> {

	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
	}
	
	@Comment("test")
	public String testSubConfig = "HI";
}
