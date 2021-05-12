package org.asf.cyan.loader.eventbus;

import java.util.Iterator;

import org.asf.cyan.api.events.core.EventBus.ResultContainer;
import org.asf.cyan.api.events.core.IEventListener;

public class CyanEventList implements Iterable<CyanEventList.CELEntry> {
	public class CELEntry {
		public ResultContainer result;
		public IEventListener listener;
		public CELEntry nextEntry;
	}

	public class CELIter implements Iterator<CELEntry> {
		public CELEntry nextEntry;

		public CELIter(CELEntry entry) {
			nextEntry = entry;
		}

		@Override
		public boolean hasNext() {
			return nextEntry != null;
		}

		@Override
		public CELEntry next() {
			CELEntry next = nextEntry;
			if (next != null)
				nextEntry = nextEntry.nextEntry;

			return next;
		}
	}

	private CELEntry mainEntry;
	private CELEntry currentEntry;

	@Override
	public Iterator<CELEntry> iterator() {
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
		if (mainEntry == null) {
			mainEntry = new CELEntry();
			mainEntry.listener = listener;
			currentEntry = mainEntry;
			return;
		}

		currentEntry.nextEntry = new CELEntry();
		currentEntry.nextEntry.listener = listener;
		currentEntry = currentEntry.nextEntry;
	}

}
