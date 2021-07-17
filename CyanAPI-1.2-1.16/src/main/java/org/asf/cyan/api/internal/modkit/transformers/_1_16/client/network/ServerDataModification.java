package org.asf.cyan.api.internal.modkit.transformers._1_16.client.network;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.TargetClass;

import com.google.gson.JsonObject;

@FluidTransformer
@TargetClass(target = "net.minecraft.client.multiplayer.ServerData")
public class ServerDataModification implements ServerDataAccessor {

	private JsonObject cyanServerStatus;

	@Override
	public JsonObject cyanGetServerData() {
		return cyanServerStatus;
	}

	@Override
	public void cyanAssignJsonObject(JsonObject obj) {
		cyanServerStatus = obj;
	}

}
