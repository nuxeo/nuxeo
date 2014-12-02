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
package org.nuxeo.ecm.automation.client.jaxrs.util;

import java.io.IOException;
import java.util.List;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.nuxeo.ecm.automation.client.Constants;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.client.model.HasFile;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class MultipartInput extends MimeMultipart {

    public MultipartInput() {
        super("related");
    }

    public void setRequest(String content) throws IOException {
        MimeBodyPart part = new MimeBodyPart();
        try {
            part.setText(content, "UTF-8");
            part.setContentID("request");
            part.setHeader("Content-Type", Constants.CTYPE_REQUEST);
            part.setHeader("Content-Transfer-Encoding", "8bit");
            part.setHeader("Content-Length", Integer.toString(content.length()));
            addBodyPart(part);
        } catch (MessagingException e) {
            throw new IOException(e);
        }
    }

    public void setBlob(Blob blob) throws IOException {
        setBlob(blob, "input");
    }

    protected void setBlob(Blob blob, String id) throws IOException {
        try {
            MimeBodyPart part = new MimeBodyPart();
            if (blob instanceof HasFile) {
                part.attachFile(((HasFile) blob).getFile());
            } else {
                part.setDataHandler(new DataHandler(new BlobDataSource(blob)));
                if (blob.getFileName() != null) {
                    part.setFileName(blob.getFileName());
                }
            }
            part.setHeader("Content-Type", blob.getMimeType());
            part.setHeader("Content-Transfer-Encoding", "binary");
            int length = blob.getLength();
            if (length > -1) {
                part.setHeader("Content-Length", Integer.toString(length));
            }
            part.setContentID(id);
            addBodyPart(part);
        } catch (MessagingException e) {
            throw new IOException(e);
        }
    }

    public void setBlobs(List<Blob> blobs) throws IOException {
        for (int i = 0, size = blobs.size(); i < size; i++) {
            setBlob(blobs.get(i), "input#" + i);
        }
    }

}
