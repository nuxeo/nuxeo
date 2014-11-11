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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.preview.adapter;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.preview.api.PreviewException;

/**
 * @author Alexandre Russel
 */
public class ImagePreviewer extends AbstractPreviewer implements
        MimeTypePreviewer {

    public List<Blob> getPreview(Blob blob, DocumentModel dm) throws PreviewException {
        List<Blob> blobResults = new ArrayList<Blob>();

        try {
            StringBuffer htmlPage = new StringBuffer();

            htmlPage.append("<html><head><title>");
            htmlPage.append(getPreviewTitle(dm));
            htmlPage.append("</title></head><body>");
            appendPreviewSettings(htmlPage);
            htmlPage.append("<img src=\"image\">");

            Blob mainBlob = new StringBlob(htmlPage.toString());
            mainBlob.setFilename("index.html");
            mainBlob.setMimeType("text/html");
            blob.setFilename("image");

            blobResults.add(mainBlob);
            blobResults.add(blob);
        } catch (ClientException e) {
            throw new PreviewException("Unable to get document property", e);
        }

        return blobResults;
    }

    private static void appendPreviewSettings(StringBuffer sb) {
        sb.append("<script type=\"text/javascript\">");
        sb.append("var previewSettings = { ");
        sb.append("imageOnly: true");
        sb.append("}");
        sb.append("</script>");
    }

}
