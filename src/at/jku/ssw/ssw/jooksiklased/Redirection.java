package at.jku.ssw.ssw.jooksiklased;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

public class Redirection extends Thread {
	private Reader in;
	private Writer out;

	Redirection(InputStream is, OutputStream os) {
		in = new InputStreamReader(is);
		out = new OutputStreamWriter(os);
	}

	public void run() {
		try {
			char[] buf = new char[1024];
			int n;
			while ((n = in.read(buf, 0, 1024)) >= 0)
				out.write(buf, 0, n);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
