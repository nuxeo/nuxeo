/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.styleguide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.service.AbstractDocumentViewCodec;

/**
 * Provides clean urls without conversation id to handle navigation
 *
 * @since 5.7
 */
public class StyleGuideURLCodec extends AbstractDocumentViewCodec {

    public static final String DEFAULT_VIEW_ID = "style_guide";

    // prefix/menu_id/(tab/subtab)?requestParams
    public static final String GET_URL_PATTERN = "/" // slash
            + "([a-zA-Z_0-9\\-\\.]*)?" // menu tab id (group 1)
            + "(/([a-zA-Z_0-9\\-]*))?" // tab id (group 3) (optional)
            + "(/([a-zA-Z_0-9\\-]*))?" // sub tab id (group 4) (optional)
            + "(/)?" // final slash (optional)
            + "(\\?(.*)?)?"; // query (group 8) (optional)

    // prefix/outcome/in/several/parts/template.faces?requestParams
    public static final String POST_URL_PATTERN = "/style_guide.faces(/)?(\\?(.*)?)?";

    public String getUrlFromDocumentView(DocumentView docView) {
        DocumentLocation docLoc = docView.getDocumentLocation();
        if (docLoc != null) {
            List<String> items = new ArrayList<>();
            items.add(getPrefix());
            String viewId = docView.getViewId();
            if (viewId != null) {
                items.add(viewId);
            } else {
                items.add(DEFAULT_VIEW_ID);
            }
            Map<String, String> docViewParams = docView.getParameters();
            Map<String, String> params = new HashMap<>();
            if (docViewParams != null) {
                params.putAll(docViewParams);
                params.remove("conversationId");
                if (params.containsKey("tabId")) {
                    items.add(params.get("tabId"));
                    params.remove("tabId");
                    if (params.containsKey("subTabId")) {
                        items.add(params.get("subTabId"));
                        params.remove("subTabId");
                    }
                }
            }
            String uri = StringUtils.join(items, "/");
            return URIUtils.addParametersToURIQuery(uri, params);
        }
        return null;
    }

    /**
     * Extracts view id and parameters, for both get and post methods
     */
    public DocumentView getDocumentViewFromUrl(String url) {
        // strip url of any jsessionid param
        int jsessionidIndex = url.indexOf(";jsessionid");
        if (jsessionidIndex != -1) {
            url = url.substring(0, jsessionidIndex);
        }

        // try POST pattern -> FIXME this is not enough information to restore
        // a tab
        Pattern pattern = Pattern.compile(getPrefix() + POST_URL_PATTERN);
        Matcher m = pattern.matcher(url);
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

                final DocumentLocation docLoc = new DocumentLocationImpl(null, null);

                return new DocumentViewImpl(docLoc, null, params);
            }
        }
        // try GET pattern
        pattern = Pattern.compile(getPrefix() + GET_URL_PATTERN);
        m = pattern.matcher(url);
        if (m.matches()) {
            if (m.groupCount() >= 1) {

                // for debug
                // for (int i = 1; i < m.groupCount() + 1; i++) {
                // System.err.println(i + ": " + m.group(i));
                // }

                Map<String, String> params = new HashMap<>();
                String menuId = m.group(1);
                if (menuId != null && !"".equals(menuId)) {
                    params.put("menuId", menuId);
                }

                String tabId = m.group(3);
                if (tabId != null && !"".equals(tabId)) {
                    params.put("tabId", tabId);
                }

                String subTabId = m.group(5);
                if (subTabId != null && !"".equals(subTabId)) {
                    params.put("subTabId", subTabId);
                }

                // get other parameters

                if (m.groupCount() > 3) {
                    String query = m.group(8);
                    Map<String, String> queryParams = URIUtils.getRequestParameters(query);
                    if (queryParams != null) {
                        queryParams.remove("conversationId");
                        params.putAll(queryParams);
                    }
                }

                final DocumentLocation docLoc = new DocumentLocationImpl(null, null);

                return new DocumentViewImpl(docLoc, DEFAULT_VIEW_ID, params);
            }
        }

        return null;
    }

}
