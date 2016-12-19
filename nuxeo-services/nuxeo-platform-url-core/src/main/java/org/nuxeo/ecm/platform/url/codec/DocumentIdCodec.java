/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.url.codec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.service.AbstractDocumentViewCodec;

/**
 * Codec handling a document repository, id, view and additional request parameters.
 *
 * @author Anahide Tchertchian
 */
public class DocumentIdCodec extends AbstractDocumentViewCodec {

    public static final String PREFIX = "nxdoc";

    // nxdoc/server/docId/view_id/?requestParams
    public static final String URLPattern = "/(\\w+)/([a-zA-Z_0-9\\-]+)(/([a-zA-Z_0-9\\-\\.;=]*))?(/)?(\\?(.*)?)?";

    public DocumentIdCodec() {
    }

    public DocumentIdCodec(String prefix) {
    }

    @Override
    public String getPrefix() {
        if (prefix != null) {
            return prefix;
        }
        return PREFIX;
    }

    @Override
    public String getUrlFromDocumentView(DocumentView docView) {
        DocumentLocation docLoc = docView.getDocumentLocation();
        if (docLoc != null) {
            List<String> items = new ArrayList<>();
            items.add(getPrefix());
            items.add(docLoc.getServerName());
            IdRef docRef = docLoc.getIdRef();
            if (docRef == null) {
                return null;
            }
            items.add(docRef.toString());
            String viewId = docView.getViewId();
            if (viewId != null) {
                items.add(viewId);
            }
            String uri = String.join("/", items);
            return URIUtils.addParametersToURIQuery(uri, docView.getParameters());
        }
        return null;
    }

    /**
     * Extracts document location from a Zope-like URL ie: server/path_or_docId/view_id/tab_id .
     */
    @Override
    public DocumentView getDocumentViewFromUrl(String url) {
        final Pattern pattern = Pattern.compile(getPrefix() + URLPattern);
        Matcher m = pattern.matcher(url);
        if (m.matches()) {
            if (m.groupCount() >= 4) {

                // for debug
                // for (int i = 1; i < m.groupCount() + 1; i++) {
                // System.err.println(i + ": " + m.group(i));
                // }

                final String server = m.group(1);
                String uuid = m.group(2);
                final DocumentRef docRef = new IdRef(uuid);
                String viewId = m.group(4);
                if (viewId != null) {
                    int jsessionidIndex = viewId.indexOf(";jsessionid");
                    if (jsessionidIndex != -1) {
                        viewId = viewId.substring(0, jsessionidIndex);
                    }
                }

                // get other parameters

                Map<String, String> params = null;
                if (m.groupCount() > 6) {
                    String query = m.group(7);
                    params = URIUtils.getRequestParameters(query);
                }

                final DocumentLocation docLoc = new DocumentLocationImpl(server, docRef);

                return new DocumentViewImpl(docLoc, viewId, params);
            }
        }

        return null;
    }

}
