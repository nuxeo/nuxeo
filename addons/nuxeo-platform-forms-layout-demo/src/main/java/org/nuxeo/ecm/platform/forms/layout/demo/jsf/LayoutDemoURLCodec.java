/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.demo.jsf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.service.AbstractDocumentViewCodec;

/**
 * @author Anahide Tchertchian
 */
public class LayoutDemoURLCodec extends AbstractDocumentViewCodec {

    // prefix/view_id?requestParams
    public static final String GET_URL_PATTERN = "/([a-zA-Z_0-9\\-\\.]*)?(/)?(\\?(.*)?)?";

    // prefix/outcome/in/several/parts/template.faces?requestParams
    public static final String POST_URL_PATTERN = "/([a-zA-Z_0-9\\-\\./]*)?(.faces)(/)?(\\?(.*)?)?";

    public static final String DEFAULT_VIEW_ID = "layoutDemoIntroduction";

    public String getUrlFromDocumentView(DocumentView docView) {
        DocumentLocation docLoc = docView.getDocumentLocation();
        if (docLoc != null) {
            List<String> items = new ArrayList<String>();
            items.add(getPrefix());
            String viewId = docView.getViewId();
            if (viewId != null) {
                items.add(viewId);
            } else {
                items.add(DEFAULT_VIEW_ID);
            }
            String uri = StringUtils.join(items, "/");
            Map<String, String> params = new HashMap<String, String>();
            Map<String, String> docViewParams = docView.getParameters();
            if (docViewParams != null) {
                params.putAll(docViewParams);
                params.remove("conversationId");
            }
            return URIUtils.addParametersToURIQuery(uri, params);
        }
        return null;
    }

    /**
     * Extracts view id and parameters, for both get and post methods
     */
    public DocumentView getDocumentViewFromUrl(String url) {
        Pattern pattern = Pattern.compile(getPrefix() + GET_URL_PATTERN);
        Matcher m = pattern.matcher(url);
        if (m.matches()) {
            if (m.groupCount() >= 1) {

                // for debug
                // for (int i = 1; i < m.groupCount() + 1; i++) {
                // System.err.println(i + ": " + m.group(i));
                // }

                String viewId = m.group(1);
                if (viewId == null || "".equals(viewId)) {
                    viewId = DEFAULT_VIEW_ID;
                }

                // get other parameters

                Map<String, String> params = null;
                if (m.groupCount() > 3) {
                    String query = m.group(4);
                    params = URIUtils.getRequestParameters(query);
                    if (params != null) {
                        params.remove("conversationId");
                    }
                }

                final DocumentLocation docLoc = new DocumentLocationImpl(null,
                        null);

                return new DocumentViewImpl(docLoc, viewId, params);
            }
        }
        // try post
        pattern = Pattern.compile(getPrefix() + POST_URL_PATTERN);
        m = pattern.matcher(url);
        if (m.matches()) {
            if (m.groupCount() >= 1) {

                // for debug
                // for (int i = 1; i < m.groupCount() + 1; i++) {
                // System.err.println(i + ": " + m.group(i));
                // }

                // get other parameters

                Map<String, String> params = null;

                if (m.groupCount() > 4) {
                    String query = m.group(5);
                    params = URIUtils.getRequestParameters(query);
                    if (params != null) {
                        params.remove("conversationId");
                    }
                }

                final DocumentLocation docLoc = new DocumentLocationImpl(null,
                        null);

                return new DocumentViewImpl(docLoc, null, params);
            }
        }

        return null;
    }

}
