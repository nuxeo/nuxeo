/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.http.entity.AbstractHttpEntity;
import org.nuxeo.ecm.automation.client.jaxrs.Constants;
import org.nuxeo.ecm.automation.client.jaxrs.model.Blob;
import org.nuxeo.ecm.automation.client.jaxrs.model.HasFile;
import org.nuxeo.ecm.automation.client.jaxrs.util.BlobDataSource;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class MultipartRequestEntity extends AbstractHttpEntity {

    protected MimeMultipart mp;

    public MultipartRequestEntity() {
        this(new MimeMultipart("related"));
    }

    public MultipartRequestEntity(String subType) {
        this(new MimeMultipart(subType));
    }

    public MultipartRequestEntity(MimeMultipart mp) {
        this.mp = mp;
        setContentType(mp.getContentType() + "; type=\""
                + Constants.CTYPE_REQUEST_NOCHARSET + "\"; start=\"request\"");
    }

    public void setRequest(String content) throws Exception {
        MimeBodyPart part = new MimeBodyPart();
        part.setText(content, "UTF-8");
        part.setContentID("request");
        part.setHeader("Content-Type", Constants.CTYPE_REQUEST);
        part.setHeader("Content-Transfer-Encoding", "8bit");
        part.setHeader("Content-Length", Integer.toString(content.length()));
        mp.addBodyPart(part);
    }

    public void setBlob(Blob blob) throws Exception {
        setBlob(blob, "input");
    }

    protected void setBlob(Blob blob, String id) throws Exception {
        MimeBodyPart part = new MimeBodyPart();
        if (blob instanceof HasFile) {
            part.attachFile(((HasFile) blob).getFile());
        } else {
            part.setDataHandler(new DataHandler(new BlobDataSource(blob)));
        }
        part.setHeader("Content-Type", blob.getMimeType());
        part.setHeader("Content-Transfer-Encoding", "binary");
        int length = blob.getLength();
        if (length > -1) {
            part.setHeader("Content-Length", Integer.toString(length));
        }
        part.setContentID(id);
        mp.addBodyPart(part);
    }

    public void setBlobs(List<Blob> blobs) throws Exception {
        for (int i = 0, size = blobs.size(); i < size; i++) {
            setBlob(blobs.get(i), "input#" + i);
        }
    }

    public boolean isRepeatable() {
        return false;
    }

    public boolean isStreaming() {
        return false;
    }

    public InputStream getContent() throws IOException, IllegalStateException {
        throw new UnsupportedOperationException("Not a streaming entity");
    }

    public void writeTo(OutputStream arg0) throws IOException {
        try {
            mp.writeTo(arg0);
        } catch (MessagingException e) {
            IOException ioe = new IOException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
    }

    public long getContentLength() {
        return -1;
    }

}
