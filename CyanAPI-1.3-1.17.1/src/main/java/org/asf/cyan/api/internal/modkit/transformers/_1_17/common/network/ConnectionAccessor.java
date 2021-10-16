package org.asf.cyan.api.internal.modkit.transformers._1_17.common.network;

public interface ConnectionAccessor {

	public <T> void cyanSetData(String key, T value, Class<T> type);

	public <T> T cyanGetData(String key, Class<T> type);

}
