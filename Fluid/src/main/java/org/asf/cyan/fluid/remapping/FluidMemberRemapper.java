package org.asf.cyan.fluid.remapping;

import org.asf.cyan.fluid.deobfuscation.DeobfuscationTarget;
import org.asf.cyan.fluid.deobfuscation.DeobfuscationTargetMap;
import org.objectweb.asm.commons.Remapper;

public class FluidMemberRemapper extends Remapper {

	private DeobfuscationTargetMap map;

	public FluidMemberRemapper(DeobfuscationTargetMap mp) {
		map = mp;
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
