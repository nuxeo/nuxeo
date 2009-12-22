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

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.api.model.Property;

public class PictureBlobHolder extends DocumentBlobHolder {

    public PictureBlobHolder(DocumentModel doc, String path) {
        super(doc, path);
    }

    @Override
    public Blob getBlob() throws ClientException {
        return getBlob("Original");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setBlob(Blob blob) throws ClientException {
        xPathFilename = null;
        // check if there are templates
        ArrayList<Map<String, Object>> pictureTemplates = null;
        DocumentModel parent = doc.getCoreSession().getParentDocument(
                doc.getRef());
        if (parent.getType().equals("PictureBook")) {
            // use PictureBook Properties
            pictureTemplates = (ArrayList<Map<String, Object>>) parent.getProperty(
                    "picturebook", "picturetemplates");
            if (pictureTemplates.size() == 0) {
                pictureTemplates = null;
            }
        }
        // upload blob and create views
        PictureResourceAdapter picture = doc.getAdapter(PictureResourceAdapter.class);
        String filename = blob == null ? null : blob.getFilename();
        String title = (String) doc.getProperty("dublincore", "title"); // re-set
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
