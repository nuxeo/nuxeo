/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 *
 */

package org.nuxeo.ecm.platform.preview.adapter;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.preview.api.PreviewException;

/**
 * @author Alexandre Russel
 * @since 10.3
 * @deprecated since 10.3
 */
public class PlainImagePreviewer extends AbstractPreviewer implements MimeTypePreviewer {

    public List<Blob> getPreview(Blob blob, DocumentModel dm) throws PreviewException {
        List<Blob> blobResults = new ArrayList<Blob>();
        StringBuffer htmlPage = new StringBuffer();
        htmlPage.append("<html><head><title>");
        htmlPage.append(getPreviewTitle(dm));
        htmlPage.append("</title></head><body>");
        appendPreviewSettings(htmlPage);
        htmlPage.append("<img src=\"image\">");
        Blob mainBlob = Blobs.createBlob(htmlPage.toString(), "text/html", null, "index.html");
        blob.setFilename("image");
        blobResults.add(mainBlob);
        blobResults.add(blob);
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
