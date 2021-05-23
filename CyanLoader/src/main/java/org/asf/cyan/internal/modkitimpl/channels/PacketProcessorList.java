package org.asf.cyan.internal.modkitimpl.channels;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import modkit.network.channels.AbstractPacketProcessor;

public class PacketProcessorList implements Iterable<AbstractPacketProcessor> {
	public class PacketProcessorEntry {
		public AbstractPacketProcessor processor;
		public PacketProcessorEntry next;
	}

	public PacketProcessorEntry first;
	public PacketProcessorEntry current;

	public void add(AbstractPacketProcessor processor) {
		if (current == null) {
			first = new PacketProcessorEntry();
			current = first;
			first.processor = processor;
			return;
		}

		current.next = new PacketProcessorEntry();
		current = current.next;
		current.processor = processor;
	}

	public void add(Class<? extends AbstractPacketProcessor> processor) {
		try {
			Constructor<? extends AbstractPacketProcessor> ctor = processor.getDeclaredConstructor();
			ctor.setAccessible(true);
			add(ctor.newInstance());
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException("Failed to instantiate processor " + processor.getTypeName(), e);
		}
	}

	@Override
	public Iterator<AbstractPacketProcessor> iterator() {
		Iter i = new Iter();
		i.current = first;
		return i;
	}

	public class Iter implements Iterator<AbstractPacketProcessor> {
		public PacketProcessorEntry current;

		@Override
		public boolean hasNext() {
			return current != null;
		}

		@Override
		public AbstractPacketProcessor next() {
			PacketProcessorEntry ent = current;
			current = current.next;
			return ent.processor;
		}

	}
}
