package modkit.network;

/**
 * 
 * Network Byte Output Flow
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public interface OutputFlow {
	public void write(byte data);

	@Deprecated
	public default void write(int data) {
		write((byte) data);
	}

	public void close();
}
