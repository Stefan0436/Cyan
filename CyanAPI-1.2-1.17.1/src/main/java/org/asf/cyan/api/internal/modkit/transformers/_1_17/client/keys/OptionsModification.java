package org.asf.cyan.api.internal.modkit.transformers._1_17.client.keys;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.Modifiers;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import modkit.events.keys.KeyBindingRegistryEvent;
import modkit.events.objects.keys.KeyBindingRegistryEventObject;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

@FluidTransformer
@TargetClass(target = "net.minecraft.client.Options")
public class OptionsModification {

	@Modifiers(modifiers = Modifier.PUBLIC)
	public KeyMapping[] keyMappings = null;

	@Constructor
	@InjectAt(location = InjectLocation.TAIL, targetCall = "load()")
	public void ctor(@TargetType(target = "net.minecraft.client.Minecraft") Minecraft var1, File var2) {
		KeyBindingRegistryEventObject cyanKeys = new KeyBindingRegistryEventObject();
		KeyBindingRegistryEvent.getInstance().dispatch(cyanKeys).getResult();
		Map<Supplier<KeyMapping>, Consumer<KeyMapping>> keys = cyanKeys.getKeys();
		KeyMapping[] newCyanMappings = new KeyMapping[keys.size() + keyMappings.length];
		int i = 0;
		for (KeyMapping oldMapping : keyMappings) {
			newCyanMappings[i++] = oldMapping;
		}

		for (Supplier<KeyMapping> ctor : keys.keySet()) {
			KeyMapping mapping = ctor.get();
			newCyanMappings[i++] = mapping;
			keys.get(ctor).accept(mapping);
		}
		keyMappings = newCyanMappings;
	}

}
