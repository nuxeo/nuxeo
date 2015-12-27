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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;

import ro.isdc.wro.cache.CacheKey;
import ro.isdc.wro.cache.factory.DefaultCacheKeyFactory;
import ro.isdc.wro.config.ReadOnlyContext;
import ro.isdc.wro.model.group.GroupExtractor;
import ro.isdc.wro.model.group.Inject;

/**
 * Cache key extractor generating a key depending on all request parameters.
 *
 * @since 7.3
 */
public class NuxeoWroCacheKeyFactory extends DefaultCacheKeyFactory {

    private static final Log log = LogFactory.getLog(NuxeoWroCacheKeyFactory.class);

    @Inject
    private GroupExtractor groupExtractor;

    @Inject
    private ReadOnlyContext context;

    @Override
    public CacheKey create(HttpServletRequest request) {
        notNull(request);
        // this requires a type to be detected for now
        CacheKey key = super.create(request);
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

}
