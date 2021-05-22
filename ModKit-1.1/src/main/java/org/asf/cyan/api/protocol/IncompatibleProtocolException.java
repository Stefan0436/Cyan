package org.asf.cyan.api.protocol;

/**
 * 
 * Exception thrown for incompatible ModKit Modloaders
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 * @since 1.1
 *
 */
public class IncompatibleProtocolException extends RuntimeException {

	/**
	 * Instantiates the exception
	 * 
	 * @param message Error message
	 */
	public IncompatibleProtocolException(String message) {
		super(message);
	}

	private static final long serialVersionUID = 1L;

}
