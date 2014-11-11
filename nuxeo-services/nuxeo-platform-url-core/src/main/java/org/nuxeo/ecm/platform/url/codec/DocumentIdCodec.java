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
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: DocumentIdCodec.java 22535 2007-07-13 14:57:58Z atchertchian $
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
 * Codec handling a document repository, id, view and additional request
 * parameters.
 *
 * @author Anahide Tchertchian
 */
public class DocumentIdCodec extends AbstractDocumentViewCodec {

    public static final String PREFIX = "nxdoc";

    // nxdoc/server/docId/view_id/?requestParams
    public static final String URLPattern
            = "/(\\w+)/([a-zA-Z_0-9\\-]+)(/([a-zA-Z_0-9\\-\\.]*))?(/)?(\\?(.*)?)?";

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

    public String getUrlFromDocumentView(DocumentView docView) {
        DocumentLocation docLoc = docView.getDocumentLocation();
        if (docLoc != null) {
            List<String> items = new ArrayList<String>();
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
            String uri = StringUtils.join(items, "/");
            return URIUtils.addParametersToURIQuery(uri,
                    docView.getParameters());
        }
        return null;
    }

    /**
     * Extracts document location from a Zope-like URL ie:
     * server/path_or_docId/view_id/tab_id .
     */
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
                final String viewId = m.group(4);

                // get other parameters

                Map<String, String> params = null;
                if (m.groupCount() > 6) {
                    String query = m.group(7);
                    params = URIUtils.getRequestParameters(query);
                }

                final DocumentLocation docLoc = new DocumentLocationImpl(
                        server, docRef);

                return new DocumentViewImpl(docLoc, viewId, params);
            }
        }

        return null;
    }

}
