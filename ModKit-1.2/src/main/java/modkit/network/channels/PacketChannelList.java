package modkit.network.channels;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

public class PacketChannelList implements Iterable<PacketChannel> {
	public class PacketProcessorEntry {
		public PacketChannel channel;
		public PacketProcessorEntry next;
	}

	public PacketProcessorEntry first;
	public PacketProcessorEntry current;

	public void add(PacketChannel channel) {
		if (current == null) {
			first = new PacketProcessorEntry();
			current = first;
			first.channel = channel;
			return;
		}

		current.next = new PacketProcessorEntry();
		current = current.next;
		current.channel = channel;
	}

	public void add(Class<? extends PacketChannel> channel) {
		try {
			Constructor<? extends PacketChannel> ctor = channel.getDeclaredConstructor();
			ctor.setAccessible(true);
			add(ctor.newInstance());
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException("Failed to instantiate channel " + channel.getTypeName(), e);
		}
	}

	@Override
	public Iterator<PacketChannel> iterator() {
		Iter i = new Iter();
		i.current = first;
		return i;
	}

	public class Iter implements Iterator<PacketChannel> {
		public PacketProcessorEntry current;

		@Override
		public boolean hasNext() {
			return current != null;
		}

		@Override
		public PacketChannel next() {
			PacketProcessorEntry ent = current;
			current = current.next;
			return ent.channel;
		}

	}
}
