package org.asf.cyan;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.security.ClassTrustEntry;
import org.asf.cyan.security.TrustContainer;
import org.asf.cyan.security.TrustContainerBuilder;
import org.junit.Test;

public class TestTrustContainer {
	@Test
	public void testValidate() throws IOException {
		TrustContainerBuilder builder = new TrustContainerBuilder("test");
		builder.addClass(Modloader.class);
		builder.addClass(TrustContainer.class);
		builder.addClass(TrustContainerBuilder.class);
		builder.addClass(ClassTrustEntry.class);
		builder.addClass(getClass());
		if (!new File("bin/test").exists())
			new File("bin/test").mkdirs();
		
		builder.build().exportContainer(new File("bin/test/test.ctc"));

		TrustContainer container = TrustContainer.importContainer(new File("bin/test/test.ctc"));
		assertTrue(container.validateClass(getClass()) == 0);
		assertTrue(container.validateClass(TrustContainerBuilder.class) == 0);
		assertTrue(container.validateClass(TrustContainer.class) == 0);
		assertTrue(container.validateClass(ClassTrustEntry.class) == 0);
		assertTrue(container.validateClass(Modloader.class) == 0);
	}
}
