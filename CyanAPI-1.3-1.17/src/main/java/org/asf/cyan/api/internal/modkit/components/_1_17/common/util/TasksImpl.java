package org.asf.cyan.api.internal.modkit.components._1_17.common.util;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;

import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.mods.events.SimpleEvent;
import org.asf.cyan.mods.internal.BaseEventController;

import modkit.events.core.ServerStartupEvent;
import modkit.events.core.ServerShutdownEvent;
import modkit.events.objects.core.ServerEventObject;
import modkit.util.server.ITickTask;
import modkit.util.server.Tasks;

public class TasksImpl extends Tasks implements IModKitComponent {

	private ArrayList<TickInfo> tasks = new ArrayList<TickInfo>();

	private class TickInfo {
		public ITickTask task;

		public int delay = -1;
		public int interval = -1;
		public int remaining = -1;
		public int intervalT = 0;
	}

	@Override
	public void initializeComponent() {
		implementation = this;
		BaseEventController.addEventContainer(this);
	}

	@SimpleEvent(ServerShutdownEvent.class)
	private void stopServer(ServerEventObject event) {
		tasks.clear();
	}

	@SimpleEvent(ServerStartupEvent.class)
	private void startServer(ServerEventObject event) {
		event.getServer().addTickable(() -> {
			while (true) {
				try {
					for (TickInfo tick : new ArrayList<TickInfo>(tasks)) {
						if (tick.delay > 0) {
							tick.delay--;
							continue;
						}

						if (tick.intervalT > 0 && tick.interval > 0) {
							tick.intervalT--;
							if (tick.intervalT != 0)
								continue;
						}
						if (tick.intervalT == 0 && tick.interval >= 0) {
							tick.intervalT = tick.interval;
						}

						tick.task.accept(event.getServer());

						if (tick.remaining == 0 || tick.interval == -1) {
							tasks.remove(tick);
						} else if (tick.remaining > 0) {
							tick.remaining--;
						}
					}
					break;
				} catch (ConcurrentModificationException e) {
				}
			}
		});
	}

	@Override
	protected void cancelTask(ITickTask task) {
		for (TickInfo tick : new ArrayList<TickInfo>(tasks)) {
			if (tick.task == task) {
				tasks.remove(tick);
			}
		}
	}

	@Override
	protected void scheduleOneshot(ITickTask task) {
		TickInfo t = new TickInfo();
		t.task = task;
		tasks.add(t);
	}

	@Override
	protected void scheduleDelayed(ITickTask task, int ticks) {
		TickInfo t = new TickInfo();
		t.task = task;
		t.delay = ticks;
		tasks.add(t);
	}

	@Override
	protected void scheduleRepeating(ITickTask task) {
		TickInfo t = new TickInfo();
		t.task = task;
		t.interval = 0;
		tasks.add(t);
	}

	@Override
	protected void scheduleDelayedRepeating(ITickTask task, int ticks) {
		TickInfo t = new TickInfo();
		t.task = task;
		t.delay = ticks;
		t.interval = 0;
		tasks.add(t);
	}

	@Override
	protected void scheduleInterval(ITickTask task, int intervalTicks) {
		TickInfo t = new TickInfo();
		t.task = task;
		t.interval = intervalTicks;
		tasks.add(t);
	}

	@Override
	protected void scheduleDelayedInterval(ITickTask task, int ticks, int intervalTicks) {
		TickInfo t = new TickInfo();
		t.task = task;
		t.delay = ticks;
		t.interval = intervalTicks;
		tasks.add(t);
	}

	@Override
	protected void scheduleCountdown(ITickTask task, int intervalTicks, int lengthTicks) {
		TickInfo t = new TickInfo();
		t.task = task;
		t.interval = intervalTicks;
		t.remaining = lengthTicks;
		tasks.add(t);
	}

	@Override
	protected void scheduleDelayedCountdown(ITickTask task, int ticks, int intervalTicks, int lengthTicks) {
		TickInfo t = new TickInfo();
		t.task = task;
		t.delay = ticks;
		t.interval = intervalTicks;
		t.remaining = lengthTicks;
		tasks.add(t);
	}

}
