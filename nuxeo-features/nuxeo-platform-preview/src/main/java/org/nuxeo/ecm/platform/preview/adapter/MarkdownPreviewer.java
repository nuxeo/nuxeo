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
 *     Andre Justo
 */

package org.nuxeo.ecm.platform.preview.adapter;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.preview.api.PreviewException;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @since 8.2
 */
public class MarkdownPreviewer extends AbstractPreviewer implements MimeTypePreviewer {

    @Override
    public List<Blob> getPreview(Blob blob, DocumentModel dm) throws PreviewException {
        List<Blob> blobResults = new ArrayList<>();
        String basePath = VirtualHostHelper.getContextPathProperty();
        StringBuffer html = new StringBuffer();
        html.append("<html><head>");
        html.append("<title>" + getPreviewTitle(dm) + "</title>");
        html.append(String.format("<script src=\"%s/bower_components/webcomponentsjs/webcomponents-lite.js\"></script>", basePath));
        html.append(String.format("<link rel=\"import\" href=\"%s/viewers/marked-element.vulcanized.html\">", basePath));
        try {
            html.append("<marked-element>");
            html.append("<div class=\"markdown-html\"></div>");
            html.append("<script type=\"text/markdown\">");
            html.append(blob.getString());
            html.append("</script>");
            html.append("</marked-element>");
            html.append("</body>");
            Blob mainBlob = Blobs.createBlob(html.toString(), "text/html", null, "index.html");
            blobResults.add(mainBlob);
            return blobResults;
        } catch (IOException e) {
            throw new PreviewException(e);
        }
    }
}
