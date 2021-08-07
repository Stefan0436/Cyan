package org.asf.cyan.fluid.remapping;

import java.util.Arrays;

import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.Transformer.FluidMethodInfo;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.asf.cyan.fluid.deobfuscation.DeobfuscationTarget;
import org.asf.cyan.fluid.deobfuscation.DeobfuscationTargetMap;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * 
 * FLUID supertype remapper -- remaps supertype implementations according to
 * FLUID deobfuscation mappings
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class SupertypeRemapper {
	private DeobfuscationTargetMap mappings;
	private FluidClassPool pool;

	public SupertypeRemapper(DeobfuscationTargetMap initialMappings) {
		mappings = initialMappings;
	}

	public SupertypeRemapper(DeobfuscationTargetMap initialMappings, FluidClassPool pool) {
		mappings = initialMappings;
		this.pool = pool;
	}

	/**
	 * Remaps the given class node (if its supertype or interface implements a
	 * matching class)
	 * 
	 * @param input Input class
	 * @return Remapped class
	 */
	public ClassNode remap(ClassNode input) {
		DeobfuscationTarget clsTarget = new DeobfuscationTarget();
		clsTarget.outputName = input.name.replace("/", ".");
		clsTarget.jvmName = input.name;

		if (input.superName != null && !input.superName.equals("java/lang/Object")) {
			if (pool != null) {
				try {
					remap(pool.getClassNode(input.superName));
				} catch (ClassNotFoundException e) {
				}
			}
			mappings.forEach((type, target) -> {
				if (type.equals(input.superName)) {
					for (String fieldKey : target.fields.keySet()) {
						clsTarget.fields.putIfAbsent(fieldKey, target.fields.get(fieldKey));
					}
					for (String mthKey : target.methods.keySet()) {
						clsTarget.methods.putIfAbsent(mthKey, target.methods.get(mthKey));
					}
					remap(input, type, target);
				}
			});
		}
		if (input.interfaces != null) {
			for (String interfaceName : input.interfaces) {
				if (pool != null) {
					try {
						remap(pool.getClassNode(interfaceName));
					} catch (ClassNotFoundException e) {
					}
				}
				mappings.forEach((type, target) -> {
					if (type.equals(interfaceName)) {
						for (String fieldKey : target.fields.keySet()) {
							clsTarget.fields.putIfAbsent(fieldKey, target.fields.get(fieldKey));
						}
						for (String mthKey : target.methods.keySet()) {
							clsTarget.methods.putIfAbsent(mthKey, target.methods.get(mthKey));
						}
						remap(input, type, target);
					}
				});
			}
		}
//		if (input.name.contains("$")) { // Not sure this is right
//			String host = input.name.substring(0, input.name.lastIndexOf("$"));
//			mappings.forEach((type, target) -> {
//				if (type.equals(host)) {
//					for (String fieldKey : target.fields.keySet()) {
//						clsTarget.fields.putIfAbsent(fieldKey, target.fields.get(fieldKey));
//					}
//					for (String mthKey : target.methods.keySet()) {
//						clsTarget.methods.putIfAbsent(mthKey, target.methods.get(mthKey));
//					}
//				}
//			});
//		}

		mappings.putIfAbsent(input.name, clsTarget);
		return input;
	}

	private void remap(ClassNode input, String typeName, DeobfuscationTarget type) {
		for (MethodNode mth : input.methods) {
			for (String method : type.methods.keySet()) {
				String deobf = type.methods.get(method);
				String name = method.substring(0, method.indexOf(" "));
				String desc = method.substring(method.indexOf(" ") + 1);
				if (name.equals(mth.name)) {
					String returnType = Fluid.parseDescriptor(desc.substring(desc.indexOf(")") + 1));
					String[] types = Fluid.parseMultipleDescriptors(desc.substring(1, desc.indexOf(")")));

					FluidMethodInfo meth = FluidMethodInfo.create(mth);
					if (meth.returnType.equals(returnType) && Arrays.equals(types, meth.types)) {
						mth.name = deobf;
					}
				}
			}
			for (AbstractInsnNode instr : mth.instructions) {
				if (instr instanceof FieldInsnNode) {
					FieldInsnNode fld = (FieldInsnNode) instr;
					for (String field : type.fields.keySet()) {
						String deobf = type.fields.get(field);
						String name = field.substring(0, field.indexOf(" "));
						String fieldType = field.substring(field.indexOf(" ") + 1);
						if (name.equals(fld.name) && (fld.owner.equals(input.name) || fld.owner.equals(typeName))
								&& fld.desc.equals(fieldType)) {
							fld.name = deobf;
						}
					}
				}
			}
		}
	}
}
