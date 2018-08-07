/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.preview.helper;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.platform.preview.adapter.base.ConverterBasedHtmlPreviewAdapter;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;
import org.nuxeo.ecm.platform.preview.api.PreviewException;

public class PreviewHelper {

    public static final String REST_API_PREFIX = "site/api/v1";

    protected static final Map<String, Boolean> hasPreviewByType = new ConcurrentHashMap<>();

    private PreviewHelper() {
    }

    public static String getPreviewURL(DocumentModel doc) {
        return getPreviewURL(doc, null);
    }

    public static String getPreviewURL(DocumentModel doc, String xpath) {
        StringJoiner sj = new StringJoiner("/", "", "/") // add trailing slash
            .add(REST_API_PREFIX)
            .add("repo").add(doc.getRepositoryName())
            .add("id").add(doc.getId());
        if (xpath != null) {
            sj.add("@blob").add(xpath);
        }
        String result = sj.add("@preview").toString();
        String ct = doc.getChangeToken();
        if (StringUtils.isNotBlank(ct)) {
            try {
                result += "?" + CoreSession.CHANGE_TOKEN + "=" + URLEncoder.encode(ct, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new PreviewException(e);
            }
        }
        return result;
    }

    public static boolean typeSupportsPreview(DocumentModel doc) {
        String docType = doc.getType();
        if (hasPreviewByType.containsKey(docType)) {
            return hasPreviewByType.get(docType);
        } else {
            HtmlPreviewAdapter adapter = doc.getAdapter(HtmlPreviewAdapter.class);
            if (adapter == null) {
                hasPreviewByType.put(docType, false);
                return false;
            } else {
                hasPreviewByType.put(docType, true);
                return true;
            }
        }
    }

    /**
     * @param document
     * @throws PreviewException
     * @since 5.7.3
     */
    public static boolean docHasBlobToPreview(DocumentModel document) throws PreviewException {
        HtmlPreviewAdapter adapter = document.getAdapter(HtmlPreviewAdapter.class);
        return adapter != null && adapter.hasBlobToPreview();
    }

    /**
     * @since 8.2
     */
    public static boolean blobSupportsPreview(DocumentModel doc, String xpath) {
        if (isBlobHolder(doc, xpath)) {
            xpath = null;
        }
        HtmlPreviewAdapter adapter = getBlobPreviewAdapter(doc);
        return adapter != null && adapter.hasPreview(xpath);
    }

    /**
     * @since 8.2
     */
    public static HtmlPreviewAdapter getBlobPreviewAdapter(DocumentModel doc) {
        ConverterBasedHtmlPreviewAdapter adapter = new ConverterBasedHtmlPreviewAdapter();
        adapter.setAdaptedDocument(doc);
        return adapter;
    }

    private static boolean isBlobHolder(DocumentModel doc, String xpath) {
        DocumentBlobHolder bh = (DocumentBlobHolder) doc.getAdapter(BlobHolder.class);
        return bh != null && bh.getXpath().equals(xpath);
    }
}
