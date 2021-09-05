package org.example.mods.examplemod.config;

import java.io.IOException;

import modkit.config.ModConfiguration;

import org.asf.cyan.api.config.annotations.Comment;
import org.example.mods.examplemod.ExampleMod;

public class ExampleModConfig extends ModConfiguration<ExampleModConfig, ExampleMod> {

	public ExampleModConfig(ExampleMod instance) throws IOException {
		super(instance);
	}

	@Override // Optional override, default uses mod.ccfg
	public String filename() {
		return "example.ccfg";
	}
	
	@Comment("A comment")
	public String exampleNode1 = "Default value";

	@Comment("Example numeric config node")
	public int exampleNumber = 2;

}
