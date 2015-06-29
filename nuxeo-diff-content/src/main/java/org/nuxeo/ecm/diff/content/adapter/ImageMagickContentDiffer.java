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
 */

package org.nuxeo.ecm.diff.content.adapter;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.diff.content.ContentDiffException;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

/**
 * Dummy content differ.
 * <p>
 * TODO Use ImageMagick to perform a real diff!
 *
 * @since 7.4
 */
public class ImageMagickContentDiffer implements MimeTypeContentDiffer {

    protected static final String NUXEO_DEFAULT_CONTEXT_PATH = "/nuxeo";

    @Override
    public List<Blob> getContentDiff(Blob leftBlob, Blob rightBlob, Locale locale) throws ContentDiffException {

        try {
            List<Blob> blobResults = new ArrayList<Blob>();
            StringWriter sw = new StringWriter();

            sw.write("<html>");
            sw.write("<body>");
            sw.write("<div>Hello, this is a <b>killer feature</b>!</div>");
            sw.write("</body>");
            sw.write("</html>");

            String stringBlob = sw.toString().replaceAll(NUXEO_DEFAULT_CONTEXT_PATH,
                    VirtualHostHelper.getContextPathProperty());
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
}
