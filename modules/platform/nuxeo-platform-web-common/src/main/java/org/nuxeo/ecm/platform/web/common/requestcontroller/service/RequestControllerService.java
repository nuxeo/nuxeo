/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.platform.web.common.requestcontroller.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

import com.thetransactioncompany.cors.CORSFilter;

/**
 * Runtime component that implements the {@link RequestControllerManager} interface. Contains both the Extension point
 * logic and the service implementation.
 *
 * @author tiry
 */
public class RequestControllerService extends DefaultComponent implements RequestControllerManager {

    public static final String FILTER_CONFIG_EP = "filterConfig";

    public static final String CORS_CONFIG_EP = "corsConfig";

    /**
     * @since 6.0
     */
    public static final String HEADERS_CONFIG_EP = "responseHeaders";

    protected Map<String, FilterConfigDescriptor> grantPatterns;

    protected Map<String, FilterConfigDescriptor> denyPatterns;

    protected Map<String, String> headers;

    // @GuardedBy("itself")
    protected final Map<String, RequestFilterConfig> configCache = new LRUCachingMap<>(250);

    @Override
    public void start(ComponentContext context) {
        List<FilterConfigDescriptor> filters = getRegistryContributions(FILTER_CONFIG_EP);
        grantPatterns = filters.stream()
                               .filter(FilterConfigDescriptor::isGrantRule)
                               .collect(Collectors.toMap(FilterConfigDescriptor::getName, Function.identity(),
                                       (e1, e2) -> e1, LinkedHashMap::new));
        denyPatterns = filters.stream()
                              .filter(Predicate.not(FilterConfigDescriptor::isGrantRule))
                              .collect(Collectors.toMap(FilterConfigDescriptor::getName, Function.identity(),
                                      (e1, e2) -> e1, LinkedHashMap::new));
        headers = this.<NuxeoHeaderDescriptor> getRegistryContributions(HEADERS_CONFIG_EP)
                      .stream()
                      .collect(Collectors.toMap(NuxeoHeaderDescriptor::getName, NuxeoHeaderDescriptor::getValue));
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        grantPatterns = null;
        denyPatterns = null;
        headers = null;
    }

    /* Service interface */

    protected NuxeoCorsFilterDescriptor getFirstMatchingDescriptor(String uri) {
        for (NuxeoCorsFilterDescriptor filterDesc : this.<NuxeoCorsFilterDescriptor> getRegistryContributions(
                CORS_CONFIG_EP)) {
            Pattern pattern = filterDesc.pattern;
            if (pattern == null || pattern.matcher(uri).matches()) {
                return filterDesc;
            }
        }
        return null;
    }

    @Override
    public CORSFilter getCorsFilterForRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        NuxeoCorsFilterDescriptor descriptor = getFirstMatchingDescriptor(uri);
        return descriptor == null ? null : descriptor.getFilter();
    }

    @Override
    @Deprecated(since = "10.1")
    public FilterConfig getCorsConfigForRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        NuxeoCorsFilterDescriptor descriptor = getFirstMatchingDescriptor(uri);
        return descriptor != null ? descriptor.buildFilterConfig() : null;
    }

    @Override
    public RequestFilterConfig getConfigForRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        if (queryString != null) {
            uri += '?' + queryString;
        }
        RequestFilterConfig config = null;

        synchronized (configCache) {
            config = configCache.get(uri);
        }
        if (config == null) {
            config = computeConfigForRequest(uri);
            synchronized (configCache) {
                configCache.put(uri, config);
            }
        }
        return config;
    }

    public RequestFilterConfig computeConfigForRequest(String uri) {
        // handle deny patterns
        for (FilterConfigDescriptor desc : denyPatterns.values()) {
            Pattern pat = desc.getCompiledPattern();
            Matcher m = pat.matcher(uri);
            if (m.matches()) {
                return new RequestFilterConfigImpl(false, false, false, false, false, "");
            }
        }

        // handle grant patterns
        for (FilterConfigDescriptor desc : grantPatterns.values()) {
            Pattern pat = desc.getCompiledPattern();
            Matcher m = pat.matcher(uri);
            if (m.matches()) {
                return new RequestFilterConfigImpl(desc.useSync(), desc.useTx(), desc.useTxBuffered(), desc.isCached(),
                        desc.isPrivate(), desc.getCacheTime());
            }
        }

        // return deny by default
        return new RequestFilterConfigImpl(false, false, false, false, false, "");
    }

    @Override
    public Map<String, String> getResponseHeaders() {
        return Collections.unmodifiableMap(headers);
    }
}
