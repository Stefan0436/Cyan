package modkit.util.server;

import java.util.function.Consumer;

import net.minecraft.server.MinecraftServer;

/**
 * 
 * Tick-task interface
 * 
 * @since ModKit 1.3
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public interface ITickTask extends Consumer<MinecraftServer> {

	/**
	 * Runs the tick task
	 * 
	 * @param server Minecraft server ticking the task
	 */
	public void accept(MinecraftServer server);

}
