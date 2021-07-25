package org.asf.cyan.fluid.remapping;

import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.deobfuscation.DeobfuscationTarget;
import org.asf.cyan.fluid.deobfuscation.DeobfuscationTargetMap;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.Remapper;

public class FluidMemberRemapper extends Remapper {

	public static class FluidMemberVisitor extends MethodRemapper {

		public FluidMemberVisitor(final MethodVisitor methodVisitor, final Remapper remapper) {
			super(methodVisitor, remapper);
		}

		@Override
		public void visitInvokeDynamicInsn(String name, final String descriptor, final Handle bootstrapMethodHandle,
				final Object... bootstrapMethodArguments) {
			if (remapper instanceof FluidMemberRemapper)
				name = ((FluidMemberRemapper) remapper).mapInvokeDynamicMethodName(name, descriptor,
						bootstrapMethodArguments);
			super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
		}

	}

	private DeobfuscationTargetMap map;

	public FluidMemberRemapper(DeobfuscationTargetMap mp) {
		map = mp;
	}

	public String mapInvokeDynamicMethodName(final String name, final String descriptor, final Object[] bsmArgs) {
		if (bsmArgs.length > 1 && bsmArgs[0] instanceof Type && descriptor.contains(")")) {
			String owner = Fluid.parseDescriptor(descriptor.substring(descriptor.indexOf(")") + 1)).replace(".", "/");
			String desc = ((Type) bsmArgs[0]).getDescriptor();
			return mapMethodName(owner, name, desc);
		}
		return name;
	}

	@Override
	public String mapMethodName(final String owner, final String name, final String descriptor) {
		if (map.containsKey(owner)) {
			DeobfuscationTarget cls = map.get(owner);
			return cls.methods.getOrDefault(name + " " + descriptor, name);
		}
		return name;
	}

	@Override
	public String mapFieldName(final String owner, final String name, final String descriptor) {
		if (map.containsKey(owner)) {
			DeobfuscationTarget cls = map.get(owner);
			return cls.fields.getOrDefault(name + " " + descriptor, name);
		}
		return name;
	}

}
