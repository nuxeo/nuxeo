package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class URLChecker {

	private int status;

	public boolean checkUrlContentAndStatusOK(URL url) throws IOException {
		String contentOf = getContentOf(url);
		return contentOf.length() > 0 && isStatus(HttpURLConnection.HTTP_OK);
	}

	public String getContentOf(URL url) throws IOException {
		URLConnection connection = url.openConnection();
		this.status = ((HttpURLConnection) connection).getResponseCode();
		InputStream inputStream = connection.getInputStream();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int ln = 0;
		while ((ln = inputStream.read(buf)) > -1) {
			bos.write(buf, 0, ln);
		}
		bos.flush();
		bos.close();
		inputStream.close();
		String res = bos.toString();
		return res;
	}

	public boolean isStatus(int status) {
		return this.status == status;
	}

}
