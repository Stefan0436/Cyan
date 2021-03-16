package org.asf.cyan.fluid.remapping;

import org.asf.cyan.fluid.deobfuscation.DeobfuscationTargetMap;
import org.objectweb.asm.commons.Remapper;

public class FluidClassRemapper extends Remapper {

	private DeobfuscationTargetMap map;

	public FluidClassRemapper(DeobfuscationTargetMap mp) {
		this.map = mp;
	}

	@Override
	public String map(String internal) {
		if (map.containsKey(internal))
			return map.get(internal).jvmName;
		return internal;
	}
	
}
