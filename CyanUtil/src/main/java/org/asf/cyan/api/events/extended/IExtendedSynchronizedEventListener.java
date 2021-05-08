package org.asf.cyan.api.events.extended;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.asf.cyan.api.events.core.ISynchronizedEventListener;
import org.asf.cyan.api.events.extended.EventObject.EventResult;

/**
 * 
 * Extended synchrFonized event listener
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 * @param <T> Event object type
 */
public interface IExtendedSynchronizedEventListener<T extends EventObject> extends ISynchronizedEventListener {
	@SuppressWarnings("unchecked")
	public default void received(Object... params) {
		if (params.length != 1)
			return;

		Class<T> type;
		try {
			ParameterizedType eventSuper = null;
			for (Type t : getClass().getGenericInterfaces()) {
				String typeName = t.getTypeName();
				typeName = typeName.substring(0, typeName.indexOf("<"));
				if (typeName.equals(IExtendedSynchronizedEventListener.class.getTypeName()))
					eventSuper = (ParameterizedType) t;
			}
			type = (Class<T>) Class.forName(eventSuper.getActualTypeArguments()[0].getTypeName());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		if (type.isAssignableFrom(params[0].getClass()) && ((T) params[0]).getResult() != EventResult.CANCEL) {
			received((T) params[0]);
		}
	}

	public void received(T params);
}
