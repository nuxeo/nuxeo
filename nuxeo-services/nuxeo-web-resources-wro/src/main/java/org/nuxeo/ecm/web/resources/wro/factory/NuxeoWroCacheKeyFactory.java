/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
