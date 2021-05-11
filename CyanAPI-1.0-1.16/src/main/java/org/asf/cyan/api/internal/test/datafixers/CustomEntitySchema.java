package org.asf.cyan.api.internal.test.datafixers;

import java.util.Map;
import java.util.function.Supplier;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class CustomEntitySchema extends NamespacedSchema {

	public CustomEntitySchema(int version, Schema parent) {
		super(version, parent);
	}

	protected TypeTemplate createTemplate(Schema schema) {
		return DSL.list(References.ITEM_STACK.in(schema));
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		schema.register(map, "testmod:testentity", () -> {
			return createTemplate(schema);
		});
		return map;
	}

}
