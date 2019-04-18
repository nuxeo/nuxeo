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
package org.nuxeo.ecm.automation.jaxrs.io.operations;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.jaxrs.io.InputStreamDataSource;
import org.nuxeo.ecm.automation.jaxrs.io.SharedFileInputStream;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.web.common.RequestContext;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Provider
@Consumes({ "multipart/form-data", "multipart/related" })
public class MultiPartFormRequestReader implements MessageBodyReader<ExecutionRequest> {

    private static final Log log = LogFactory.getLog(MultiPartFormRequestReader.class);

    @Context
    protected HttpServletRequest request;

    @Context
    JsonFactory factory;

    public CoreSession getCoreSession() {
        return SessionFactory.getSession(request);
    }

    @Override
    public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
        return ExecutionRequest.class.isAssignableFrom(arg0); // TODO check media type too
    }

    @Override
    public ExecutionRequest readFrom(Class<ExecutionRequest> arg0, Type arg1, Annotation[] arg2, MediaType arg3,
            MultivaluedMap<String, String> headers, InputStream in) throws IOException, WebApplicationException {
        ExecutionRequest req = null;
        try {
            List<String> ctypes = headers.get("Content-Type");
            String ctype = ctypes.get(0);
            // we need to copy first the stream into a file otherwise it may
            // happen that
            // javax.mail fail to receive some parts - I am not sure why -
            // perhaps the stream is no more available when javax.mail need it?
            File tmp = Framework.createTempFile("nx-automation-mp-upload-", ".tmp");
            FileUtils.copyInputStreamToFile(in, tmp);
            // get the input from the saved file
            try (InputStream sfin = new SharedFileInputStream(tmp)) {
                MimeMultipart mp = new MimeMultipart(new InputStreamDataSource(sfin, ctype));
                BodyPart part = mp.getBodyPart(0); // use content ids
                try (InputStream pin = part.getInputStream(); //
                        JsonParser jp = factory.createParser(pin)) {
                    req = JsonRequestReader.readRequest(jp, headers, getCoreSession());
                }
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
                        log.error("Received parts: " + mp.getBodyPart(i).getHeader("Content-ID")[0] + " -> "
                                + mp.getBodyPart(i).getContentType());
                    }
                    throw new NuxeoException(
                            new IllegalStateException("Received only " + cnt + " part in a multipart request"));
                }
            } finally {
                tmp.delete();
            }
        } catch (MessagingException | IOException e) {
            throw new NuxeoException("Failed to parse multipart request", e);
        }
        return req;
    }

    public static Blob readBlob(HttpServletRequest request, BodyPart part) throws MessagingException, IOException {
        String ctype = part.getContentType();
        String fname = part.getFileName();
        try {
            // get back the original filename header bytes and try to decode them using UTF-8
            // if decoding succeeds, use it as the new filename, otherwise keep the original one
            byte[] bytes = fname.getBytes("ISO-8859-1");
            CharsetDecoder dec = Charset.forName("UTF-8").newDecoder();
            CharBuffer buffer = dec.onUnmappableCharacter(CodingErrorAction.REPORT)
                                   .onMalformedInput(CodingErrorAction.REPORT)
                                   .decode(ByteBuffer.wrap(bytes));
            fname = buffer.toString();
        } catch (CharacterCodingException e) {
            // do nothing, keep the original filename
        }

        final File tmp = Framework.createTempFile("nx-automation-upload-", ".tmp");
        try (InputStream pin = part.getInputStream()) {
            FileUtils.copyInputStreamToFile(pin, tmp);
        }
        Blob blob = Blobs.createBlob(tmp, ctype, null, fname);
        RequestContext.getActiveContext(request).addRequestCleanupHandler(req -> tmp.delete());
        return blob;
    }

}
