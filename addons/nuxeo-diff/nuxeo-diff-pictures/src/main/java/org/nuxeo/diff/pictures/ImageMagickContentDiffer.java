/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
