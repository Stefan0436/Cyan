package org.asf.cyan.api.internal.modkit.transformers._1_16.common.world.ai;

import java.lang.reflect.Modifier;
import java.util.Map;

import org.asf.cyan.api.events.ingame.entities.EntityAttributesEvent;
import org.asf.cyan.api.events.objects.ingame.entities.EntityAttributesEventObject;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.Modifiers;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

@FluidTransformer
@TargetClass(target = "net.minecraft.world.entity.ai.attributes.DefaultAttributes")
public class DefaultAttributesModification {

	@Modifiers(modifiers = Modifier.PUBLIC | Modifier.STATIC)
	private static Map<EntityType<? extends LivingEntity>, AttributeSupplier> SUPPLIERS;

	@Constructor(clinit = true)
	@InjectAt(location = InjectLocation.TAIL)
	public static void clinit() {
		Builder<EntityType<? extends LivingEntity>, AttributeSupplier> builder = ImmutableMap
				.<EntityType<? extends LivingEntity>, AttributeSupplier>builder().putAll(SUPPLIERS);

		EntityAttributesEventObject object = new EntityAttributesEventObject(builder);
		EntityAttributesEvent.getInstance().dispatch(object).getResult();
		builder = object.getBuilder();

		SUPPLIERS = builder.build();
	}
}
