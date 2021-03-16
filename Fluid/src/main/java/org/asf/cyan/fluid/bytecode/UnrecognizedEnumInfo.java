package org.asf.cyan.fluid.bytecode;

public class UnrecognizedEnumInfo {
	private String type;
	private String name;
	
	public UnrecognizedEnumInfo(String cls, String value) {
		this.type = cls;
		this.name = value;
	}
	
	public String getType() {
		return type;
	}
	
	public String getValue() {
		return name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
