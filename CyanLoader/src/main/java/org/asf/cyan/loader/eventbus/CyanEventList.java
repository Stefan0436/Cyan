package org.asf.cyan.loader.eventbus;

import java.util.Iterator;

import org.asf.cyan.api.events.core.IEventListener;

class CyanEventList implements Iterable<IEventListener> {
	private class CELEntry {
		public IEventListener listener;
		public CELEntry nextEntry;
	}

	private class CELIter implements Iterator<IEventListener> {
		public CELEntry nextEntry;

		public CELIter(CELEntry entry) {
			nextEntry = entry;
		}

		@Override
		public boolean hasNext() {
			return nextEntry != null;
		}

		@Override
		public IEventListener next() {
			CELEntry next = nextEntry;
			if (next != null)
				nextEntry = nextEntry.nextEntry;

			return next.listener;
		}
	}

	private CELEntry mainEntry;

	public long length() {
		long value = 0;
		CELEntry cEntry = mainEntry;

		while (cEntry != null) {
			if (value + 1 != Long.MAX_VALUE) {
				value++;
				cEntry = cEntry.nextEntry;
			} else {
				value = -1;
				break;
			}
		}

		return value;
	}

	public IEventListener getAt(long index) {
		if (length() == -1 || index < length()) {
			CELEntry cEntry = mainEntry;
			long ind = index;
			while (ind != 0) {
				cEntry = cEntry.nextEntry;
				if (cEntry == null) {
					return null;
				}
				ind--;
			}
			return cEntry.listener;
		} else {
			throw new IndexOutOfBoundsException(index + " is outside of the event list!");
		}
	}

	public IEventListener getLast() {
		return getLastInternal().listener;
	}

	private CELEntry getLastInternal() {
		CELEntry cEntry = mainEntry;
		while (cEntry != null) {
			if (cEntry.nextEntry == null)
				break;

			cEntry = cEntry.nextEntry;
		}
		return cEntry;
	}

	@Override
	public Iterator<IEventListener> iterator() {
		return new CELIter(mainEntry);
	}
	
	public boolean contains(IEventListener listener) {
		CELEntry entry = mainEntry;
		while (entry != null) {			
			if (entry.listener.getListenerName().equals(listener.getListenerName())) {
				return true;
			}
			entry = entry.nextEntry;
		}
		return false;
	}

	public void add(IEventListener listener) {
		CELEntry entry = getLastInternal();

		if (mainEntry == null) {
			entry = new CELEntry();
			entry.listener = listener;
			mainEntry = entry;
			return;
		}

		entry.nextEntry = new CELEntry();
		entry = entry.nextEntry;

		entry.listener = listener;
	}

	public IEventListener[] toArray() {
		CELEntry cEntry = mainEntry;

		int length = 0;
		while (cEntry != null) {
			if (length + 1 != Long.MAX_VALUE) {
				length++;
				cEntry = cEntry.nextEntry;
			} else {
				break;
			}
		}

		cEntry = mainEntry;

		IEventListener[] entries = new IEventListener[length];

		for (int i = 0; i < length; i++) {
			entries[i] = cEntry.listener;
			cEntry = cEntry.nextEntry;
		}

		return entries;
	}
}
