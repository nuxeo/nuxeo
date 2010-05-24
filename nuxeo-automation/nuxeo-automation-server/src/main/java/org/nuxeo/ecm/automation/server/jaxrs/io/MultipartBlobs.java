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
package org.nuxeo.ecm.automation.server.jaxrs.io;

import java.util.List;

import javax.activation.DataHandler;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.automation.client.jaxrs.impl.blob.InputStreamDataSource;
import org.nuxeo.ecm.core.api.Blob;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MultipartBlobs extends MimeMultipart {

    public MultipartBlobs() {
        super ("mixed");
    }

    public MultipartBlobs(List<Blob> blobs) throws Exception {
        super ("mixed");
        addBlobs(blobs);
    }

    public void addBlobs(List<Blob> blobs) throws Exception {
        for (Blob blob : blobs) {
            addBlob(blob);
        }
    }

    public void addBlob(Blob blob) throws Exception {
        MimeBodyPart part = new MimeBodyPart();
        part.setDataHandler(new DataHandler(new InputStreamDataSource(blob.getStream(), blob.getMimeType(), blob.getFilename())));
        String filename = blob.getFilename();
        if (filename != null) {
            part.setDisposition("attachment; filename="+blob.getFilename());
        } else {
            part.setDisposition("attachment");
        }
        addBodyPart(part);
    }

    public Response getResponse() {
        return Response.ok(this).type(getContentType()).build();
    }

}
