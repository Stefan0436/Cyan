package org.asf.cyan.backends;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.asf.cyan.api.packet.PacketParser;

public class NewsMessage {
	public String author;
	public String authorImagePath;
	public String title;
	public String message;
	public Date time;

	public void setDate(String date) throws IOException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		try {
			time = dateFormat.parse(date);
		} catch (ParseException e) {
			throw new IOException("Invalid date");
		}
	}

	public static NewsMessage load(File newsContainer) throws IOException {
		NewsMessage cont = new NewsMessage();
		FileInputStream strm = new FileInputStream(newsContainer);
		PacketParser parser = new PacketParser();
		parser.importStream(strm);
		cont.author = parser.<String>nextEntry().get();
		cont.title = parser.<String>nextEntry().get();
		cont.authorImagePath = "news-cyan/imgs/author." + parser.<String>nextEntry().get() + ".jpg";
		cont.message = parser.<String>nextEntry().get();
		cont.setDate(parser.<String>nextEntry().get());
		strm.close();
		return cont;
	}
}
