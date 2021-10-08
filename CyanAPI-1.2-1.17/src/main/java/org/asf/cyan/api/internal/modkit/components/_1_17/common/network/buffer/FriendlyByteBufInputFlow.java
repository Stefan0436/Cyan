package org.asf.cyan.api.internal.modkit.components._1_17.common.network.buffer;

import modkit.network.ByteFlow;
import net.minecraft.network.FriendlyByteBuf;

public class FriendlyByteBufInputFlow implements ByteFlow {

	private FriendlyByteBuf buffer;

	private byte next = -1;
	private boolean end = false;
	private boolean hasNext = false;

	public FriendlyByteBufInputFlow(FriendlyByteBuf buffer) {
		this.buffer = buffer;
	}

	@Override
	public byte read() {
		if (end)
			return -1;

		if (hasNext) {
			hasNext = false;
			byte b = next;
			next = -1;
			return b;
		}

		if (buffer.readableBytes() == 0) {
			end = true;
			return -1;
		}

		return buffer.readByte();
	}

	public FriendlyByteBuf getBuffer() {
		return buffer;
	}

	@Override
	public boolean hasNext() {
		if (!hasNext) {
			next = read();
			hasNext = true;
		}

		return !end;
	}

}
