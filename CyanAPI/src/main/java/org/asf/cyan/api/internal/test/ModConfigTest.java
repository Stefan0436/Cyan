package org.asf.cyan.api.internal.test;

import java.io.IOException;

import org.asf.cyan.api.config.ModConfiguration;

public class ModConfigTest extends ModConfiguration<ModConfigTest, TestEventListeners> {

	public ModConfigTest(TestEventListeners instance) throws IOException {
		super(instance);
	}
	
	public String test = "hello";

}
