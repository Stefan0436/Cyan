package org.asf.cyan.fluid.bytecode.enums;

import org.asf.cyan.fluid.Fluid;

public enum OpcodeUseCase {	
	SOURCE("^(SOURCE_|V).*$"),
	ACC_FIELD("ACC_VOLATILE|ACC_TRANSIENT"),
	ACC_MODULE("ACC_OPEN|ACC_MODULE"),
	ACC_MODULE_REQ("ACC_TRANSITIVE|ACC_STATIC_PHASE"),
	ACCESS_CODE("^ACC_.*$"),
	ASM_STACKMAP("^F_.*$"),
	ASM_VERSION("^ASM.*$"),
	NEW_ARRAY("^T_.*$"),
	STACK_FRAME("@TYPE:Ljava/lang/Integer;^.*$"),
	INVOKE_METH("^H_.*$"),
	JVM_OPCODE("^.*$");

	public final String type;
	public final String value;

	private OpcodeUseCase(String value) {
		this.value = value;
		String type = "int";

		if (value.startsWith("@TYPE:")) {
			value = value.substring(6);
			String desc = "";

			for (int i = 0; i < value.length(); i++) {
				char ch = value.charAt(i);
				if (ch != 'L' && !desc.startsWith("L")) {
					desc += ch;
					break;
				} else if (ch != ';') {
					desc += ch;
				} else {
					desc += ch;
					break;
				}
			}

			value = value.substring(desc.length());
			type = Fluid.parseDescriptor(desc);
		}
		this.type = type;
	}

	public static OpcodeUseCase valueOf(String name, String type) {
		for (OpcodeUseCase value : values()) {
			if (value.toString().equals("JVM_OPCODE"))
				continue;

			if (value.type.equals(type) && name.matches(value.value)) {
				return value;
			}
		}
		for (OpcodeUseCase value : values()) {
			if (value.type.equals(type) && name.matches(value.value)) {
				return value;
			}
		}
		return null;
	}
}
