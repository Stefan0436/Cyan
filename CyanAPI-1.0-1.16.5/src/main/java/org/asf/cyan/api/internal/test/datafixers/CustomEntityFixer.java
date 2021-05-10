package org.asf.cyan.api.internal.test.datafixers;

import java.util.Map;
import java.util.function.Supplier;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class CustomEntityFixer {

	public static class CustomEntityAddSchema extends NamespacedSchema {

		public CustomEntityAddSchema(int version, Schema parent) {
			super(version, parent);
		}

		protected static TypeTemplate equipment(Schema var0) {
			return DSL.optionalFields("ArmorItems", DSL.list(References.ITEM_STACK.in(var0)), "HandItems",
					DSL.list(References.ITEM_STACK.in(var0)));
		}

		@Override
		public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
			Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
			schema.register(map, "testmod:testentity", () -> {
				return equipment(schema);
			});
			return map;
		}

	}

}
