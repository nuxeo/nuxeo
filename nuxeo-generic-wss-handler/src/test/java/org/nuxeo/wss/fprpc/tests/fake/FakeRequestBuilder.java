package org.nuxeo.wss.fprpc.tests.fake;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class FakeRequestBuilder {

    public static FakeRequest build(File file) throws Exception {
        InputStream in = new FileInputStream(file);
        return build(in);
    }

    public static FakeRequest buildFromResource(String resourceName) throws IOException {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
        return build(in);
    }

    public static FakeRequest build(InputStream in) throws IOException {
        BufferedReader reader = null;
        FakeRequest request = null;
        reader = new BufferedReader(new InputStreamReader(in));
        String line;
        boolean firstLine=true;
        boolean inBody=false;
        StringBuffer bodyBuffer = new StringBuffer();
        while ((line = reader.readLine()) != null) {
            if (firstLine) {
                request = initFakeRequest(line);
                firstLine=false;
            }
            else {
                if (inBody) {
                    bodyBuffer.append(line);
                    bodyBuffer.append("\n");
                }
                else {
                    if ("".equals(line.trim())) {
                        inBody=true;
                    } else {
                        String[] parts = line.split(":");
                        int idx = line.indexOf(":");
                        //request.addHeader(parts[0].trim(), parts[1].trim());
                        request.addHeader(line.substring(0,idx).trim(), line.substring(idx+1).trim());
                    }
                }
            }
        }

        if ("application/x-www-form-urlencoded".equals(request.getHeader("Content-Type"))) {
            request.setFormEncodedBody(bodyBuffer.toString());
        } else {
            request.setStream(new FakeServletInputStream(bodyBuffer.toString()));
        }

        return request;
    }

    protected static FakeRequest initFakeRequest(String firstLine) {

        String[] parts = firstLine.split(" ");
        String method = parts[0].trim();
        String url = parts[1].trim();
        FakeRequest request = new FakeRequest(method, url);
        return request;
    }

}
