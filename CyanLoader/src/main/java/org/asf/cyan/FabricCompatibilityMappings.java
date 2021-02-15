package org.asf.cyan;

import java.lang.reflect.InvocationTargetException;

import javax.xml.parsers.DocumentBuilderFactory;

import org.asf.cyan.api.cyanloader.CyanSide;
import org.asf.cyan.fluid.mappings.MAPTYPE;
import org.asf.cyan.fluid.mappings.Mapping;

/**
 * 
 * Remaps the Yarn mappings to allow Fabric to run Cyan, credits to FabricMC 
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
class FabricCompatibilityMappings extends CompatibilityMappings {
	
	// Use: http://maven.modmuss50.me/net/fabricmc/yarn/maven-metadata.xml to find the right mappings version
	// URL: http://maven.modmuss50.me/net/fabricmc/yarn/%version%+%build%/yarn-%version%+%build%-tiny.gz
	
	// Gonna need to write a parser though
	
	public FabricCompatibilityMappings(Mapping<?> mappings, CyanSide side) {
		try {
			this.mappings = new Mapping<?>[] {
				createMapping("", "", MAPTYPE.CLASS, null)
			};
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
		}
	}
	
	public static YarnMappings downloadYarnMappings(String version) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		return null;
	}
}
