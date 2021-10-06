package modkit.util.server;

import org.asf.cyan.mods.events.IEventListenerContainer;

import net.minecraft.server.MinecraftServer;

/**
 * 
 * Server tick utility class -- register single-time, delayed, repeatable and
 * interval tick tasks
 * 
 * @since ModKit 1.3
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public abstract class Tasks implements IEventListenerContainer {

	private static class WrapperTask implements ITickTask {

		private Runnable task;

		public WrapperTask(Runnable task) {
			this.task = task;
		}

		@Override
		public void accept(MinecraftServer server) {
			task.run();
		}
	}

	protected static Tasks implementation;

	protected abstract void cancelTask(ITickTask task);

	protected abstract void scheduleOneshot(ITickTask task);

	protected abstract void scheduleDelayed(ITickTask task, int ticks);

	protected abstract void scheduleRepeating(ITickTask task);

	protected abstract void scheduleDelayedRepeating(ITickTask task, int ticks);

	protected abstract void scheduleInterval(ITickTask task, int intervalTicks);

	protected abstract void scheduleDelayedInterval(ITickTask task, int ticks, int intervalTicks);

	protected abstract void scheduleCountdown(ITickTask task, int intervalTicks, int lengthTicks);

	protected abstract void scheduleDelayedCountdown(ITickTask task, int ticks, int intervalTicks, int lengthTicks);

	/**
	 * Cancels the given task
	 * 
	 * @param task Tick task to cancel
	 */
	public static void cancel(ITickTask task) {
		implementation.cancelTask(task);
	}

	/**
	 * Schedules a one-shot tick task to be called on the next server tick.
	 * 
	 * @param task Tick task to schedule
	 */
	public static void oneshot(ITickTask task) {
		implementation.scheduleOneshot(task);
	}

	/**
	 * Schedules a one-shot tick task to be called on the next server tick.
	 * 
	 * @param task Tick task to schedule
	 */
	public static void oneshot(Runnable task) {
		oneshot(new WrapperTask(task));
	}

	/**
	 * Schedules a one-shot tick task to be called after a given amount of ticks.
	 * 
	 * @param ticks The amount of ticks to wait before calling the task
	 * @param task  Tick task to schedule
	 */
	public static void delayed(int ticks, ITickTask task) {
		implementation.scheduleDelayed(task, ticks);
	}

	/**
	 * Schedules a one-shot tick task to be called after a given amount of ticks.
	 * 
	 * @param ticks The amount of ticks to wait before calling the task
	 * @param task  Tick task to schedule
	 */
	public static void delayed(int ticks, Runnable task) {
		delayed(ticks, new WrapperTask(task));
	}

	/**
	 * Schedules a tick task that is called on every server tick.
	 * 
	 * @param task Tick task to schedule
	 * @return TickTask instance (to cancel)
	 */
	public static ITickTask repeating(ITickTask task) {
		implementation.scheduleRepeating(task);
		return task;
	}

	/**
	 * Schedules a tick task that is called on every server tick.
	 * 
	 * @param task Tick task to schedule.
	 * @return TickTask instance (to cancel)
	 */
	public static ITickTask repeating(Runnable task) {
		return repeating(new WrapperTask(task));
	}

	/**
	 * Schedules a tick task that is called on every server tick. (delayed start, no
	 * delay after starting)
	 * 
	 * @param ticks The amount of ticks to wait before calling the task
	 * @param task  Tick task to schedule
	 * @return TickTask instance (to cancel)
	 */
	public static ITickTask delayedRepeating(int ticks, ITickTask task) {
		implementation.scheduleDelayedRepeating(task, ticks);
		return task;
	}

	/**
	 * Schedules a tick task that is called on every server tick. (delayed start, no
	 * delay after starting)
	 * 
	 * @param ticks The amount of ticks to wait before calling the task
	 * @param task  Tick task to schedule.
	 * @return TickTask instance (to cancel)
	 */
	public static ITickTask delayedRepeating(int ticks, Runnable task) {
		return delayedRepeating(ticks, new WrapperTask(task));
	}

	/**
	 * Schedules a tick task that is repeating with a specified delay.
	 * 
	 * @param intervalTicks The amount of ticks to wait before re-running the task
	 * @param task          Tick task to schedule
	 * @return TickTask instance (to cancel)
	 */
	public static ITickTask interval(int intervalTicks, ITickTask task) {
		implementation.scheduleInterval(task, intervalTicks);
		return task;
	}

	/**
	 * Schedules a tick task that is repeating with a specified delay.
	 * 
	 * @param intervalTicks The amount of ticks to wait before re-running the task
	 * @param task          Tick task to schedule.
	 * @return TickTask instance (to cancel)
	 */
	public static ITickTask interval(int intervalTicks, Runnable task) {
		return interval(intervalTicks, new WrapperTask(task));
	}

	/**
	 * Schedules a delayed tick task that is repeating with a specified delay.
	 * 
	 * @param ticks         The amount of ticks to wait before calling the task
	 * @param intervalTicks The amount of ticks to wait before re-running the task
	 * @param task          Tick task to schedule
	 * @return TickTask instance (to cancel)
	 */
	public static ITickTask delayedInterval(int ticks, int intervalTicks, ITickTask task) {
		implementation.scheduleDelayedInterval(task, ticks, intervalTicks);
		return task;
	}

	/**
	 * Schedules a delayed tick task that is repeating with a specified delay.
	 * 
	 * @param ticks         The amount of ticks to wait before calling the task
	 * @param intervalTicks The amount of ticks to wait before re-running the task
	 * @param task          Tick task to schedule.
	 * @return TickTask instance (to cancel)
	 */
	public static ITickTask delayedInterval(int ticks, int intervalTicks, Runnable task) {
		return delayedInterval(ticks, intervalTicks, new WrapperTask(task));
	}

	/**
	 * Schedules a tick task that is repeating with a specified delay for only a
	 * specific amount ticks before stopping.
	 * 
	 * @param intervalTicks The amount of ticks to wait before re-running the task
	 * @param lengthTicks   The amount of times to run this task
	 * @param task          Tick task to schedule
	 * @return TickTask instance (to cancel)
	 */
	public static ITickTask countdown(int intervalTicks, int lengthTicks, ITickTask task) {
		implementation.scheduleCountdown(task, intervalTicks, lengthTicks);
		return task;
	}

	/**
	 * Schedules a tick task that is repeating with a specified delay for only a
	 * specific amount ticks before stopping.
	 * 
	 * @param intervalTicks The amount of ticks to wait before re-running the task
	 * @param lengthTicks   The amount of times to run this task
	 * @param task          Tick task to schedule.
	 * @return TickTask instance (to cancel)
	 */
	public static ITickTask countdown(int intervalTicks, int lengthTicks, Runnable task) {
		return countdown(intervalTicks, lengthTicks, new WrapperTask(task));
	}

	/**
	 * Schedules a delayed tick task that is repeating with a specified delay for
	 * only a specific amount ticks before stopping.
	 * 
	 * @param ticks         The amount of ticks to wait before calling the task
	 * @param intervalTicks The amount of ticks to wait before re-running the task
	 * @param lengthTicks   The amount of times to run this task
	 * @param task          Tick task to schedule
	 * @return TickTask instance (to cancel)
	 */
	public static ITickTask delayedCountdown(int ticks, int intervalTicks, int lengthTicks, ITickTask task) {
		implementation.scheduleDelayedCountdown(task, ticks, intervalTicks, lengthTicks);
		return task;
	}

	/**
	 * Schedules a delayed tick task that is repeating with a specified delay for
	 * only a specific amount ticks before stopping.
	 * 
	 * @param ticks         The amount of ticks to wait before calling the task
	 * @param intervalTicks The amount of ticks to wait before re-running the task
	 * @param lengthTicks   The amount of times to run this task
	 * @param task          Tick task to schedule.
	 * @return TickTask instance (to cancel)
	 */
	public static ITickTask delayedCountdown(int ticks, int intervalTicks, int lengthTicks, Runnable task) {
		return delayedCountdown(ticks, intervalTicks, lengthTicks, new WrapperTask(task));
	}

}
