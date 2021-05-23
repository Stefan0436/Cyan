package modkit.network;

/**
 * 
 * Network Byte Output Flow
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public interface OutputFlow {
	public void write(int data);
	public void close();
}