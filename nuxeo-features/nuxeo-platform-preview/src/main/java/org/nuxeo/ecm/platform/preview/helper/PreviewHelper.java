/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.preview.helper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;

public class PreviewHelper {

    public static final String PREVIEWURL_PREFIX = "restAPI/preview/";
    public static final String PREVIEWURL_DEFAULTXPATH = "default";

    protected static final Map<String, Boolean> hasPreviewByType = new ConcurrentHashMap<String, Boolean>();

    private PreviewHelper() {
    }

    public static String getPreviewURL(DocumentModel doc) {
        return getPreviewURL(doc, PREVIEWURL_DEFAULTXPATH);
    }

    public static String getPreviewURL(DocumentModel doc, String xpath) {
        if (xpath == null) {
            xpath = PREVIEWURL_DEFAULTXPATH;
        }

        StringBuilder sb = new StringBuilder();

        sb.append(PREVIEWURL_PREFIX);
        sb.append(doc.getRepositoryName());
        sb.append("/");
        sb.append(doc.getId());
        sb.append("/");
        sb.append(xpath);
        sb.append("/");

        return sb.toString();
    }

    public static DocumentRef getDocumentRefFromPreviewURL(String url) {
        if (url == null) {
            return null;
        }

        String[] urlParts = url.split(PREVIEWURL_PREFIX);
        String[] parts = urlParts[1].split("/");
        String strRef = parts[1];
        return new IdRef(strRef);
    }

    public static boolean typeSupportsPreview(DocumentModel doc) {
        String docType = doc.getType();
        if (hasPreviewByType.containsKey(docType)) {
            return hasPreviewByType.get(docType);
        } else {
            HtmlPreviewAdapter adapter = doc.getAdapter(HtmlPreviewAdapter.class);
            if (adapter == null) {
                synchronized (hasPreviewByType) {
                    hasPreviewByType.put(docType, false);
                    return false;
                }
            } else {
                synchronized (hasPreviewByType) {
                    hasPreviewByType.put(docType, true);
                    return true;
                }
            }
        }
    }

}
