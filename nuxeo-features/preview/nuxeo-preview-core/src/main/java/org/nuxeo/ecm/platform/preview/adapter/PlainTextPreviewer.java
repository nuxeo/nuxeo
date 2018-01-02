/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.preview.adapter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.preview.api.PreviewException;

import com.ibm.icu.text.CharsetDetector;

public class PlainTextPreviewer extends AbstractPreviewer implements MimeTypePreviewer {

    public String htmlContent(String content) {
        // & and < are the only two that we really need to escape
        // then we also make newlines visible
        String escaped = StringUtils.replaceEach(content, //
                new String[] { "&", "<", "\n" }, //
                new String[] { "&amp;", "&lt;", "<br/>" });
        return "<pre>" + escaped + "</pre>";
    }

    @Override
    public List<Blob> getPreview(Blob blob, DocumentModel dm) throws PreviewException {
        List<Blob> blobResults = new ArrayList<>();

        StringBuilder htmlPage = new StringBuilder();

        byte[] data;
        try {
            data = blob.getByteArray();
        } catch (IOException e) {
            throw new PreviewException("Cannot fetch blob content", e);
        }
        String encoding = blob.getEncoding();
        if (StringUtils.isEmpty(encoding)) {
            CharsetDetector detector = new CharsetDetector();
            detector.setText(data);
            encoding = detector.detect().getName();
        }

        String content;
        try {
            content = new String(data, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new PreviewException("Cannot encode blob content to string", e);
        }

        htmlPage.append("<?xml version=\"1.0\" encoding=\"UTF-8\"/>");
        htmlPage.append("<html>");
        htmlPage.append("<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/></head>");
        htmlPage.append("<body>");
        htmlPage.append(htmlContent(content));
        htmlPage.append("</body></html>");

        Blob mainBlob = Blobs.createBlob(htmlPage.toString(), "text/html", "UTF-8", "index.html");

        blobResults.add(mainBlob);
        return blobResults;
    }

}
