/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.opensocial.mydocs.rest;

import javax.ws.rs.GET;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;

@WebAdapter(name = "picture", type = "pictureAdapter", targetType = "Document")
public class PictureAdapter extends DefaultAdapter {

    @GET
    public Object doGet() throws ClientException {

        DocumentModel doc = ((DocumentObject) getTarget()).getDocument();

        BlobHolder blobHolder = new SimpleBlobHolder(doc.getProperty(
                "picture:views/view[0]/content")
                .getValue(Blob.class));

        Blob blob = blobHolder.getBlob();

        return Response.ok(blob)
                .header("Content-Disposition",
                        "inline;filename=" + blob.getFilename())
                .type(blob.getMimeType())
                .build();
    }
}
