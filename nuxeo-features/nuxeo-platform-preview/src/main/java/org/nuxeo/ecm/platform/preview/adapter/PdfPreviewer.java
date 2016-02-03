/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Gabriel Barata <gbarata@nuxeo.com>
 */

package org.nuxeo.ecm.platform.preview.adapter;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.preview.api.PreviewException;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 8.2
 */
public class PdfPreviewer extends AbstractPreviewer implements MimeTypePreviewer {

    public List<Blob> getPreview(Blob blob, DocumentModel dm) throws PreviewException {
        List<Blob> blobResults = new ArrayList<Blob>();
        StringBuffer htmlPage = new StringBuffer();
        String basePath = VirtualHostHelper.getContextPathProperty();

        htmlPage.append("<script src=\"" + basePath + "/bower_components/webcomponentsjs/webcomponents-lite.js\"></script>");
        htmlPage.append("<link rel=\"import\" href=\"" + basePath + "/viewers/nuxeo-pdf-viewer.vulcanized.html\">");

        htmlPage.append("<style>");
        htmlPage.append("nuxeo-pdf-viewer {");
        htmlPage.append("weight: 100%;");
        htmlPage.append("height: 100%;");
        htmlPage.append("--nuxeo-pdf-viewer-iframe: {");
        htmlPage.append("min-height: 500px; }}");
        htmlPage.append("</style>");
        htmlPage.append("</head><body>");

        htmlPage.append("<nuxeo-pdf-viewer src=\"pdf\"></nuxeo-pdf-viewer>");
        Blob mainBlob = Blobs.createBlob(htmlPage.toString(), "text/html", null, "index.html");
        blob.setFilename("pdf");
        blobResults.add(mainBlob);
        blobResults.add(blob);

        return blobResults;
    }
}
