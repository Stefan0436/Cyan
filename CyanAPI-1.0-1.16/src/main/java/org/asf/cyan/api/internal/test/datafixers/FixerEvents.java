package org.asf.cyan.api.internal.test.datafixers;

import org.asf.cyan.api.events.core.DataFixerEvent;
import org.asf.cyan.api.events.objects.core.DataFixerEventObject;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;

import com.mojang.datafixers.schemas.Schema;

import net.minecraft.SharedConstants;
import net.minecraft.util.datafix.fixes.AddNewChoices;
import net.minecraft.util.datafix.fixes.References;

public class FixerEvents implements IEventListenerContainer {

	@SimpleEvent(DataFixerEvent.class)
	public void test(DataFixerEventObject event) {
		Schema schem = event.getBuilder().addSchema(SharedConstants.getCurrentVersion().getWorldVersion(),
				CustomEntitySchema::new);
		event.getBuilder().addFixer(new AddNewChoices(schem, "Add custom entities", References.ENTITY));
	}

}
