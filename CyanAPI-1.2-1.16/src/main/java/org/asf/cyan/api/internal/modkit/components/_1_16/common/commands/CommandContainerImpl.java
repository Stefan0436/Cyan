package org.asf.cyan.api.internal.modkit.components._1_16.common.commands;

import java.util.ArrayList;
import java.util.function.Function;

import org.asf.cyan.api.internal.IModKitComponent;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import modkit.commands.Command;
import modkit.commands.Command.CommandContainer;
import modkit.commands.Command.CommandExecutionContext;
import modkit.permissions.PermissionManager;
import net.minecraft.commands.CommandSourceStack;

public class CommandContainerImpl extends CommandContainer implements IModKitComponent {

	private Command owner;
	private ArrayList<Byte> earlyTasks = new ArrayList<Byte>();
	private ArrayList<Object> earlyTaskArguments = new ArrayList<Object>();
	private ArrayList<Object> nodes = new ArrayList<Object>();
	private ArgumentBuilder<CommandSourceStack, ?> current;

	public CommandContainerImpl() {
	}

	public CommandContainerImpl(Command owner) {
		this.owner = owner;
	}

	@Override
	protected CommandContainer newInstance(Command owner) {
		return new CommandContainerImpl(owner);
	}

	@Override
	public void attachExecutionEngine() {
		if (current == null) {
			earlyTasks.add((byte) 0);
			earlyTaskArguments.add(null);
			return;
		}
		current = current.executes(t -> {
			return owner.execute(new CommandExecutionContextImpl(t));
		});
	}

	@Override
	public void initializeComponent() {
		implementation = this;
	}

	@Override
	public void add(ArgumentBuilder<CommandSourceStack, ?> node) {
		nodes.add(node);
		current = node;
	}

	@Override
	public ArgumentBuilder<CommandSourceStack, ?> getCurrent() {
		return current;
	}

	@Override
	public void attachPermission() {
		attachPermission(owner.getPermission(), owner.fallbackMinOpLevel());
	}

	@Override
	@SuppressWarnings("unchecked")
	public void attachPermission(String perm, int level) {
		if (current == null) {
			earlyTasks.add((byte) 2);
			earlyTaskArguments.add(new Object[] { perm, level });
			return;
		}
		current = (ArgumentBuilder<CommandSourceStack, ?>) nodes.set(nodes.indexOf(current), current.requires(t -> {
			try {
				return PermissionManager.getInstance().hasPermission(t.getEntityOrException(), perm);
			} catch (CommandSyntaxException ex) {
				if (level == 0 || level == -1)
					return true;
				else
					return t.hasPermission(level);
			}
		}));
	}

	@Override
	@SuppressWarnings("unchecked")
	public LiteralArgumentBuilder<CommandSourceStack> build(LiteralArgumentBuilder<CommandSourceStack> newOwner) {
		while (!earlyTasks.isEmpty()) {
			byte b = earlyTasks.remove(0);
			Object a = earlyTaskArguments.remove(0);

			switch (b) {
			case 0:
				newOwner = newOwner.executes(t -> {
					return owner.execute(new CommandExecutionContextImpl(t));
				});
				break;
			case 1:
				Function<CommandExecutionContext, Integer> engine = (Function<CommandExecutionContext, Integer>) a;
				newOwner = newOwner.executes(t -> {
					return engine.apply(new CommandExecutionContextImpl(t));
				});
				break;
			case 2:
				String perm = ((Object[]) a)[0].toString();
				int level = (int) ((Object[]) a)[1];
				newOwner = newOwner.requires(t -> {
					try {
						return PermissionManager.getInstance().hasPermission(t.getEntityOrException(), perm);
					} catch (CommandSyntaxException ex) {
						if (level == 0 || level == -1)
							return true;
						else
							return t.hasPermission(level);
					}
				});
				break;
			}
		}

		boolean hasNext = true;
		int ind = 0;
		ArrayList<Object> nodesb = new ArrayList<Object>(nodes);

		while (hasNext) {
			if (ind >= nodesb.size())
				break;
			if (nodesb.get(ind) instanceof Integer) {
				ind++;
				continue;
			}
			hasNext = false;

			ArgumentBuilder<CommandSourceStack, ?> nd = (ArgumentBuilder<CommandSourceStack, ?>) nodesb.get(ind);
			ArgumentBuilder<CommandSourceStack, ?> ndm = nd;
			ArgumentBuilder<CommandSourceStack, ?> ndmf = nd;

			int depth = 0;
			boolean first = true;
			for (Object obj : new ArrayList<Object>(nodes)) {
				nodes.remove(obj);
				if (obj instanceof Integer) {
					if (depth > 1)
						nd = nd.then(ndmf);
					newOwner = newOwner.then(nd);
					hasNext = true;
					ind++;
					break;
				}

				ArgumentBuilder<CommandSourceStack, ?> node = (ArgumentBuilder<CommandSourceStack, ?>) obj;
				if (first) {
					first = false;
					continue;
				}

				depth++;
				ind++;
				if (depth > 1) {
					ndm = ndm.then(node);
					if (depth == 2)
						ndmf = ndm;
					else
						ndmf = ndmf.then(ndm);
				} else
					nd = nd.then(node);
				ndm = node;
			}
			if (depth > 1)
				nd = nd.then(ndmf);
			newOwner = newOwner.then(nd);
		}
		nodes.clear();
		current = null;
		return newOwner;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void attachExecutionEngine(Function<CommandExecutionContext, Integer> engine) {
		if (current == null) {
			earlyTasks.add((byte) 1);
			earlyTaskArguments.add(engine);
			return;
		}
		current = (ArgumentBuilder<CommandSourceStack, ?>) nodes.set(nodes.indexOf(current), current.executes(t -> {
			return engine.apply(new CommandExecutionContextImpl(t));
		}));
	}

	@Override
	public void closeEntry() {
		if (current == null)
			return;
		nodes.add(-1);
		current = null;
	}

}
