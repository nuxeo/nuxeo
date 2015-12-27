/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:glefevre@nuxeo.com">Gildas</a>
 */
package org.nuxeo.ecm.user.center;

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
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.service.AbstractDocumentViewCodec;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract class for the User and Group codec.
 *
 * @since 6.0
 */
public abstract class AbstractUserGroupCodec extends AbstractDocumentViewCodec {

    public static final String DEFAULT_VIEW_ID = "view_home";

    // prefix/groupname/view_id?requestParams
    public static final String GET_URL_PATTERN = "/" // slash
            + "([a-zA-Z_0-9\\-\\.@]*)?" // username (group 1)
            + "(/([a-zA-Z_0-9\\-\\.]*))?" // view id (group 3) (optional)
            + "/?" // final slash (optional)
            + "(\\?((.*)?))?"; // query (group 5) (optional)

    /**
     * Get the DocumentView for a user or a group from a URL.
     *
     * @param url
     * @param defaultTab
     * @param paramIdName
     * @param paramShowName
     * @return
     */
    public DocumentView getDocumentViewFromUrl(String url, String defaultTab, String paramIdName, String paramShowName) {
        Pattern pattern = Pattern.compile(getPrefix() + GET_URL_PATTERN);
        Matcher m = pattern.matcher(url);
        if (m.matches()) {
            if (m.groupCount() >= 1) {
                String id = m.group(1);

                String viewId = m.group(3);
                if (viewId == null || "".equals(viewId)) {
                    viewId = DEFAULT_VIEW_ID;
                }

                String query = m.group(5);
                Map<String, String> params = URIUtils.getRequestParameters(query);
                if (params == null) {
                    params = new HashMap<String, String>();
                }

                params.put(paramIdName, id);
                params.put(paramShowName, "true");

                if (!params.containsKey("tabIds")) {
                    params.put("tabIds", defaultTab);
                }

                final DocumentLocation docLoc = new DocumentLocationImpl(getDefaultRepositoryName(), null);
                return new DocumentViewImpl(docLoc, viewId, params);
            }
        }
        return null;
    }

    /**
     * Get the url from a DocumentView for a user or a group.
     *
     * @param docView
     * @param id
     * @return
     */
    public String getUrlFromDocumentViewAndID(DocumentView docView, String paramName) {
        String id = docView.getParameter(paramName);
        if (id != null) {
            docView.removeParameter(paramName);
            List<String> items = new ArrayList<String>();
            items.add(getPrefix());
            items.add(URIUtils.quoteURIPathComponent(id, true, false));
            String viewId = docView.getViewId();
            if (viewId != null) {
                items.add(viewId);
            }
            String uri = StringUtils.join(items, "/");
            Map<String, String> parameters = docView.getParameters();
            if (parameters == null) {
                parameters = new HashMap<String, String>();
            }
            return URIUtils.addParametersToURIQuery(uri, parameters);
        }
        return null;
    }

    protected String getDefaultRepositoryName() {
        if (Framework.isInitialized()) {
            return Framework.getService(RepositoryManager.class).getDefaultRepositoryName();
        } else {
            // unit tests
            return null;
        }
    }
}
