package org.asf.cyan.mixin;

import java.util.HashMap;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

public class CyanMixinGlobalPropStore implements IGlobalPropertyService {

	class MixinProp implements IPropertyKey {
		String name = "";
		
		public MixinProp(String name) {
			this.name = name;
		}
		public MixinProp(IPropertyKey key) {
			if (key instanceof MixinProp) {
				name = ((MixinProp)key).name;
			} else {
				name = key.getClass().getTypeName();
			}
		}
		@Override
		public boolean equals(Object other) {
			if (other instanceof MixinProp) {
				return ((MixinProp)other).name.equals(name);
			} else return other.hashCode() == this.hashCode();
		}
	}

	HashMap<MixinProp, Object> props = new HashMap<MixinProp, Object>(); 
	
	@Override
	public IPropertyKey resolveKey(String name) {
		if (props.keySet().stream().anyMatch(t->t.name.equals(name))) {
			return props.keySet().stream().filter(t->t.name.equals(name)).findFirst().get();
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
		props.put(props.containsKey(key) ? (MixinProp)key : new MixinProp(key), value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getProperty(IPropertyKey key, T defaultValue) {
		return (T)props.getOrDefault(key, defaultValue);
	}

	@Override
	public String getPropertyString(IPropertyKey key, String defaultValue) {
		return props.getOrDefault(key, defaultValue).toString();		
	}

}
