package org.asf.cyan.mixin.services;

import java.util.HashMap;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

public class CyanMixinGlobalPropStore implements IGlobalPropertyService {

	class MixinProp implements IPropertyKey {
		String name = "";
		
		public MixinProp(String name) {
			this.name = name;
		}
		
		@Override
		public String toString() {
			return name;
		}
	}

	HashMap<MixinProp, Object> props = new HashMap<MixinProp, Object>(); 
	
	@Override
	public IPropertyKey resolveKey(String name) {
		for (MixinProp k : props.keySet()) {
			if (k.name.equals(name))
				return k;
		}
		MixinProp p = new MixinProp(name);
		props.put(p, null);
		return p;
	}

	@Override
	public <T> T getProperty(IPropertyKey key) {
		return getProperty(key, null);
	}

	@Override
	public void setProperty(IPropertyKey key, Object value) {
		props.put(findKey(key), value);
	}

	private MixinProp findKey(IPropertyKey key) {
		String name = key.toString();
		if (key instanceof MixinProp) {
			name = ((MixinProp)key).name;
		}
		for (MixinProp k : props.keySet()) {
			if (k.name.equals(name))
				return k;
		}
		return new MixinProp(name);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getProperty(IPropertyKey key, T defaultValue) {
		MixinProp k = findKey(key);
		if (!props.containsKey(k))
			props.put(k, defaultValue);
		return (T)props.get(k);
	}

	@Override
	public String getPropertyString(IPropertyKey key, String defaultValue) {
		MixinProp k = findKey(key);
		if (!props.containsKey(k))
			props.put(k, defaultValue);
		return props.get(k).toString();
	}

}
