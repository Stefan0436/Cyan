package org.asf.cyan.api.internal.modkit.transformers._1_15_2.common;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.TargetClass;

@FluidTransformer
@TargetClass(target = "net.minecraft.server.players.StoredUserEntry")
public class StoredUserEntryModification implements PlayerEntryExtension {
	private final Object user = null;

	@Override
	public Object getUserCyan() {
		return user;
	}
}
