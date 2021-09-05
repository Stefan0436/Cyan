package org.asf.cyan.api.internal.modkit.components._1_17.common.network.buffer;

import io.netty.buffer.Unpooled;
import modkit.network.OutputFlow;
import net.minecraft.network.FriendlyByteBuf;

public class FriendlyByteBufOutputFlow implements OutputFlow {

	private FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
	
	@Override
	public void write(int data) {
		buffer.writeByte(data);
	}

	@Override
	public void close() {
	}
	
	public FriendlyByteBuf toBuffer() {
		return buffer;
	}

}
