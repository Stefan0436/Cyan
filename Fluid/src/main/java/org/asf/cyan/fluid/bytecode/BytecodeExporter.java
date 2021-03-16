package org.asf.cyan.fluid.bytecode;

import java.lang.reflect.Modifier;
import java.util.function.Consumer;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.config.serializing.internal.Splitter;
import org.asf.cyan.fluid.bytecode.enums.OpcodeUseCase;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * 
 * PseudoCode bytecode to text exporting system.<br/>
 * <b>Warning:</b> this class needs to have an implementation in order to work.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class BytecodeExporter extends CyanComponent {
	private static BytecodeExporter selectedImplementation;
	static BytecodeExporter getImplmentationInstance() {
		return selectedImplementation;
	}

	protected static void setImplementation(BytecodeExporter implementation) {
		debug("Assigning FLUID Bytecode Exporter Implementation... Using the " + implementation.getImplementationName() + " Implementation...");
		selectedImplementation = implementation;
	}

	protected BytecodeExporter() {}
	
	protected abstract String getImplementationName();
	protected abstract String getOpcodeName(Object opcode, OpcodeUseCase useCase);
	protected abstract String insnNodeToString(AbstractInsnNode insn, int index);
	protected abstract String mthAnnotationHeadToString(MethodNode method);
	protected abstract String mthHeadToString(MethodNode method);
	protected abstract String classHeadToString(ClassNode cls);
	protected abstract String fieldToStringInternal(FieldNode field);
	
	public static String opcodeToString(int opcode, OpcodeUseCase useCase) {
		return selectedImplementation.getOpcodeName(opcode, useCase);
	}
	
	public static String opcodeToString(Integer opcode, OpcodeUseCase useCase) {
		return selectedImplementation.getOpcodeName(opcode, useCase);
	}

	public static String instructionToString(AbstractInsnNode instruction, int index) {
		return selectedImplementation.insnNodeToString(instruction, index);
	}

	public static String methodAnnotationHeadToString(MethodNode method) {
		return selectedImplementation.mthAnnotationHeadToString(method);
	}

	public static String methodHeadToString(MethodNode method) {
		return selectedImplementation.mthHeadToString(method);
	}

	public static String fieldToString(FieldNode field) {
		return selectedImplementation.fieldToStringInternal(field);
	}

	public static String methodToString(MethodNode method) {
		StringBuilder result = new StringBuilder();
		selectedImplementation.methodToStringInternal(method, (str) -> result.append(str));
		return result.toString();
	}

	public static String classToString(ClassNode cls) {
		StringBuilder result = new StringBuilder();
		boolean hasAnno = selectedImplementation.classToStringInternal(cls, (str) -> result.append(str));
		boolean first = true;
		boolean firstContainsAnno = hasAnno;
		if (result.toString().startsWith("\n"))
			firstContainsAnno = true;
		
		for (MethodNode mth : cls.methods) {
			if (!first) {
				result.append("\n");
			}
			
			result.append("\n");
			StringBuilder strs = new StringBuilder();
			selectedImplementation.methodToStringInternal(mth, (str)->{
				strs.append(str);
			});
			boolean first2 = true;
			if (strs.toString().startsWith("@")) {
				result.append("\n");
				if (first)
					firstContainsAnno = true;
			}
			
			for (String line : Splitter.split(strs.toString(), '\n')) {
				if (!first2)
					result.append("\n");
				
				result.append("\t").append(line);
				first2 = false;
			}
			
			first = false;
		}
		if (firstContainsAnno)
			result.append("\n");
		
		result.append("\n}");
		return result.toString();
	}
	
	protected boolean classToStringInternal(ClassNode cls, Consumer<String> append) {
		boolean firstIsAnno = false;
		boolean first = true;
		append.accept(selectedImplementation.classHeadToString(cls));
		for (FieldNode field : cls.fields) {
			append.accept("\n");
			append.accept("\t");
			String fieldOut = selectedImplementation.fieldToStringInternal(field).replaceAll("\n", "\n\t");
			if (fieldOut.startsWith("@")) {
				append.accept("\n\t");
				if (first)
					firstIsAnno = true;
			}
			first = false;
			append.accept(fieldOut);
		}
		return firstIsAnno;
	}
	
	protected void methodToStringInternal(MethodNode method, Consumer<String> append) {
		append.accept(methodHeadToString(method));
		
		boolean indent = false;
		int normalIndex = 0;
		int labelIndex = 0;
		append.accept("\n\n");
		if (!Modifier.isAbstract(method.access)) {
			for (AbstractInsnNode insn : method.instructions) {
				int index = normalIndex;
				if (insn instanceof LabelNode)
					index = labelIndex++;
				else
					normalIndex++;

				String instrOut = BytecodeExporter.instructionToString(insn, index);
				if (instrOut.endsWith("{")) {
					if (indent) {
						append.accept("\t}\n\n");
						normalIndex = 0;
					}
					indent = true;
				} else if (indent)
					append.accept("\t");

				append.accept("\t");
				append.accept(instrOut);
				append.accept("\n");
			}
			if (indent) {
				append.accept("\t}");
			}
			
			append.accept("\n}");
		}
	}
}
