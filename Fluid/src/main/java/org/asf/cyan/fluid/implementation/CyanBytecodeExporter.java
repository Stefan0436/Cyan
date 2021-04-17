package org.asf.cyan.fluid.implementation;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.config.serializing.internal.Splitter;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.Transformer.AnnotationInfo;
import org.asf.cyan.fluid.Transformer.FluidMethodInfo;
import org.asf.cyan.fluid.bytecode.BytecodeExporter;
import org.asf.cyan.fluid.bytecode.UnrecognizedEnumInfo;
import org.asf.cyan.fluid.bytecode.enums.OpcodeUseCase;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableAnnotationNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

/**
 * 
 * Cyan implementation of the FLUID PseudoCode Bytecode Exporting System, based
 * on ASM.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@CYAN_COMPONENT
public class CyanBytecodeExporter extends BytecodeExporter {
	private HashMap<OpcodeUseCase, HashMap<Object, String>> opcodes = new HashMap<OpcodeUseCase, HashMap<Object, String>>();

	protected static void initComponent() {
		BytecodeExporter.setImplementation(new CyanBytecodeExporter());
	}

	protected CyanBytecodeExporter() {
		for (Field field : Opcodes.class.getFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				field.setAccessible(true);
				try {
					Object value = field.get(null);
					OpcodeUseCase useCase = OpcodeUseCase.valueOf(field.getName().toUpperCase(),
							field.getType().getTypeName());
					HashMap<Object, String> mp = opcodes.getOrDefault(useCase, new HashMap<Object, String>());
					mp.put(value, field.getName().toUpperCase());
					opcodes.put(useCase, mp);
				} catch (IllegalArgumentException | IllegalAccessException e) {
				}
			}
		}
	}

	@Override
	protected String getImplementationName() {
		return "Cyan";
	}

	@Override
	protected String getOpcodeName(Object opcode, OpcodeUseCase useCase) {
		return opcodes.getOrDefault(useCase, new HashMap<Object, String>()).get(opcode);
	}

	@Override
	protected String insnNodeToString(AbstractInsnNode insn, int index) {
		String output = "";
		String type = "";
		String value = "";

		switch (insn.getClass().getSimpleName()) {
		case "LabelNode":
			output += "Label label" + (index + 1) + " = {";
			break;
		case "LineNumberNode":
			type = "LineNode";
			value = "line: " + ((LineNumberNode) insn).line;
			break;
		case "VarInsnNode":
			type = "VariableNode";
			value = "var: " + "var" + ((VarInsnNode) insn).var;

			String opcode = getOpcodeName(insn.getOpcode(), OpcodeUseCase.JVM_OPCODE);
			if (opcode.contains("ALOAD")) {
				value += ", method: load";
			} else if (opcode.contains("ASTORE")) {
				value += ", method: store";
			}
			break;
		case "LdcInsnNode":
			type = "LdcNode";
			Object ldcValue = ((LdcInsnNode) insn).cst;
			String valueStr = "";
			String typeStr = "";
			if (ldcValue instanceof String) {
				valueStr = "\"" + ldcValue.toString() + "\"";
				typeStr = "string";
			} else if (ldcValue instanceof Type) {
				Type ldcType = (Type) ldcValue;
				typeStr = "classref";
				valueStr = "(classof: \"" + ldcType.getClassName() + "\")";
			} else {
				valueStr = ldcValue.toString();
				typeStr = ldcValue.getClass().getSimpleName().toLowerCase();
			}
			value = "value: " + valueStr + ", type: " + typeStr;
			break;
		case "TypeInsnNode":
			type = "TypeNode";
			value = "type: \"" + ((TypeInsnNode) insn).desc + "\"";
			break;
		case "MethodInsnNode":
			type = "MethodNode";
			value = "owner: \"" + ((MethodInsnNode) insn).owner + "\", name: \"" + ((MethodInsnNode) insn).name
					+ "\", desc: \"" + ((MethodInsnNode) insn).desc + "\"";
			break;
		case "FieldInsnNode":
			type = "FieldNode";
			value = "owner: \"" + ((FieldInsnNode) insn).owner + "\", name: \"" + ((FieldInsnNode) insn).name
					+ "\", type: \"" + Fluid.parseDescriptor(((FieldInsnNode) insn).desc) + "\"";

			value += ", method: " + getOpcodeName(insn.getOpcode(), OpcodeUseCase.JVM_OPCODE).toLowerCase();
			break;
		case "JumpInsnNode":
			type = "JumpNode";
			int labelIndex = 1;
			AbstractInsnNode tmpNode = ((JumpInsnNode) insn).label.getPrevious();
			while (tmpNode != null) {
				if (tmpNode instanceof LabelNode)
					labelIndex++;
				tmpNode = tmpNode.getPrevious();
			}
			value = "target: label" + labelIndex + ", code: "
					+ getOpcodeName(insn.getOpcode(), OpcodeUseCase.JVM_OPCODE);
			break;
		case "IincInsnNode":
			type = "IincNode";
			value = "var: var" + ((IincInsnNode) insn).var + ", value: " + ((IincInsnNode) insn).incr;
			break;
		case "IntInsnNode":
			IntInsnNode inode = (IntInsnNode) insn;
			value = "operand: " + inode.operand;
			break;
		case "InvokeDynamicInsnNode":
			InvokeDynamicInsnNode dnode = (InvokeDynamicInsnNode) insn;
			String bsmArgs = "";
			for (Object bsmArg : dnode.bsmArgs) {
				if (!bsmArgs.isEmpty())
					bsmArgs += ", ";
				if (bsmArg instanceof Handle) {
					bsmArgs += "[ desc: \"" + ((Handle) bsmArg).getDesc() + "\", tag: " + ((Handle) bsmArg).getTag()
							+ ", owner: \"" + ((Handle) bsmArg).getOwner() + "\", name: \""
							+ ((Handle) bsmArg).getName() + "\" ]";
				} else if (bsmArg instanceof Type) {
					Type bArgType = (Type) bsmArg;
					bsmArgs += "\"" + bArgType.getDescriptor() + "\"";
				}
			}
			value = "call: \"" + dnode.name + "\", desc: \"" + dnode.desc + "\", bsm: [ desc: \"" + dnode.bsm.getDesc()
					+ "\", tag: " + dnode.bsm.getTag() + ", owner: \"" + dnode.bsm.getOwner() + "\", name: \""
					+ dnode.bsm.getName() + "\" ], bsmArgs: [ " + bsmArgs + "]";
			break;
		case "FrameNode":
			type = "FrameNode";
			String stack = "";
			String local = "";
			FrameNode fnode = (FrameNode) insn;
			if (fnode.stack != null) {
				stack += " ";

				boolean first = true;
				for (Object item : fnode.stack) {
					if (!first)
						stack += ", ";

					first = false;

					if (item instanceof String) {
						stack += "\"" + item + "\"";
					} else if (item instanceof LabelNode) {
						LabelNode label = (LabelNode) item;
						String label_value = "";
						if (label.getNext() instanceof TypeInsnNode) {
							label_value = ((TypeInsnNode) label.getNext()).desc.replace("/", ".");
						}

						stack += "\"" + getOpcodeName(label.getOpcode(), OpcodeUseCase.ASM_STACKMAP) + " " + label_value
								+ "\"";
					} else {
						stack += "\"" + getOpcodeName((Integer) item, OpcodeUseCase.STACK_FRAME) + "\"";
					}
				}

				stack += " ";
			}
			if (fnode.local != null) {
				local += " ";

				boolean first = true;
				for (Object item : fnode.local) {
					if (!first)
						local += ", ";

					first = false;

					if (item instanceof String) {
						local += "\"" + item + "\"";
					} else {
						local += "\"" + getOpcodeName((Integer) item, OpcodeUseCase.STACK_FRAME) + "\"";
					}
				}

				local += " ";
			}

			value = "stack: [" + stack + "], local: [" + local + "]";
			break;
		}
		if (output.equals("")) {
			String opcode = getOpcodeName(insn.getOpcode(), OpcodeUseCase.JVM_OPCODE);
			if (opcode != null && opcode.contains("RETURN")) {
				value = opcode;
			}
		}
		if (output.equals("")) {
			String methName = "";
			String objName = "";
			if (type.equals("")) {
				type = getOpcodeName(insn.getOpcode(), OpcodeUseCase.JVM_OPCODE).toLowerCase();
				methName = type;
				type = type.substring(0, 1).toUpperCase() + type.substring(1) + "Node";
				objName = type.replaceAll("[^A-Z]", "").toLowerCase();
				if (objName.equals(""))
					objName = type;
			} else {
				objName = type.replaceAll("[^A-Z]", "").toLowerCase();
				if (objName.equals(""))
					objName = type;

				methName = type.toLowerCase().replaceAll("node", "");
			}

			output += type + " " + objName + (index + 1) + " = " + methName + "(" + value + ");";
		}

		return output;
	}

	@Override
	protected String mthHeadToString(MethodNode method) {
		FluidMethodInfo meth = FluidMethodInfo.create(method.name, method.desc);
		StringBuilder result = new StringBuilder();
		StringBuilder annotationHead = new StringBuilder();

		if (method.localVariables != null) {
			for (LocalVariableNode var : method.localVariables) {
				if (!annotationHead.isEmpty())
					annotationHead.append("\n");

				AnnotationInfo anno = new AnnotationInfo();
				anno.name = "LocalVariable";
				anno.values.put("%pseudoannotation", true);

				int labelIndex = 1;
				AbstractInsnNode tmpNode = var.end.getPrevious();
				while (tmpNode != null) {
					if (tmpNode instanceof LabelNode)
						labelIndex++;
					tmpNode = tmpNode.getPrevious();
				}

				anno.values.put("_end", "label" + labelIndex);

				labelIndex = 1;
				tmpNode = var.start.getPrevious();
				while (tmpNode != null) {
					if (tmpNode instanceof LabelNode)
						labelIndex++;
					tmpNode = tmpNode.getPrevious();
				}
				anno.values.put("_start", "label" + labelIndex);
				anno.values.put("type", Fluid.parseDescriptor(var.desc));
				anno.values.put("index", var.index);
				anno.values.put("name", var.name);

				annotationHead.append(annotationToString(anno));
			}
		}

		String annotations = mthAnnotationHeadToString(method);
		if (!annotationHead.isEmpty() && !annotations.isEmpty())
			annotationHead.append("\n\n");

		annotationHead.append(annotations);

		if (!annotationHead.isEmpty()) {
			result.append(annotationHead).append("\n");
		}
		String mod = Modifier.toString(method.access);

		String[] lines = Splitter.split(annotationHead.toString(), '\n');
		Arrays.sort(lines, (one, two) -> {
			return one.compareTo(two);
		});

		annotationHead = new StringBuilder();
		for (String line : lines) {
			if (!annotationHead.isEmpty())
				annotationHead.append("\n");
			annotationHead.append(line);
		}

		result.append(mod);
		if (!mod.isEmpty())
			result.append(" ");
		result.append(meth.returnType);
		result.append(" ");
		result.append(meth.name);
		result.append("(");

		int var = 0;
		if (!Modifier.isStatic(method.access))
			var = 1;
		boolean hasAnnotations = false;
		boolean first = true;
		for (String type : meth.types) {
			if (!first)
				result.append(", ");
			first = false;

			List<AnnotationInfo> paramAnnotations = FluidMethodInfo.getParameterAnnotations(method, var);
			for (AnnotationInfo paramAnno : paramAnnotations) {
				result.append("\n\t");
				result.append(annotationToString(paramAnno));
				result.append(" ");
				hasAnnotations = true;
			}

			result.append(type);
			result.append(" var").append(var++);
		}
		if (hasAnnotations)
			result.append("\n");

		result.append(")");

		first = true;
		if (method.exceptions != null && method.exceptions.size() != 0) {
			result.append(" throws ");
			for (String exception : method.exceptions) {
				if (!first)
					result.append(", ");
				first = false;

				result.append(exception.replaceAll("/", "."));
			}
		}

		if (!Modifier.isAbstract(method.access)) {
			result.append(" {");

			result.append("\n\t");
			result.append("// Method descriptor: " + meth.name + meth.toDescriptor());
		} else
			result.append(";");

		return result.toString();
	}

	@Override
	protected String mthAnnotationHeadToString(MethodNode method) {
		String result = "";

		if (method.visibleAnnotations != null) {
			for (AnnotationNode anode : method.visibleAnnotations) {
				if (!result.isEmpty())
					result += "\n";

				AnnotationInfo i = AnnotationInfo.create(anode);
				i.values.put("%invis", false);
				result += annotationToString(i);
			}
		}

		if (method.invisibleAnnotations != null) {
			for (AnnotationNode anode : method.invisibleAnnotations) {
				if (!result.isEmpty())
					result += "\n";

				AnnotationInfo i = AnnotationInfo.create(anode);
				i.values.put("%invis", true);
				result += annotationToString(i);
			}
		}

		if (method.visibleLocalVariableAnnotations != null) {
			for (LocalVariableAnnotationNode anode : method.visibleLocalVariableAnnotations) {
				if (!result.isEmpty())
					result += "\n";

				AnnotationInfo i = AnnotationInfo.create(anode);
				i.values.put("%invis", false);
				i.values.put("%localVarAnnotation", true);
				i.values.put("%varIndex", anode.index);

				result += annotationToString(i);
			}
		}

		if (method.invisibleLocalVariableAnnotations != null) {
			for (LocalVariableAnnotationNode anode : method.invisibleLocalVariableAnnotations) {
				if (!result.isEmpty())
					result += "\n";

				AnnotationInfo i = AnnotationInfo.create(anode);
				i.values.put("%invis", true);
				i.values.put("%localVarAnnotation", true);
				i.values.put("%varIndex", anode.index);

				result += annotationToString(i);
			}
		}

		return result;
	}

	private String annotationToString(AnnotationInfo anno) {
		String result = "";

		result += "@" + anno.name;
		if (anno.values.size() != 0)
			result += "(";
		for (int i = anno.values.keySet().size() - 1; i >= 0; i--) {
			String param = anno.values.keySet().toArray(t -> new String[t])[i];
			Object value = anno.values.get(param);
			if (!result.endsWith("("))
				result += ", ";
			result += parseParam(param, value);
		}
		if (anno.values.size() != 0)
			result += ")";

		return result;
	}

	private String parseParam(String param, Object value) {
		if (value instanceof String) {
			return param + ": \"" + value + "\"";
		} else if (value instanceof Enum) {
			return param + ": " + value.getClass().getTypeName() + "." + ((Enum<?>) value).name();
		} else if (value instanceof UnrecognizedEnumInfo) {
			return ((UnrecognizedEnumInfo) value).getType() + "." + ((UnrecognizedEnumInfo) value).getName();
		} else if (value instanceof Integer || value instanceof Boolean) {
			return param + ": " + value.toString();
		} else if (value instanceof List) {
			String strs = "";
			for (Object obj : (List<?>) value) {
				if (!strs.isEmpty())
					strs += ", ";

				strs += parseParam("dummy", obj).substring("dummy: ".length());
			}
			return param + ": [ " + strs + " ]";
		} else {
			return param + ": " + value.toString() + value.getClass().getSimpleName().substring(0, 1).toLowerCase();
		}
	}

	@Override
	protected String classHeadToString(ClassNode cls) {
		StringBuilder result = new StringBuilder();

		if (cls.visibleAnnotations != null) {
			for (AnnotationNode anode : cls.visibleAnnotations) {
				if (!result.isEmpty())
					result.append("\n");

				AnnotationInfo i = AnnotationInfo.create(anode);
				i.values.put("%invis", false);
				result.append(annotationToString(i));
			}
		}

		if (cls.invisibleAnnotations != null) {
			for (AnnotationNode anode : cls.invisibleAnnotations) {
				if (!result.isEmpty())
					result.append("\n");

				AnnotationInfo i = AnnotationInfo.create(anode);
				i.values.put("%invis", true);
				result.append(annotationToString(i));
			}
		}

		if (!result.isEmpty())
			result.append("\n");

		boolean added = false;
		if (Modifier.isPublic(cls.access)) {
			result.append("public");
			added = true;
		}

		if (Modifier.isPrivate(cls.access)) {
			if (added)
				result.append(" ");

			result.append("private");
			added = true;
		}

		if (Modifier.isStatic(cls.access)) {
			if (added)
				result.append(" ");

			result.append("static");
			added = true;
		}

		if (Modifier.isProtected(cls.access)) {
			if (added)
				result.append(" ");

			result.append("protected");
			added = true;
		}

		if (Modifier.isInterface(cls.access) && cls.interfaces != null
				&& cls.interfaces.stream().anyMatch(t -> t.equals("java/lang/annotation/Annotation"))) {
			if (added)
				result.append(" ");

			result.append("@interface");
			added = true;
		} else if (Modifier.isInterface(cls.access)) {
			if (added)
				result.append(" ");

			result.append("interface");
			added = true;
		}

		if (added)
			result.append(" ");

		result.append("class");
		result.append(" ");
		result.append(cls.name.replaceAll("/", "."));

		if (cls.superName != null && !cls.superName.equals(Object.class.getTypeName().replaceAll("\\.", "/"))) {
			result.append(" extends ");
			result.append(cls.superName.replaceAll("/", "."));
		}

		if (!Modifier.isInterface(cls.access) && cls.interfaces != null && cls.interfaces.size() != 0) {
			result.append(" implements");
			boolean first = true;
			for (String inter : cls.interfaces) {
				if (!first)
					result.append(",");
				result.append(" ").append(inter.replaceAll("/", "."));
				first = false;
			}
		}

		result.append(" {");

		return result.toString();
	}

	@Override
	protected String fieldToStringInternal(FieldNode field) {
		StringBuilder result = new StringBuilder();

		if (field.visibleAnnotations != null) {
			for (AnnotationNode anode : field.visibleAnnotations) {
				if (!result.isEmpty())
					result.append("\n");

				AnnotationInfo i = AnnotationInfo.create(anode);
				i.values.put("%invis", false);
				result.append(annotationToString(i));
			}
		}

		if (field.invisibleAnnotations != null) {
			for (AnnotationNode anode : field.invisibleAnnotations) {
				if (!result.isEmpty())
					result.append("\n");

				AnnotationInfo i = AnnotationInfo.create(anode);
				i.values.put("%invis", true);
				result.append(annotationToString(i));
			}
		}

		if (!result.isEmpty())
			result.append("\n");

		String mod = Modifier.toString(field.access);
		result.append(mod);

		if (!mod.isEmpty())
			result.append(" ");

		result.append(Fluid.parseDescriptor(field.desc));
		result.append(" ");
		result.append(field.name);

		if (field.value != null) {
			result.append(" = ");
			Object value = field.value;
			if (value instanceof String) {
				result.append("\"" + value + "\"");
			} else if (value instanceof Integer || value instanceof Boolean) {
				result.append(value.toString());
			} else {
				result.append(value.toString() + value.getClass().getSimpleName().substring(0, 1).toLowerCase());
			}
		}
		result.append(";");

		return result.toString();
	}

}
