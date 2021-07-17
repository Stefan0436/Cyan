package org.asf.cyan.api.internal.modkit.transformers._1_17.client.network;

import java.lang.reflect.Field;

import org.asf.cyan.api.internal.modkit.transformers._1_17.common.network.ServerStatusInterface;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.Reflect;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.protocol.status.ClientStatusPacketListener;

@FluidTransformer
@TargetClass(target = "net.minecraft.network.protocol.status.ClientboundStatusResponsePacket")
public class ClientboundStatusResponsePacketModification {

	@InjectAt(location = InjectLocation.HEAD)
	public void handle(
			@TargetType(target = "net.minecraft.network.protocol.status.ClientStatusPacketListener") ClientStatusPacketListener listener) {
		for (Field f : listener.getClass().getDeclaredFields()) {
			if (f.getType().getTypeName().equals(ServerData.class.getTypeName())) {
				f.setAccessible(true);
				try {
					ServerDataAccessor data = (ServerDataAccessor) f.get(listener);
					data.cyanAssignJsonObject(getStatus().getJson());
				} catch (IllegalArgumentException | IllegalAccessException e) {
				}
			}
		}
	}

	@Reflect
	@TargetType(target = "net.minecraft.network.protocol.status.ServerStatus")
	public ServerStatusInterface getStatus() {
		return null;
	}

}
