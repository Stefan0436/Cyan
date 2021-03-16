package org.asf.cyan.fluid.deobfuscation;

import java.io.Serializable;
import java.util.HashMap;

public class DeobfuscationTarget implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public String jvmName = "";
	public String outputName = "";
	public HashMap<String, String> fields = new HashMap<String, String>();
	public HashMap<String, String> methods = new HashMap<String, String>();
}
