package org.asf.cyan.core.logger;

import java.io.PrintStream;
import java.io.Serializable;

import static org.fusesource.jansi.Ansi.*;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.asf.cyan.core.CyanCore;
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.Ansi.Attribute;

@Plugin(name = "ProcessingOutputAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class ProcessingOutputAppender extends AbstractAppender {
	private PrintStream output;

	private final PrintStream realError = System.err;
	private final PrintStream realOutput = System.out;

	@SuppressWarnings("deprecation")
	protected ProcessingOutputAppender(String name, Filter filter, Layout<? extends Serializable> layout,
			ConsoleAppender.Target target) {
		super(name, filter, layout);
		if (target == Target.SYSTEM_ERR) {
			output = realError;
		} else if (target == Target.SYSTEM_OUT) {
			output = realOutput;
		}
		AnsiConsole.systemInstall();
	}

	@Override
	public void append(LogEvent event) {
		String msg = new String(this.getLayout().toByteArray(event));
		if (CyanCore.isIdeMode()) {
			msg = msg.replaceAll("§.", "");
		} else if (msg.contains("§")) {
			String newMsg = "";
			boolean escape = false;
			boolean check = false;
			for (char ch : msg.toCharArray()) {
				if (ch == '\\' && !escape) {
					escape = true;
					continue;
				}
				if (check) {
					check = false;
					if (ch == '0') {
						newMsg += ansi().fgBlack();
					} else if (ch == '1') {
						newMsg += ansi().fgBlue();
					} else if (ch == '2') {
						newMsg += ansi().fgGreen();
					} else if (ch == '3') {
						newMsg += ansi().fgCyan();
					} else if (ch == '4') {
						newMsg += ansi().fgRed();
					} else if (ch == '5') {
						newMsg += ansi().fgMagenta();
					} else if (ch == '6') {
						newMsg += ansi().fgYellow();
					} else if (ch == '7') {
						newMsg += ansi().fg(Color.WHITE);
					} else if (ch == '8') {
						newMsg += ansi().fgBrightBlack();
					} else if (ch == '9') {
						newMsg += ansi().fgBlue();
					} else if (ch == 'a') {
						newMsg += ansi().fgBrightGreen();
					} else if (ch == 'b') {
						newMsg += ansi().fgBrightCyan();
					} else if (ch == 'c') {
						newMsg += ansi().fgBrightRed();
					} else if (ch == 'd') {
						newMsg += ansi().fgBrightMagenta();
					} else if (ch == 'e') {
						newMsg += ansi().fgBrightYellow();
					} else if (ch == 'f') {
						newMsg += ansi().fgBright(Color.WHITE);
					} else if (ch == 'l') {
						newMsg += ansi().bold();
					} else if (ch == 'm') {
						newMsg += ansi().a(Attribute.STRIKETHROUGH_ON);
					} else if (ch == 'n') {
						newMsg += ansi().a(Attribute.UNDERLINE);
					} else if (ch == 'o') {
						newMsg += ansi().a(Attribute.ITALIC);
					} else if (ch == 'r') {
						newMsg += ansi().reset();
					} else if (ch != 'k') {
						newMsg += "§" + ch;
					}
					continue;
				}
				if (ch == '§') {
					check = true;
				} else {
					newMsg += ch;
				}
				escape = false;
			}
			msg = newMsg;
			msg += ansi().fgDefault();
		}
		output.print(msg);
	}

	@SuppressWarnings("unchecked")
	@PluginFactory
	public static ProcessingOutputAppender createAppender(@PluginAttribute("name") String name,
			@SuppressWarnings("rawtypes") @PluginElement("Layout") Layout layout,
			@PluginElement("Filters") Filter filter, @PluginAttribute("target") ConsoleAppender.Target target) {
		return new ProcessingOutputAppender(name, filter, layout, target);
	}

}
