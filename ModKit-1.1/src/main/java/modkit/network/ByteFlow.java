package modkit.network;

/**
 * 
 * Network Byte Flow
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public interface ByteFlow {
	public boolean hasNext();

	public int read();
}
