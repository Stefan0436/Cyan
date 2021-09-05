package org.asf.cyan.api.internal.modkit.transformers._1_17.client.network;

import com.google.gson.JsonObject;

public interface ServerDataAccessor {
	
	public JsonObject cyanGetServerData();
	public void cyanAssignJsonObject(JsonObject obj);
	
}
