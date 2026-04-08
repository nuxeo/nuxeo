/*
 * (C) Copyright 2006-2024 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.web.common.requestcontroller.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.nuxeo.common.collections.CircularLinkedHashMap;
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

    /** @since 6.0 */
    public static final String HEADERS_CONFIG_EP = "responseHeaders";

    protected final Map<String, RequestFilterConfig> configCache = new CircularLinkedHashMap<>(250, 1.0f, true);

    protected Map<String, FilterConfigDescriptor> grantPatterns;

    protected Map<String, FilterConfigDescriptor> denyPatterns;

    protected List<NuxeoCorsFilterDescriptor> corsFilters;

    protected Map<String, String> headerValues;

    @Override
    public void start(ComponentContext context) {
        var filterConfigs = this.<FilterConfigDescriptor> getDescriptors(FILTER_CONFIG_EP)
                                .stream()
                                .collect(Collectors.partitioningBy(FilterConfigDescriptor::isGrantRule,
                                        Collectors.toMap(FilterConfigDescriptor::getName, Function.identity(),
                                                (a, b) -> b, LinkedHashMap::new)));
        grantPatterns = filterConfigs.get(Boolean.TRUE);
        denyPatterns = filterConfigs.get(Boolean.FALSE);
        corsFilters = this.<NuxeoCorsFilterDescriptor> getDescriptors(CORS_CONFIG_EP)
                          .stream()
                          .filter(NuxeoCorsFilterDescriptor::isEnabled)
                          .toList();
        headerValues = this.<NuxeoHeaderDescriptor> getDescriptors(HEADERS_CONFIG_EP)
                           .stream()
                           .filter(NuxeoHeaderDescriptor::isEnabled)
                           .collect(Collectors.toMap(NuxeoHeaderDescriptor::getName, NuxeoHeaderDescriptor::getValue));
    }

    @Override
    public void stop(ComponentContext context) {
        corsFilters = null;
    }

    /* Service interface */

    @Override
    public CORSFilter getCorsFilterForRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return corsFilters.stream()
                          .filter(descriptor -> descriptor.pattern == null || descriptor.pattern.matcher(uri).matches())
                          .findFirst()
                          .map(NuxeoCorsFilterDescriptor::getFilter)
                          .orElse(null);
    }

    @Override
    public RequestFilterConfig getConfigForRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        if (queryString != null) {
            uri += '?' + queryString;
        }
        RequestFilterConfig config;

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
        return headerValues;
    }
}
