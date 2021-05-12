package org.asf.cyan.internal.modkitimpl.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.function.Supplier;

import org.asf.cyan.api.util.ContainerConditions;
import org.asf.cyan.api.util.EventUtil;
import org.asf.cyan.mods.events.AttachEvent;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.internal.BaseEventController;

public class EventUtilImpl extends EventUtil implements IEventListenerContainer {

	private ArrayList<Supplier<String>> types = new ArrayList<Supplier<String>>();

	public static void init() {
		implementation = new EventUtilImpl();
		BaseEventController.addEventContainer((EventUtilImpl)implementation);
	}

	@SuppressWarnings("unchecked")
	@AttachEvent(value = "mods.aftermodloader", synchronize = true)
	private void preGameStart(ClassLoader loader)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		for (Supplier<String> type : types) {
			Class<IEventListenerContainer> cls = (Class<IEventListenerContainer>) loader.loadClass(type.get());
			BaseEventController.addEventContainer(cls.getConstructor().newInstance());
		}
	}

	@Override
	protected void registerLater(ContainerConditions conditions, Supplier<String> containerType) {
		if (conditions.applies())
			types.add(containerType);
	}

}
