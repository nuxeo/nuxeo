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
package org.nuxeo.ecm.automation.jaxrs.io.operations;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.jaxrs.io.InputStreamDataSource;
import org.nuxeo.ecm.automation.jaxrs.io.SharedFileInputStream;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestCleanupHandler;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Provider
@Consumes("multipart/related")
public class MultiPartRequestReader implements
        MessageBodyReader<ExecutionRequest> {

    private static final Log log = LogFactory.getLog(MultiPartRequestReader.class);

    @Context
    protected HttpServletRequest request;

    @Context
    JsonFactory factory;

    public CoreSession getCoreSession() {
        return SessionFactory.getSession(request);
    }

    @Override
    public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2,
            MediaType arg3) {
        return ExecutionRequest.class.isAssignableFrom(arg0); // TODO check
        // media type too
    }

    @Override
    public ExecutionRequest readFrom(Class<ExecutionRequest> arg0, Type arg1,
            Annotation[] arg2, MediaType arg3,
            MultivaluedMap<String, String> headers, InputStream in)
            throws IOException, WebApplicationException {
        ExecutionRequest req = null;
        try {
            List<String> ctypes = headers.get("Content-Type");
            String ctype = ctypes.get(0);
            // we need to copy first the stream into a file otherwise it may
            // happen that
            // javax.mail fail to receive some parts - I am not sure why -
            // perhaps the stream is no more available when javax.mail need it?
            File tmp = File.createTempFile("nx-automation-mp-upload-", ".tmp");
            FileUtils.copyToFile(in, tmp);
            in = new SharedFileInputStream(tmp); // get the input from the saved
                                            // file
            try {
                MimeMultipart mp = new MimeMultipart(new InputStreamDataSource(
                        in, ctype));
                BodyPart part = mp.getBodyPart(0); // use content ids
                InputStream pin = part.getInputStream();
                JsonParser jp = factory.createJsonParser(pin);
                req = JsonRequestReader.readRequest(jp, headers, getCoreSession());
                int cnt = mp.getCount();
                if (cnt == 2) { // a blob
                    req.setInput(readBlob(request, mp.getBodyPart(1)));
                } else if (cnt > 2) { // a blob list
                    BlobList blobs = new BlobList();
                    for (int i = 1; i < cnt; i++) {
                        blobs.add(readBlob(request, mp.getBodyPart(i)));
                    }
                    req.setInput(blobs);
                } else {
                    log.error("Not all parts received.");
                    for (int i = 0; i < cnt; i++) {
                        log.error("Received parts: "
                                + mp.getBodyPart(i).getHeader("Content-ID")[0]
                                + " -> " + mp.getBodyPart(i).getContentType());
                    }
                    throw WebException.newException(new IllegalStateException(
                            "Received only " + cnt
                                    + " part in a multipart request"));
                }
            } finally {
                tmp.delete();
            }
        } catch (Throwable e) {
            throw WebException.newException(
                    "Failed to parse multipart request", e);
        }
        return req;
    }

    public static Blob readBlob(HttpServletRequest request, BodyPart part)
            throws Exception {
        String ctype = part.getContentType();
        String fname = part.getFileName();
        InputStream pin = part.getInputStream();
        final File tmp = File.createTempFile("nx-automation-upload-", ".tmp");
        FileUtils.copyToFile(pin, tmp);
        FileBlob blob = new FileBlob(tmp, ctype, null, fname, null);
        RequestContext.getActiveContext(request).addRequestCleanupHandler(
                new RequestCleanupHandler() {
                    @Override
                    public void cleanup(HttpServletRequest req) {
                        tmp.delete();
                    }
                });
        return blob;
    }

}
