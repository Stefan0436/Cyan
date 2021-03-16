package org.asf.cyan;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.cyan.api.modloader.IModloaderComponent;
import org.asf.cyan.api.modloader.TargetModloader;

@TargetModloader(CyanLoader.class)
public class CyanErrorHandlers implements IModloaderComponent {
	private static boolean ready = false;
	private static CyanErrorOut outp;
	
	public void attach() {
		outp = new CyanErrorOut();
		System.setErr(new PrintStream(outp));
		outp.setReady();
	}

	public static class CyanErrorOut extends OutputStream {
		private Logger logger;
		private StringBuilder buffer = new StringBuilder();

		@Override
		public void write(int arg0) throws IOException {
			if (Character.valueOf((char) arg0) == '\r')
				return;
			
			buffer.append(Character.valueOf((char) arg0));
			if (ready && Character.valueOf((char) arg0) == '\n') {
				flush();
			}
		}
		
		@Override
		public void flush() {
			if (ready) {
				String buffer = this.buffer.toString();
				for (String line : buffer.split("\n")) {
					logger.error(line);
				}
				this.buffer = new StringBuilder();
			}
		}
		
		public void setReady() {
			if (!ready) {
				logger = LogManager.getLogger("STDERR");
				flush();
				ready = true;				
			}
		}
	}
}
