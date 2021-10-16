package org.asf.cyan.api.internal.modkit.transformers._1_17.common.network;

import java.util.HashMap;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.TargetClass;

@FluidTransformer
@TargetClass(target = "net.minecraft.network.Connection")
public class ConnectionModification implements ConnectionAccessor {

	private HashMap<String, Object> cyanData;

	@Override
	public <T> void cyanSetData(String key, T value, Class<T> type) {
		if (cyanData == null)
			cyanData = new HashMap<String, Object>();
		cyanData.put(key + "-" + type.getTypeName(), value);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T cyanGetData(String key, Class<T> type) {
		if (cyanData == null)
			cyanData = new HashMap<String, Object>();
		return (T) cyanData.get(key + "-" + type.getTypeName());
	}

}
