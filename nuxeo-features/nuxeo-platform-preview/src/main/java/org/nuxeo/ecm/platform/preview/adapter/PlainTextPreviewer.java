/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.preview.api.PreviewException;

import com.ibm.icu.text.CharsetDetector;

public class PlainTextPreviewer extends AbstractPreviewer implements
        MimeTypePreviewer {

    protected String htmlContent(String content) {
        return "<pre>"
                + content.replace("&", "&amp;").replace("<", "&lt;").replace(
                        ">", "&gt;").replace("\'", "&apos;").replace("\"",
                        "&quot;").replace("\n", "<br/>") + "</pre>";
    }

    @Override
    public List<Blob> getPreview(Blob blob, DocumentModel dm)
            throws PreviewException {
        List<Blob> blobResults = new ArrayList<Blob>();

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

        String content = null;
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

        Blob mainBlob = new StringBlob(htmlPage.toString(), "text/html", "UTF-8");
        mainBlob.setFilename("index.html");

        blobResults.add(mainBlob);
        return blobResults;
    }

}
