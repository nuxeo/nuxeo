/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.jaxrs.io.documents;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.automation.jaxrs.io.InputStreamDataSource;
import org.nuxeo.ecm.core.api.Blob;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class MultipartBlobs extends MimeMultipart {

    private static final Pattern BOUNDARY = Pattern.compile(";\\s*boundary\\s*=\"([^\"]+)\"");

    public MultipartBlobs() {
        super("mixed");
    }

    public MultipartBlobs(List<Blob> blobs) throws MessagingException, IOException {
        super("mixed");
        addBlobs(blobs);
    }

    public void addBlobs(List<Blob> blobs) throws MessagingException, IOException {
        for (Blob blob : blobs) {
            addBlob(blob);
        }
    }

    public void addBlob(Blob blob) throws MessagingException, IOException {
        MimeBodyPart part = new MimeBodyPart();
        part.setDataHandler(new DataHandler(new InputStreamDataSource(blob.getStream(), blob.getMimeType(),
                blob.getFilename())));
        part.setDisposition("attachment");
        String filename = blob.getFilename();
        if (filename != null) {
            part.setFileName(filename);
        }
        // must set the mime type at end because setFileName is also setting a
        // wrong content type.
        String mimeType = blob.getMimeType();
        if (mimeType != null) {
            part.setHeader("Content-type", mimeType);
        }

        addBodyPart(part);
    }

    public String getBoundary() {
        Matcher m = BOUNDARY.matcher(getContentType());
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public Response getResponse() {
        // jersey is not correctly reading ctype string -> it is removing quote
        // from values..
        // we need to rebuild ourself the correct header to preserve quotes on
        // boundary value (otherwise javax.mail will not work on client side)
        // for this we use our own MediaType class
        return Response.ok(this).type(new BoundaryMediaType(getContentType())).build();
    }

    /**
     * Workaround to be able to output boundary with quotes if needed.
     *
     * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
     */
    static class BoundaryMediaType extends MediaType {
        private final String ctype;

        BoundaryMediaType(String ctype) {
            super("multipart", "mixed");
            this.ctype = ctype;
        }

        @Override
        public String toString() {
            return ctype;
        }
    }

}
