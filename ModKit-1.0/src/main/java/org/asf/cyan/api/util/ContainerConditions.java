package org.asf.cyan.api.util;

/**
 *
 * Container Condition Interface
 *
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public interface ContainerConditions {
	public boolean applies();
	
	public static final ContainerConditions COMMON = new CommonContainerCondition();
	public static final ContainerConditions CLIENT = new ClientContainerCondition();
	public static final ContainerConditions SERVER = new ServerContainerCondition();
	public static final ContainerConditions FIXERS = new DataFixerContainerCondition();
}
