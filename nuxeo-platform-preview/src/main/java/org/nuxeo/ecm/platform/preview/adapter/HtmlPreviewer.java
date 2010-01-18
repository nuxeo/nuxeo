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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.preview.api.PreviewException;

/**
 * @author Alexandre Russel
 */
public class HtmlPreviewer extends AbstractPreviewer implements
        MimeTypePreviewer {

    public List<Blob> getPreview(Blob blob, DocumentModel dm) throws PreviewException {
        List<Blob> blobResults=new ArrayList<Blob>();

        StringBuffer htmlPage = new StringBuffer();

        htmlPage.append("<html>");
        try {
            htmlPage.append("<pre>").append(blob.getString()).append("</pre>");
        } catch (IOException e) {
            throw new PreviewException(e);
        }
        htmlPage.append("</html>");

        Blob mainBlob = new StringBlob(htmlPage.toString());
        mainBlob.setFilename("index.html");
        mainBlob.setMimeType("text/html");

        blobResults.add(mainBlob);
        return blobResults;
    }

}
