package org.asf.cyan.internal.modkitimpl.threading;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;

import modkit.threading.ModThread;
import modkit.threading.ModThread.LoopCall;
import modkit.threading.ThreadQueue;

public class ThreadQueueImplementation extends ThreadQueue {

	public ArrayList<ThreadCall> calls = new ArrayList<ThreadCall>();

	@Override
	public ThreadCall getFirstCall() {
		if (calls.size() != 0) {
			while (true) {
				try {
					if (calls.size() == 0)
						return null;

					ThreadCall call = calls.get(0);
					return call;
				} catch (ConcurrentModificationException e) {
				}
			}
		}
		return null;
	}

	@Override
	public ThreadCall[] getAllCalls() {
		while (true) {
			try {
				return calls.toArray(t -> new ThreadCall[t]);
			} catch (ConcurrentModificationException e) {
			}
		}
	}

	@Override
	public void addCall(ThreadCall call) {
		while (true) {
			try {
				for (ThreadCall call2 : calls) {
					if (call2 instanceof LoopCall) {
						LoopCall c = (LoopCall) call2;
						c.calls.add(call);
						break;
					}
				}
				break;
			} catch (ConcurrentModificationException e) {
			}
		}
		calls.add(call);
	}

	@Override
	protected ThreadCall runFirst(ModThread thread) {
		while (true) {
			try {
				if (calls.size() == 0) {
					return null;
				}

				ThreadCall call = calls.remove(0);
				call.call(thread);
				return call;
			} catch (ConcurrentModificationException e) {
			}
		}
	}

	public void removeLoop() {
		while (true) {
			try {
				for (ThreadCall call : calls) {
					if (call instanceof LoopCall) {
						calls.remove(call);
						return;
					}
				}
				return;
			} catch (ConcurrentModificationException e) {
			}
		}
	}

}
