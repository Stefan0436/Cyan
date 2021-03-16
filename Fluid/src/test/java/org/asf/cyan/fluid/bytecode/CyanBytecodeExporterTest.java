package org.asf.cyan.fluid.bytecode;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Random;

import org.asf.cyan.fluid.TestCCImplementation;
import org.asf.cyan.fluid.bytecode.enums.OpcodeUseCase;
import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class CyanBytecodeExporterTest {
	Random rnd = new Random();

	//
	// Important notice: i am not sure how to verify the output, so the test always
	// passes if opcodeToStringTest is successful.
	// The test will only fail if an exception is thrown.
	//

	@Test
	public void opcodeToStringTest() throws IllegalArgumentException, IllegalAccessException {
		TestCCImplementation.setDebugLog();
		if (!TestCCImplementation.isInitialized()) {
			TestCCImplementation.initializeComponents();
		}

		for (Field f : Opcodes.class.getFields()) {
			if (f.getName().startsWith("ASM"))
				continue;

			Object val = f.get(null);
			OpcodeUseCase use = OpcodeUseCase.valueOf(f.getName().toUpperCase(), f.getType().getTypeName());
			if (f.getName().equals("SOURCE_DEPRECATED") || f.getName().equals("ACC_SUPER"))
				continue;

			if (f.getType().getTypeName().equals(Integer.class.getTypeName())) {
				if (!BytecodeExporter.opcodeToString((Integer) val, use).equals(f.getName())) {
					System.err.println(f.getName() + " was " + BytecodeExporter.opcodeToString((Integer) val, use)
							+ ", value: " + val);
				}
				assertTrue(BytecodeExporter.opcodeToString((Integer) val, use).equals(f.getName()));
			} else {
				if (!BytecodeExporter.opcodeToString((int) val, use).equals(f.getName())) {
					System.err.println(f.getName() + " was " + BytecodeExporter.opcodeToString((int) val, use)
							+ ", value: " + val);
				}
				assertTrue(BytecodeExporter.opcodeToString((int) val, use).equals(f.getName()));
			}
		}
	}

	@Test
	public void methodAnnotationHeadToStringTest() throws ClassNotFoundException {
		TestCCImplementation.setDebugLog();
		if (!TestCCImplementation.isInitialized()) {
			TestCCImplementation.initializeComponents();
		}
		FluidClassPool pool = FluidClassPool.create();

		ClassNode[] classes = new ClassNode[] { pool.getClassNode("aaa"), pool.getClassNode("SomeTest"),
				pool.getClassNode("aab"), pool.getClassNode("aac"), pool.getClassNode("aac$a1") };

		ClassNode nd = classes[rnd.nextInt(classes.length)];
		MethodNode mth = nd.methods.get(rnd.nextInt(nd.methods.size()));

		BytecodeExporter.methodAnnotationHeadToString(mth);
	}

	@Test
	public void methodToStringTest() throws ClassNotFoundException {
		TestCCImplementation.setDebugLog();
		if (!TestCCImplementation.isInitialized()) {
			TestCCImplementation.initializeComponents();
		}
		FluidClassPool pool = FluidClassPool.create();

		ClassNode[] classes = new ClassNode[] { pool.getClassNode("aaa"), pool.getClassNode("SomeTest"),
				pool.getClassNode("aab"), pool.getClassNode("aac"), pool.getClassNode("aac$a1") };

		ClassNode nd = classes[rnd.nextInt(classes.length)];
		MethodNode mth = nd.methods.get(rnd.nextInt(nd.methods.size()));

		BytecodeExporter.methodToString(mth);
	}

	@Test
	public void classToStringTest() throws ClassNotFoundException {
		TestCCImplementation.setDebugLog();
		if (!TestCCImplementation.isInitialized()) {
			TestCCImplementation.initializeComponents();
		}
		FluidClassPool pool = FluidClassPool.create();

		ClassNode[] classes = new ClassNode[] { pool.getClassNode("aaa"), pool.getClassNode("SomeTest"),
				pool.getClassNode("aab"), pool.getClassNode("aac"), pool.getClassNode("aac$a1") };

		ClassNode nd = classes[rnd.nextInt(classes.length)];
		BytecodeExporter.classToString(nd);
	}

	@Test
	public void methodHeadToStringTest() throws ClassNotFoundException {
		TestCCImplementation.setDebugLog();
		if (!TestCCImplementation.isInitialized()) {
			TestCCImplementation.initializeComponents();
		}
		FluidClassPool pool = FluidClassPool.create();

		ClassNode[] classes = new ClassNode[] { pool.getClassNode("aaa"), pool.getClassNode("SomeTest"),
				pool.getClassNode("aab"), pool.getClassNode("aac"), pool.getClassNode("aac$a1") };

		ClassNode nd = classes[rnd.nextInt(classes.length)];
		MethodNode mth = nd.methods.get(rnd.nextInt(nd.methods.size()));

		BytecodeExporter.methodHeadToString(mth);
	}

	@Test
	public void instructionToStringTest() throws ClassNotFoundException {
		TestCCImplementation.setDebugLog();
		if (!TestCCImplementation.isInitialized()) {
			TestCCImplementation.initializeComponents();
		}
		FluidClassPool pool = FluidClassPool.create();

		ClassNode[] classes = new ClassNode[] { pool.getClassNode("aaa"), pool.getClassNode("SomeTest"),
				pool.getClassNode("aab"), pool.getClassNode("aac"), pool.getClassNode("aac$a1") };

		ClassNode nd = classes[rnd.nextInt(classes.length)];
		MethodNode mth = nd.methods.get(rnd.nextInt(nd.methods.size()));
		AbstractInsnNode insn = mth.instructions.get(rnd.nextInt(mth.instructions.size()));
		BytecodeExporter.instructionToString(insn, mth.instructions.indexOf(insn));
	}
}
