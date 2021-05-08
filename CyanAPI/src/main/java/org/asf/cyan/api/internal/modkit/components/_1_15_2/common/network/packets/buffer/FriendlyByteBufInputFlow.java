package org.asf.cyan.api.internal.modkit.components._1_15_2.common.network.packets.buffer;

import org.asf.cyan.api.network.ByteFlow;
import net.minecraft.network.FriendlyByteBuf;

public class FriendlyByteBufInputFlow implements ByteFlow {

	private FriendlyByteBuf buffer;
	private int next = -2;

	public FriendlyByteBufInputFlow(FriendlyByteBuf buffer) {
		this.buffer = buffer;
	}

	@Override
	public int read() {
		if (next != -2) {
			int i = next;
			next = -2;
			return i;
		}

		if (buffer.readableBytes() == 0)
			return -1;

		return buffer.readByte();
	}

	public FriendlyByteBuf getBuffer() {
		return buffer;
	}

	@Override
	public boolean hasNext() {
		if (next == -2) {
			next = read();
		}
		return next != -1;
	}

}
