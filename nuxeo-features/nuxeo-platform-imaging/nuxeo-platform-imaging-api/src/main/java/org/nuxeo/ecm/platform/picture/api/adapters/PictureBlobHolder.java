/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.picture.api.adapters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants;

public class PictureBlobHolder extends DocumentBlobHolder {

    public PictureBlobHolder(DocumentModel doc, String path) {
        super(doc, path);
    }

    @Override
    public Blob getBlob() throws ClientException {
        Blob blob = super.getBlob();
        return blob != null ? blob : getBlob("Original");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setBlob(Blob blob) throws ClientException {
        xPathFilename = null;
        super.setBlob(blob);
        // check if there are templates
        ArrayList<Map<String, Object>> pictureTemplates = null;

        CoreSession session = doc.getCoreSession();
        DocumentModel parent;
        if (session.exists(doc.getRef())) {
            parent = session.getParentDocument(
                    doc.getRef());
        } else {
            Path parentPath = doc.getPath().removeLastSegments(1);
            parent = session.getDocument(new PathRef(parentPath.toString()));
        }

        if (parent != null && ImagingDocumentConstants.PICTUREBOOK_TYPE_NAME.equals(parent.getType())) {
            // use PictureBook Properties
            pictureTemplates = (ArrayList<Map<String, Object>>) parent.getPropertyValue(
                    "picturebook:picturetemplates");
            if (pictureTemplates.isEmpty()) {
                pictureTemplates = null;
            }
        }

        // upload blob and create views
        PictureResourceAdapter picture = doc.getAdapter(PictureResourceAdapter.class);
        String filename = blob == null ? null : blob.getFilename();
        String title = (String) doc.getPropertyValue("dc:title"); // re-set
        try {
            picture.createPicture(blob, filename, title, pictureTemplates);
        } catch (IOException e) {
            throw new ClientException(e.toString(), e);
        }
    }

    @Override
    public List<Blob> getBlobs() throws ClientException {
        List<Blob> blobList = new ArrayList<Blob>();
        Collection<Property> views = doc.getProperty("picture:views").getChildren();
        for (Property property : views) {
            blobList.add((Blob) property.getValue("content"));
        }
        return blobList;

    }

    public List<Blob> getBlobs(String... viewNames) throws ClientException {
        List<Blob> blobList = new ArrayList<Blob>();
        for (int i = 0; i < viewNames.length; i++) {
            blobList.add(getBlob(viewNames[i]));
        }
        return blobList;
    }

    public Blob getBlob(String title) throws ClientException {
        PictureResourceAdapter picture = doc.getAdapter(PictureResourceAdapter.class);
        return picture.getPictureFromTitle(title);
    }

    @Override
    public String getHash() throws ClientException {

        Blob blob = getBlob();
        if (blob != null) {
            String h = blob.getDigest();
            if (h != null) {
                return h;
            }
        }
        return doc.getId() + xPath + getModificationDate().toString();
    }

}
