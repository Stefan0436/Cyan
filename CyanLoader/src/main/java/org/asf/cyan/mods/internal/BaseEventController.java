package org.asf.cyan.mods.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.function.BiConsumer;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.events.core.IEventListener;
import org.asf.cyan.api.events.core.ISynchronizedEventListener;
import org.asf.cyan.api.modloader.IModloaderComponent;
import org.asf.cyan.api.modloader.TargetModloader;
import org.asf.cyan.mods.events.AttachEvent;
import org.asf.cyan.mods.events.IEventListenerContainer;

@TargetModloader(value = CyanLoader.class, any = true)
public class BaseEventController implements IModloaderComponent {

	public BaseEventController() {
	}

	private static BaseEventController controller;
	private BiConsumer<String, IEventListener> attachMethod;
	private boolean runtime = false;

	private ArrayList<IEventListenerContainer> containers = new ArrayList<IEventListenerContainer>();

	private <T extends IEventListener> void attachEventListener(String event, T listener) {
		attachMethod.accept(event, listener);
	}

	public void attachListenerRegistry(BiConsumer<String, IEventListener> attachMethod) {
		this.attachMethod = attachMethod;
	}

	public void assign() {
		controller = this;
	}

	public static void addEventContainer(IEventListenerContainer container) {
		if (!controller.runtime)
			controller.containers.add(container);
		else
			controller.processContainer(container);
	}

	public void processContainer(IEventListenerContainer container) {
		for (Method mth : container.getClass().getDeclaredMethods()) {
			if (mth.isAnnotationPresent(AttachEvent.class)) {
				mth.setAccessible(true);
				AttachEvent ev = mth.getAnnotation(AttachEvent.class);

				if (ev.synchronize()) {
					attachEventListener(ev.value(), new ISynchronizedEventListener() {

						@Override
						public String getListenerName() {
							return mth.getName();
						}

						@Override
						public void received(Object... params) {
							runEventListener(params, container, mth);
						}
					});
				} else {
					attachEventListener(ev.value(), new IEventListener() {

						@Override
						public String getListenerName() {
							return mth.getName();
						}

						@Override
						public void received(Object... params) {
							runEventListener(params, container, mth);
						}
					});
				}
			}
		}
	}

	private void runEventListener(Object[] params, IEventListenerContainer owner, Method mth) {
		if (params.length != mth.getParameterCount())
			return;
		else {
			int index = 0;
			for (Object param : params) {
				Class<?> cls = param.getClass();
				Class<?> clsTwo = mth.getParameters()[index].getType();

				if (!clsTwo.isAssignableFrom(cls)) {
					return;
				}

				index++;
			}
		}

		try {
			mth.invoke(owner, params);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public static void work() {
		if (controller.runtime)
			return;

		for (IEventListenerContainer container : controller.containers) {
			controller.processContainer(container);
		}
		controller.containers.clear();

		controller.runtime = true;
	}

}
