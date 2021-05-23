package org.asf.cyan.internal.modkitimpl.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.function.Supplier;

import org.asf.cyan.core.CyanCore;
import org.asf.cyan.mods.events.AttachEvent;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.internal.BaseEventController;

import modkit.util.ContainerConditions;
import modkit.util.EventUtil;

public class EventUtilImpl extends EventUtil implements IEventListenerContainer {

	private ArrayList<Supplier<String>> types = new ArrayList<Supplier<String>>();

	public static void init() {
		implementation = new EventUtilImpl();
		BaseEventController.addEventContainer((EventUtilImpl) implementation);
	}

	@SuppressWarnings("unchecked")
	@AttachEvent(value = "mods.all.loaded", synchronize = true)
	private void preGameStart(ClassLoader loader)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException, ClassNotFoundException {
		for (Supplier<String> type : types) {
			Class<IEventListenerContainer> cls;
			String name = type.get();
			try {
				cls = (Class<IEventListenerContainer>) loader.loadClass(name);
			} catch (ClassNotFoundException e) {
				try {
					cls = (Class<IEventListenerContainer>) CyanCore.getClassLoader().loadClass(name);
				} catch (ClassNotFoundException e2) {
					cls = (Class<IEventListenerContainer>) CyanCore.getCoreClassLoader().loadClass(name);
				}
			}
			BaseEventController.addEventContainer(cls.getConstructor().newInstance());
		}
	}

	@Override
	protected void registerLater(ContainerConditions conditions, Supplier<String> containerType) {
		if (conditions.applies())
			types.add(containerType);
	}

}
