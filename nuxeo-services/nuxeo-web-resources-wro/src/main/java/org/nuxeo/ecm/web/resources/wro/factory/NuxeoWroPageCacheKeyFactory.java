/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.web.resources.wro.factory;

import static org.apache.commons.lang3.Validate.notNull;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;

import ro.isdc.wro.cache.CacheKey;
import ro.isdc.wro.cache.factory.DefaultCacheKeyFactory;
import ro.isdc.wro.config.ReadOnlyContext;
import ro.isdc.wro.model.group.DefaultGroupExtractor;
import ro.isdc.wro.model.group.GroupExtractor;
import ro.isdc.wro.model.group.Inject;
import ro.isdc.wro.model.resource.ResourceType;

/**
 * Cache key extractor generating a key depending on all request parameters, and accepting slash character for page
 * names.
 *
 * @since 7.10
 */
public class NuxeoWroPageCacheKeyFactory extends DefaultCacheKeyFactory {

    private static final Log log = LogFactory.getLog(NuxeoWroPageCacheKeyFactory.class);

    protected static final String URI_MARKER = "/resource/page/";

    @Inject
    private GroupExtractor groupExtractor;

    @Inject
    private ReadOnlyContext context;

    @Override
    public CacheKey create(HttpServletRequest request) {
        notNull(request);
        CacheKey key = null;

        GroupExtractor ext = new DefaultGroupExtractor() {

            @Override
            public String getGroupName(HttpServletRequest request) {
                Validate.notNull(request);
                String uri = request.getRequestURI();
                // check if include or uri path are present and use one of these as request uri.
                final String includeUriPath = (String) request.getAttribute(ATTR_INCLUDE_PATH);
                uri = includeUriPath != null ? includeUriPath : uri;
                final String groupName = FilenameUtils.removeExtension(getPageName(stripSessionID(uri)));
                return StringUtils.isEmpty(groupName) ? null : groupName;
            }

            private String stripSessionID(final String uri) { // NOSONAR
                if (uri == null) {
                    return null;
                }
                return uri.replaceFirst("(?i)(;jsessionid.*)", "");
            }

            protected String getPageName(String filename) {
                if (filename == null) {
                    return null;
                }
                int index = filename.indexOf(URI_MARKER);
                if (index != -1) {
                    return filename.substring(index + URI_MARKER.length());
                }
                return null;
            }

        };

        final String groupName = ext.getGroupName(request);
        final ResourceType resourceType = groupExtractor.getResourceType(request);
        final boolean minimize = isMinimized(request);
        if (groupName != null && resourceType != null) {
            key = new CacheKey(groupName, resourceType, minimize);
        }

        if (key != null) {
            // add additional attributes for request-related info
            Map<String, String> params = URIUtils.getRequestParameters(request.getQueryString());
            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    key.addAttribute(entry.getKey(), entry.getValue());
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("Cache key for request '%s' '%s': %s", request.getRequestURL(),
                    request.getQueryString(), key));
        }
        return key;
    }

    /**
     * Uses isMinimizeEnabled configuration to compute minimize value.
     */
    private boolean isMinimized(final HttpServletRequest request) { // NOSONAR
        return context.getConfig().isMinimizeEnabled() ? groupExtractor.isMinimized(request) : false;
    }

}
