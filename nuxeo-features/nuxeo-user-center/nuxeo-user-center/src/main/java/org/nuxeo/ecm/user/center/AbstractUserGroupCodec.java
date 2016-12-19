/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.service.AbstractDocumentViewCodec;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Abstract class for the User and Group codec.
 *
 * @since 6.0
 */
public abstract class AbstractUserGroupCodec extends AbstractDocumentViewCodec {

    public static final String DEFAULT_VIEW_ID = "view_home";

    public static final String ALLOWED_CHARACTERS_REGEX = "nuxeo.codec.usergroup.allowedCharacters";

    /**
     * Get the DocumentView for a user or a group from a URL.
     */
    public DocumentView getDocumentViewFromUrl(String url, String defaultTab, String paramIdName, String paramShowName) {
        ConfigurationService cs = Framework.getService(ConfigurationService.class);
        String allowedCharsRegex = cs.getProperty(ALLOWED_CHARACTERS_REGEX);
        String userGroupNameRegex = String.format("(%s)?", allowedCharsRegex);

        // prefix/groupname/view_id?requestParams
        String url_pattern = "/" // slash
            + userGroupNameRegex // username/groupname (group 1)
            + "(/([a-zA-Z_0-9\\-\\.]*))?" // view id (group 3) (optional)
            + "/?" // final slash (optional)
            + "(\\?((.*)?))?"; // query (group 5) (optional)

        Pattern pattern = Pattern.compile(getPrefix() + url_pattern);
        try {
            url = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new NuxeoException("Unable to decode the requested url", e);
        }
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
                    params = new HashMap<>();
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
     */
    public String getUrlFromDocumentViewAndID(DocumentView docView, String paramName) {
        String id = docView.getParameter(paramName);
        if (id != null) {
            docView.removeParameter(paramName);
            List<String> items = new ArrayList<>();
            items.add(getPrefix());
            items.add(URIUtils.quoteURIPathComponent(id, true, false));
            String viewId = docView.getViewId();
            if (viewId != null) {
                items.add(viewId);
            }
            String uri = String.join("/", items);
            Map<String, String> parameters = docView.getParameters();
            if (parameters == null) {
                parameters = new HashMap<>();
            }
            return URIUtils.addParametersToURIQuery(uri, parameters);
        }
        return null;
    }

    protected String getDefaultRepositoryName() {
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        return repositoryManager == null ? null : repositoryManager.getDefaultRepositoryName();
    }
}
