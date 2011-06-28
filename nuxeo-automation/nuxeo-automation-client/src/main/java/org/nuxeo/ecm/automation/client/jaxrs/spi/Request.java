/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi;

import static org.nuxeo.ecm.automation.client.Constants.CTYPE_AUTOMATION;
import static org.nuxeo.ecm.automation.client.Constants.CTYPE_ENTITY;
import static org.nuxeo.ecm.automation.client.Constants.CTYPE_MULTIPART_MIXED;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.BodyPart;
import javax.mail.internet.MimeMultipart;

import org.nuxeo.ecm.automation.client.RemoteException;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.ExceptionMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.util.IOUtils;
import org.nuxeo.ecm.automation.client.jaxrs.util.InputStreamDataSource;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.client.model.Blobs;
import org.nuxeo.ecm.automation.client.model.FileBlob;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Request extends HashMap<String, String> {

    public static final int GET = 0;

    public static final int POST = 1;

    private static final long serialVersionUID = 1L;

    protected static Pattern ATTR_PATTERN = Pattern.compile(
            ";?\\s*filename\\s*=\\s*([^;]+)\\s*", Pattern.CASE_INSENSITIVE);

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
     * Must read the object from the server response and return it or throw a
     * {@link RemoteException} if server sent an error.
     */
    public Object handleResult(int status, String ctype, String disp,
            InputStream stream) throws Exception {
        if (status == 204) { // no content
            return null;
        } else if (status >= 400) {
            handleException(status, ctype, stream);
        }
        if (ctype.startsWith(CTYPE_ENTITY)) {
            return JsonMarshalling.readEntity(IOUtils.read(stream));
        } else if (ctype.startsWith(CTYPE_AUTOMATION)) {
            return JsonMarshalling.readRegistry(IOUtils.read(stream));
        } else if (ctype.startsWith(CTYPE_MULTIPART_MIXED)) { // list of
                                                                // blobs
            return readBlobs(ctype, stream);
        } else { // a blob?
            String fname = null;
            if (disp != null) {
                fname = getFileName(disp);
            }
            return readBlob(ctype, fname, stream);
        }
    }

    protected static Blobs readBlobs(String ctype, InputStream in)
            throws Exception {
        Blobs files = new Blobs();
        // save the stream to a temporary file
        File file = IOUtils.copyToTempFile(in);
        FileInputStream fin = new FileInputStream(file);
        try {
            MimeMultipart mp = new MimeMultipart(new InputStreamDataSource(fin,
                    ctype));
            int size = mp.getCount();
            for (int i = 0; i < size; i++) {
                BodyPart part = mp.getBodyPart(i);
                String fname = part.getFileName();
                files.add(readBlob(part.getContentType(), fname,
                        part.getInputStream()));
            }
        } finally {
            try {
                fin.close();
            } catch (Exception e) {
            }
            file.delete();
        }
        return files;
    }

    protected static Blob readBlob(String ctype, String fileName, InputStream in)
            throws Exception {
        File file = IOUtils.copyToTempFile(in);
        file.deleteOnExit();
        FileBlob blob = new FileBlob(file);
        blob.setMimeType(ctype);
        if (fileName != null) {
            blob.setFileName(fileName);
        }
        return blob;
    }

    protected static String getFileName(String ctype) {
        Matcher m = ATTR_PATTERN.matcher(ctype);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    protected void handleException(int status, String ctype, InputStream stream)
            throws Exception {
        if (CTYPE_ENTITY.equals(ctype)) {
            String content = IOUtils.read(stream);
            RemoteException e = null;
            try {
                e = ExceptionMarshaller.readException(content);
            } catch (Throwable t) {
                throw new RemoteException(status, "ServerError",
                        "Server Error", content);
            }
            throw e;
        } else {
            throw new RemoteException(status, "ServerError", "Server Error",
                    IOUtils.read(stream));
        }
    }

}
