/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     "Guillaume Renard"
 */

package org.nuxeo.elasticsearch;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.nuxeo.elasticsearch.listener.ElasticSearchInlineListener;

/**
 * @since 8.3
 */
public class ElasticSearchFilter implements Filter {

    public static final String ES_SYNC_FLAG = "nx-es-sync";

    protected static final String ES_SYNC_FLAG_COMPAT = "nx_es_sync";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final boolean esSync = Boolean.parseBoolean(httpRequest.getHeader(ES_SYNC_FLAG))
                // NXP-28075: keep compatibility with old header with underscores
                || Boolean.parseBoolean(httpRequest.getHeader(ES_SYNC_FLAG_COMPAT));
        if (!esSync) {
            chain.doFilter(request, response);
            return;
        }
        ElasticSearchInlineListener.useSyncIndexing.set(true);
        try {
            chain.doFilter(request, response);
        } finally {
            ElasticSearchInlineListener.useSyncIndexing.set(false);
        }
    }

    @Override
    public void destroy() {
    }

}
