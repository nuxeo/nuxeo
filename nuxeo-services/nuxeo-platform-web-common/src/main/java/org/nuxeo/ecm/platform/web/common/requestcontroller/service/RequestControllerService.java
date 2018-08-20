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
 * $Id$
 */

package org.nuxeo.ecm.platform.web.common.requestcontroller.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentInstance;
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

    private static final Log log = LogFactory.getLog(RequestControllerService.class);

    protected final Map<String, FilterConfigDescriptor> grantPatterns = new LinkedHashMap<String, FilterConfigDescriptor>();

    protected final Map<String, FilterConfigDescriptor> denyPatterns = new LinkedHashMap<String, FilterConfigDescriptor>();

    // @GuardedBy("itself")
    protected final Map<String, RequestFilterConfig> configCache = new LRUCachingMap<String, RequestFilterConfig>(250);

    protected final NuxeoCorsFilterDescriptorRegistry corsFilterRegistry = new NuxeoCorsFilterDescriptorRegistry();

    protected final NuxeoHeaderDescriptorRegistry headersRegistry = new NuxeoHeaderDescriptorRegistry();

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (FILTER_CONFIG_EP.equals(extensionPoint)) {
            FilterConfigDescriptor desc = (FilterConfigDescriptor) contribution;
            registerFilterConfig(desc);
        } else if (CORS_CONFIG_EP.equals(extensionPoint)) {
            corsFilterRegistry.addContribution((NuxeoCorsFilterDescriptor) contribution);
        } else if (HEADERS_CONFIG_EP.equals(extensionPoint)) {
            headersRegistry.addContribution((NuxeoHeaderDescriptor) contribution);
        } else {
            log.error("Unknown ExtensionPoint " + extensionPoint);
        }
    }

    public void registerFilterConfig(String name, String pattern, boolean grant, boolean tx, boolean sync,
            boolean cached, boolean isPrivate, String cacheTime) {
        FilterConfigDescriptor desc = new FilterConfigDescriptor(name, pattern, grant, tx, sync, cached, isPrivate,
                cacheTime);
        registerFilterConfig(desc);
    }

    public void registerFilterConfig(FilterConfigDescriptor desc) {
        if (desc.isGrantRule()) {
            grantPatterns.put(desc.getName(), desc);
            log.debug("Registered grant filter config");
        } else {
            denyPatterns.put(desc.getName(), desc);
            log.debug("Registered deny filter config");
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (CORS_CONFIG_EP.equals(extensionPoint)) {
            corsFilterRegistry.removeContribution((NuxeoCorsFilterDescriptor) contribution);
        }
    }

    /* Service interface */

    @Override
    public CORSFilter getCorsFilterForRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        NuxeoCorsFilterDescriptor descriptor = corsFilterRegistry.getFirstMatchingDescriptor(uri);
        return descriptor == null ? null : descriptor.getFilter();
    }

    @Override
    @Deprecated
    public FilterConfig getCorsConfigForRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        NuxeoCorsFilterDescriptor descriptor = corsFilterRegistry.getFirstMatchingDescriptor(uri);
        return descriptor != null ? descriptor.buildFilterConfig() : null;
    }

    @Override
    public RequestFilterConfig getConfigForRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
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
        Map<String, String> headersCache = new HashMap<String, String>();
        for (NuxeoHeaderDescriptor header : headersRegistry.descs.values()) {
            if (header.isEnabled()) {
                headersCache.put(header.name, header.getValue());
            }
        }
        return headersCache;
    }
}
