/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 *     Thibaud Arguillere
 */

package org.nuxeo.diff.pictures;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.diff.content.ContentDiffException;
import org.nuxeo.ecm.diff.content.adapter.MimeTypeContentDiffer;

/**
 * ImageMagickContentDiffer
 *
 * @since 7.4
 */
public class ImageMagickContentDiffer implements MimeTypeContentDiffer {

    @Override
    public List<Blob> getContentDiff(DocumentModel leftDoc, DocumentModel rightDoc, String xpath, Locale locale)
            throws ContentDiffException {

        try {
            List<Blob> blobResults = new ArrayList<Blob>();
            StringWriter sw = new StringWriter();

            String html = DiffPictures.buildDiffHtml(leftDoc, rightDoc, xpath);
            sw.write(html);

            String stringBlob = sw.toString();
            Blob mainBlob = Blobs.createBlob(stringBlob);
            sw.close();

            mainBlob.setFilename("contentDiff.html");
            mainBlob.setMimeType("text/html");

            blobResults.add(mainBlob);
            return blobResults;

        } catch (Exception e) {
            throw new ContentDiffException(e);
        }
    }

    @Override
    public List<Blob> getContentDiff(Blob leftBlob, Blob rightBlob, Locale locale) throws ContentDiffException {
        throw new UnsupportedOperationException("ImageMagickContentDiffer can handle only DocumentModel");
    }
}
