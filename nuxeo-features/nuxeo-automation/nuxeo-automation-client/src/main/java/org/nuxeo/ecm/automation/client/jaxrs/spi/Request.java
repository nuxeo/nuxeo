/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi;

import static org.nuxeo.ecm.automation.client.Constants.CTYPE_AUTOMATION;
import static org.nuxeo.ecm.automation.client.Constants.CTYPE_ENTITY;
import static org.nuxeo.ecm.automation.client.Constants.CTYPE_MULTIPART_EMPTY;
import static org.nuxeo.ecm.automation.client.Constants.CTYPE_MULTIPART_MIXED;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonParser;
import org.nuxeo.ecm.automation.client.RemoteException;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.ExceptionMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.util.IOUtils;
import org.nuxeo.ecm.automation.client.jaxrs.util.InputStreamDataSource;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.client.model.Blobs;
import org.nuxeo.ecm.automation.client.model.FileBlob;
import org.nuxeo.ecm.automation.client.model.StringBlob;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Request extends HashMap<String, String> {

    public static final int GET = 0;

    public static final int POST = 1;

    private static final long serialVersionUID = 1L;

    protected static Pattern RFC2231_ATTR_PATTERN = Pattern.compile(
            ";?\\s*filename\\s*\\\\*.*\\*=([^']*)'([^']*)'\\s*([^;]+)\\s*", Pattern.CASE_INSENSITIVE);

    protected static Pattern ATTR_PATTERN = Pattern.compile(";?\\s*filename\\s*=\\s*([^;]+)\\s*",
            Pattern.CASE_INSENSITIVE);

    protected final int method;

    protected final String url;

    protected final boolean isMultiPart;

    protected Object entity;

    public Request(int method, String url) {
        this.method = method;
        this.url = url;
        isMultiPart = false;
    }

    public Request(int method, String url, MimeMultipart entity) {
        this.method = method;
        this.url = url;
        this.entity = entity;
        isMultiPart = true;
    }

    public Request(int method, String url, String entity) {
        this.method = method;
        this.url = url;
        this.entity = entity;
        isMultiPart = false;
    }

    public int getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public Object getEntity() {
        return entity;
    }

    public final boolean isMultiPart() {
        return isMultiPart;
    }

    public MimeMultipart asMultiPartEntity() {
        return isMultiPart ? (MimeMultipart) entity : null;
    }

    public String asStringEntity() {
        return isMultiPart ? null : (String) entity;
    }

    /**
     * Must read the object from the server response and return it or throw a {@link RemoteException} if server sent an
     * error.
     */
    public Object handleResult(int status, String ctype, String disp, InputStream stream)
            throws RemoteException, IOException {
        // Specific http status handling
        if (status >= Response.Status.BAD_REQUEST.getStatusCode()) {
            handleException(status, ctype, stream);
        } else if (status == Response.Status.NO_CONTENT.getStatusCode() || stream == null) {
            if (ctype != null && ctype.toLowerCase().startsWith(CTYPE_MULTIPART_EMPTY)) {
                // empty entity and content type of nuxeo empty list
                return new Blobs();
            }
            // no content
            return null;
        }
        // Check content type
        if (ctype == null) {
            if (status != Response.Status.OK.getStatusCode()) {
                // this may happen when login failed
                throw new RemoteException(status, "ServerError", "Server Error", "");
            }
            // cannot handle responses with no content type
            return null;
        }
        // Handle result
        String lctype = ctype.toLowerCase();
        if (lctype.startsWith(CTYPE_AUTOMATION)) {
            return JsonMarshalling.readRegistry(IOUtils.read(stream));
        } else if (lctype.startsWith(CTYPE_ENTITY)) {
            String body = IOUtils.read(stream);
            try {
                return JsonMarshalling.readEntity(body);
            } catch (IOException | RuntimeException e) {
                return readStringBlob(ctype, getFileName(disp), body);
            }
        } else if (lctype.startsWith(CTYPE_MULTIPART_MIXED)) { // list of blobs
            return readBlobs(ctype, stream);
        } else { // a blob?
            return readBlob(ctype, getFileName(disp), stream);
        }
    }

    protected static Blobs readBlobs(String ctype, InputStream in) throws IOException {
        Blobs files = new Blobs();
        // save the stream to a temporary file
        File file = IOUtils.copyToTempFile(in);
        try (FileInputStream fin = new FileInputStream(file)) {
            MimeMultipart mp = new MimeMultipart(new InputStreamDataSource(fin, ctype));
            int size = mp.getCount();
            for (int i = 0; i < size; i++) {
                BodyPart part = mp.getBodyPart(i);
                String fname = part.getFileName();
                files.add(readBlob(part.getContentType(), fname, part.getInputStream()));
            }
        } catch (MessagingException e) {
            throw new IOException(e);
        } finally {
            file.delete();
        }
        return files;
    }

    protected static Blob readBlob(String ctype, String fileName, InputStream in) throws IOException {
        File file = IOUtils.copyToTempFile(in);
        FileBlob blob = new FileBlob(file);
        blob.setMimeType(ctype);
        if (fileName != null) {
            blob.setFileName(fileName);
        }
        return blob;
    }

    protected static Blob readStringBlob(String ctype, String fileName, String content) {
        return new StringBlob(fileName, content, ctype);
    }

    protected static String getFileName(String ctype) {
        if (ctype == null) {
            return null;
        }

        Matcher m = RFC2231_ATTR_PATTERN.matcher(ctype);
        if (m.find()) {
            try {
                return URLDecoder.decode(m.group(3), m.group(1));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        m = ATTR_PATTERN.matcher(ctype);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    protected void handleException(int status, String ctype, InputStream stream) throws RemoteException {
        if (stream == null) {
            throw new RemoteException(status, "ServerError", "Server Error", "");
        }
        String content;
        try {
            content = IOUtils.read(stream);
        } catch (IOException e) {
            // typically: org.apache.http.ConnectionClosedException: Premature end of chunk coded message body:
            // closing chunk expected
            throw new RemoteException(status, "ServerError", "Server Error", "");
        }
        if (CTYPE_ENTITY.equalsIgnoreCase(ctype)) {
            try {
                throw ExceptionMarshaller.readException(content);
            } catch (IOException t) {
                // JSON decoding error in the payload
                throw new RemoteException(status, "ServerError", "Server Error", content);
            }
        } else {
            // no JSON payload
            throw new RemoteException(status, "ServerError", "Server Error", content);
        }
    }

}
