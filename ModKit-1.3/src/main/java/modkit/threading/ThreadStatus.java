package modkit.threading;

/**
 * 
 * Mod Thread Status Enum
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public enum ThreadStatus {

	/**
	 * The thread is running
	 */
	RUNNING,

	/**
	 * The thread has been killed
	 */
	KILLED,

	/**
	 * The thread has been suspended and can be resumed
	 */
	SUSPENDED,

	/**
	 * The thread has been killed and saved in memory for future use
	 */
	SAVED

}
