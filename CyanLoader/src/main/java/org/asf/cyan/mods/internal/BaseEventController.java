package org.asf.cyan.mods.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.events.core.IEventListener;
import org.asf.cyan.api.events.core.ISynchronizedEventListener;
import org.asf.cyan.api.events.extended.EventObject;
import org.asf.cyan.api.events.extended.IExtendedEvent;
import org.asf.cyan.api.events.extended.IExtendedEventListener;
import org.asf.cyan.api.events.extended.IExtendedSynchronizedEventListener;
import org.asf.cyan.api.modloader.IModloaderComponent;
import org.asf.cyan.api.modloader.TargetModloader;
import org.asf.cyan.mods.events.SimpleEvent;
import org.asf.cyan.mods.events.AttachEvent;
import org.asf.cyan.mods.events.IEventListenerContainer;

@TargetModloader(value = CyanLoader.class, any = true)
public class BaseEventController implements IModloaderComponent {

	public BaseEventController() {
	}

	private static BaseEventController controller;
	private Function<Class<? extends IExtendedEvent<?>>, String> extendedEventSupplier;
	private BiConsumer<String, IEventListener> attachMethod;
	private boolean runtime = false;

	private ArrayList<IEventListenerContainer> containers = new ArrayList<IEventListenerContainer>();

	private <T extends IEventListener> void attachEventListener(String event, T listener) {
		attachMethod.accept(event, listener);
	}

	public void attachListenerRegistry(BiConsumer<String, IEventListener> attachMethod,
			Function<Class<? extends IExtendedEvent<?>>, String> extendedEventSupplier) {
		this.attachMethod = attachMethod;
		this.extendedEventSupplier = extendedEventSupplier;
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
							return container.getClass().getTypeName() + ":" + mth.getName() + "@"
									+ System.currentTimeMillis() + "@" + System.nanoTime();
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
							return container.getClass().getTypeName() + ":" + mth.getName() + "@"
									+ System.currentTimeMillis() + "@" + System.nanoTime();
						}

						@Override
						public void received(Object... params) {
							runEventListener(params, container, mth);
						}
					});
				}
			} else if (mth.isAnnotationPresent(SimpleEvent.class)) {
				mth.setAccessible(true);
				SimpleEvent ev = mth.getAnnotation(SimpleEvent.class);
				if (mth.getParameterCount() == 1
						&& EventObject.class.isAssignableFrom(mth.getParameters()[0].getType())) {
					String name = extendedEventSupplier.apply(ev.value());
					if (name != null) {
						if (ev.synchronize()) {
							attachEventListener(name, new IExtendedSynchronizedEventListener<EventObject>() {

								@Override
								public String getListenerName() {
									return container.getClass().getTypeName() + ":" + mth.getName() + "@"
											+ System.currentTimeMillis() + "@" + System.nanoTime();
								}

								@Override
								public void received(EventObject params) {
									runEventListener(new Object[] { params }, container, mth);
								}
							});
						} else {
							attachEventListener(name, new IExtendedEventListener<EventObject>() {

								@Override
								public String getListenerName() {
									return container.getClass().getTypeName() + ":" + mth.getName() + "@"
											+ System.currentTimeMillis() + "@" + System.nanoTime();
								}

								@Override
								public void received(EventObject params) {
									runEventListener(new Object[] { params }, container, mth);
								}
							});
						}
					}
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
