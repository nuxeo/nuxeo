/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *     Andre Justo
 *     Miguel Nixo
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.preview.adapter;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.preview.api.PreviewException;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

/**
 * @author Alexandre Russel
 */
public class ImagePreviewer extends AbstractPreviewer implements MimeTypePreviewer {

    protected Blob getContentBlob(Blob original, DocumentModel doc) {
        return original;
    }

    public List<Blob> getPreview(Blob blob, DocumentModel dm) throws PreviewException {
        List<Blob> blobResults = new ArrayList<>();
        String basePath = VirtualHostHelper.getContextPathProperty();
        StringBuffer html = new StringBuffer();
        html.append("<html><head>");
        html.append("<title>" + getPreviewTitle(dm) + "</title>");
        html.append(String.format("<script src=\"%s/bower_components/webcomponentsjs/webcomponents-lite.js\"></script>", basePath));
        html.append(String.format("<link rel=\"import\" href=\"%s/viewers/nuxeo-image-viewer.vulcanized.html\">", basePath));
        html.append("<style>");
        html.append("nuxeo-image-viewer {");
        html.append("height: 100%; }");
        html.append("</style>");
        html.append("</head><body>");
        html.append("<nuxeo-image-viewer src=\"image\" controls responsive></nuxeo-image-viewer>");
        html.append("</body>");
        Blob mainBlob = Blobs.createBlob(html.toString(), "text/html", null, "index.html");
        blobResults.add(mainBlob);
        Blob content = getContentBlob(blob, dm);
        content.setFilename("image");
        blobResults.add(content);
        return blobResults;
    }
}
