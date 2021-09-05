package org.asf.cyan.api.internal.modkit.transformers._1_16.common.network;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import com.google.gson.JsonObject;

@FluidTransformer
@TargetClass(target = "net.minecraft.network.protocol.status.ServerStatus")
public class ServerStatusModification implements ServerStatusInterface {

	private JsonObject rawJsonCyan;

	@Override
	public JsonObject getJson() {
		return rawJsonCyan;
	}

	@Override
	public void setJson(JsonObject obj) {
		rawJsonCyan = obj;
	}

}
