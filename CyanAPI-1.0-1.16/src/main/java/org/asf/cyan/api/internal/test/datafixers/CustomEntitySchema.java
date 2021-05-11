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
	
	protected void registerCustomEntity(String entity, Schema schema, Map<String, Supplier<TypeTemplate>> map) {
		schema.register(map, entity, () -> {
			return createTemplate(schema);
		});
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		
		// Custom entities
		registerCustomEntity("testmod:testentity", schema, map);
		
		return map;
	}

}
