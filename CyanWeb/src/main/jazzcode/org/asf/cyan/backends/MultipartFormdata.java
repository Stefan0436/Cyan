package org.asf.cyan.backends;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.asf.rats.HttpRequest;

public class MultipartFormdata {
	public HashMap<String, String> headers = new HashMap<String, String>();

	public String getProp(String name) {
		String contentDisposition = headers.get("Content-Disposition");
		if (contentDisposition == null)
			return null;
		String data = contentDisposition.substring(contentDisposition.indexOf("; ") + 2);
		if (!data.contains(name + "=\""))
			return null;
		while (!data.startsWith(name + "=\""))
			data = data.substring(data.indexOf("; ") + 2);
		data = data.substring((name + "=\"").length());
		data = data.substring(0, data.indexOf("\""));
		return data.replace("\\\"", "\"");
	}

	private HttpRequest request;
	private String boundary;
	private boolean transferred = false;

	public static String getBoundary(HttpRequest request) {
		if (!request.headers.containsKey("Content-Type")
				|| !request.headers.get("Content-Type").toLowerCase().startsWith("multipart/form-data; boundary=")) {
			return null;
		}

		return request.headers.get("Content-Type").substring("multipart/form-data; boundary=".length());
	}

	public static MultipartFormdata getFirst(HttpRequest request) throws IOException {
		String boundary = getBoundary(request);
		if (boundary == null)
			return null;
		InputStream strm = request.getRequestBodyStream();

		MultipartFormdata data = new MultipartFormdata();
		data.request = request;
		data.boundary = boundary;

		if (!readTillBoundary(strm, boundary))
			return null;

		String buffer = "";
		while (true) {
			int ch = strm.read();
			if (ch == '\r')
				continue;
			if (ch == '\n') {
				if (buffer.isEmpty())
					break;

				if (buffer.contains(": ")) {
					data.headers.put(buffer.substring(0, buffer.indexOf(": ")),
							buffer.substring(buffer.indexOf(": ") + 2));
				} else {
					return null;
				}
				buffer = "";
			} else {
				buffer += (char) ch;
			}
		}

		return data;
	}

	private static boolean readTillBoundary(InputStream strm, String boundary) throws IOException {
		String buffer = "";
		int i = 0;
		while (true) {
			int c = strm.read();
			if (c == '-' && (i == 0 || i == 1)) {
				i++;
			} else {
				if (i < 2 && c != '-')
					return false;
				else if (i == 2)
					i = 3;
				else if (i != 3)
					i = 0;
				buffer += (char) c;
			}
			if (!boundary.equals(buffer) && !boundary.startsWith(buffer)) {
				return false;
			} else if (boundary.equals(buffer)) {
				strm.read();
				strm.read();
				return true;
			}
		}
	}

	public MultipartFormdata getNext() throws IOException {
		if (!transferred)
			transfer(null);

		MultipartFormdata data = new MultipartFormdata();
		data.request = request;
		data.boundary = boundary;

		String buffer = "";
		while (true) {
			int ch = request.getRequestBodyStream().read();
			if (ch == -1)
				return null;
			if (ch == '\r')
				continue;
			if (ch == '\n') {
				if (buffer.isEmpty())
					break;

				if (buffer.contains(": ")) {
					data.headers.put(buffer.substring(0, buffer.indexOf(": ")),
							buffer.substring(buffer.indexOf(": ") + 2));
				} else {
					return null;
				}
				buffer = "";
			} else {
				buffer += (char) ch;
			}
		}

		if (data.headers.size() == 0)
			return null;

		return data;
	}

	public void transfer(OutputStream output) throws IOException {
		if (transferred)
			throw new IOException("No content to transfer as the transfer was already run before.");
		transferred = true;
		ArrayList<Integer> buff = new ArrayList<Integer>();
		int i = 0;
		String buffer = "";
		while (true) {
			int c = request.getRequestBodyStream().read();
			if (c == '\r' && i == 0) {
				buff.add(c);
				i++;
			} else if (c == '\n' && i == 1) {
				buff.add(c);
				i++;
			} else if (c == '-' && (i == 2 || i == 3)) {
				buff.add(c);
				i++;
			} else if (i == 4) {
				if (buffer.isEmpty()) {
					buffer += (char) c;
					buff.add(c);
				} else {
					if (!boundary.equals(buffer) && boundary.startsWith(buffer)) {
						buffer += (char) c;
						buff.add(c);
					} else if (!boundary.equals(buffer)) {
						i = 0;
						if (output != null)
							for (int b : buff)
								output.write(b);
						buff.clear();
						buffer = "";
						if (output != null)
							output.write(c);
					} else {
						request.getRequestBodyStream().read();
						return;
					}
				}
			} else {
				i = 0;
				if (buff.size() != 0) {
					if (output != null)
						for (int b : buff)
							output.write(b);
					buff.clear();
					buffer = "";
				}
				if (output != null)
					output.write(c);
			}
		}
	}

	public String getContentString() throws IOException {
		ByteArrayOutputStream strm = new ByteArrayOutputStream();
		transfer(strm);
		return new String(strm.toByteArray());
	}
}
