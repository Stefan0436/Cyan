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
		current.executes(t -> {
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
	public void attachPermission(String perm, int level) {
		current = current.requires(t -> {
			try {
				return PermissionManager.getInstance().hasPermission(t.getEntityOrException(), perm);
			} catch (CommandSyntaxException ex) {
				if (level == 0 || level == -1)
					return true;
				else
					return t.hasPermission(level);
			}
		});
	}

	@Override
	@SuppressWarnings("unchecked")
	public LiteralArgumentBuilder<CommandSourceStack> build(LiteralArgumentBuilder<CommandSourceStack> newOwner) {
		boolean hasNext = true;
		int ind = 0;
		while (hasNext) {
			if (ind >= nodes.size())
				break;
			if (nodes.get(ind) instanceof Integer) {
				ind++;
				continue;
			}
			hasNext = false;
			
			ArgumentBuilder<CommandSourceStack, ?> nd = (ArgumentBuilder<CommandSourceStack, ?>) nodes.get(ind);
			boolean first = true;
			for (Object obj : nodes) {
				if (obj instanceof Integer) {
					hasNext = true;
					ind++;
					break;
				}
				ArgumentBuilder<CommandSourceStack, ?> node = (ArgumentBuilder<CommandSourceStack, ?>) obj;
				if (first) {
					first = false;
					continue;
				}
				ind++;
				nd = nd.then(node);
			}
			newOwner = newOwner.then(nd);
		}
		nodes.clear();
		current = null;
		return newOwner;
	}

	@Override
	public void attachExecutionEngine(Function<CommandExecutionContext, Integer> engine) {
		current.executes(t -> {
			return engine.apply(new CommandExecutionContextImpl(t));
		});
	}

	@Override
	public void closeEntry() {
		if (current == null)
			return;
		nodes.add(-1);
		current = null;
	}

}
