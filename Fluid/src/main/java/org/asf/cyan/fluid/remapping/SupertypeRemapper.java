package org.asf.cyan.fluid.remapping;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;

import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.Transformer.FluidMethodInfo;
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
public class SupertypeRemapper implements Closeable {
	private ArrayList<DeobfuscationTargetMap> mappings = new ArrayList<DeobfuscationTargetMap>();

	public SupertypeRemapper() {

	}

	public SupertypeRemapper addMappings(DeobfuscationTargetMap... mappings) {
		for (DeobfuscationTargetMap map : mappings) {
			this.mappings.add(map);
		}
		return this;
	}

	public SupertypeRemapper addMappings(Iterable<DeobfuscationTargetMap> mappings) {
		for (DeobfuscationTargetMap map : mappings) {
			this.mappings.add(map);
		}
		return this;
	}

	public SupertypeRemapper(Iterable<DeobfuscationTargetMap> initialMappings) {
		for (DeobfuscationTargetMap mappings : initialMappings) {
			this.mappings.add(mappings);
		}
	}

	public SupertypeRemapper(DeobfuscationTargetMap... initialMappings) {
		for (DeobfuscationTargetMap mappings : initialMappings) {
			this.mappings.add(mappings);
		}
	}

	/**
	 * Remaps the given class node (if its supertype or interface implements a
	 * matching class)
	 * 
	 * @param input Input class
	 * @return Remapped class
	 */
	public ClassNode remap(ClassNode input) {
		if (input.superName != null && !input.superName.equals("java/lang/Object")) {
			mappings.forEach((mappings) -> {
				mappings.forEach((type, target) -> {
					if (type.equals(input.superName)) {
						remap(input, type, target);
					}
				});
			});
		}
		if (input.interfaces != null) {
			for (String interfaceName : input.interfaces) {
				mappings.forEach((mappings) -> {
					mappings.forEach((type, target) -> {
						if (type.equals(interfaceName)) {
							remap(input, type, target);
						}
					});
				});
			}
		}

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

	@Override
	public void close() {
		mappings.clear();
	}
}
