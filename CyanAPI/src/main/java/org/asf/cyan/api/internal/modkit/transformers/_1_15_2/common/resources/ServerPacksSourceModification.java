package org.asf.cyan.api.internal.modkit.transformers._1_15_2.common.resources;

import java.util.function.Consumer;

import org.asf.cyan.api.internal.modkit.components._1_15_2.common.resources.CyanPackResources;
import org.asf.cyan.api.internal.modkit.components._1_15_2.common.resources.CyanPackSupplier;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.Pack.PackConstructor;
import net.minecraft.server.packs.repository.Pack.Position;

@FluidTransformer
@TargetClass(target = "net.minecraft.server.packs.ServerPacksSource")
public class ServerPacksSourceModification {

	private CyanPackResources cyanMods = null;

	@InjectAt(location = InjectLocation.HEAD)
	public void loadPacks(Consumer<Pack> consumer,
			@TargetType(target = "net.minecraft.server.packs.repository.Pack$PackConstructor") PackConstructor constructor) {
		if (cyanMods == null)
			cyanMods = new CyanPackResources();

		Pack cyanPack = Pack.create("cyan", true, new CyanPackSupplier(cyanMods), constructor, Position.BOTTOM,
				PackSource.BUILT_IN);
		if (cyanPack != null)
			consumer.accept(cyanPack);
	}

}
