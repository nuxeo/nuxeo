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
package org.nuxeo.ecm.automation.client.jaxrs.util;

import java.util.List;

import javax.activation.DataHandler;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.nuxeo.ecm.automation.client.jaxrs.Constants;
import org.nuxeo.ecm.automation.client.jaxrs.model.Blob;
import org.nuxeo.ecm.automation.client.jaxrs.model.HasFile;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class MultipartInput extends MimeMultipart {

    public MultipartInput() {
        super("related");
    }

    public void setRequest(String content) throws Exception {
        MimeBodyPart part = new MimeBodyPart();
        part.setText(content, "UTF-8");
        part.setContentID("request");
        part.setHeader("Content-Type", Constants.CTYPE_REQUEST);
        part.setHeader("Content-Transfer-Encoding", "8bit");
        part.setHeader("Content-Length", Integer.toString(content.length()));
        addBodyPart(part);
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
        addBodyPart(part);
    }

    public void setBlobs(List<Blob> blobs) throws Exception {
        for (int i = 0, size = blobs.size(); i < size; i++) {
            setBlob(blobs.get(i), "input#" + i);
        }
    }

}
