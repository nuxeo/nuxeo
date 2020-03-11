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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.elasticsearch.http.readonly.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.elasticsearch.http.readonly.filter.SearchRequestFilter;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 7.4
 */
public class RequestFilterService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(ComponentName.DEFAULT_TYPE,
            "org.nuxeo.elasticsearch.http.readonly.RequestFilterService");

    private static final Log log = LogFactory.getLog(RequestFilterService.class);

    protected static final String FILTER_EXT_POINT = "filters";

    protected Map<String, Class<? extends SearchRequestFilter>> requestFilters;

    @Override
    public void activate(ComponentContext context) {
        requestFilters = new ConcurrentHashMap<>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        requestFilters.clear();
        requestFilters = null;
    }

    public Map<String, Class<? extends SearchRequestFilter>> getRequestFilters() {
        return requestFilters;

    }

    public SearchRequestFilter getRequestFilters(String indices) throws ReflectiveOperationException {
        Class<? extends SearchRequestFilter> clazz = requestFilters.get(indices);
        if (clazz == null) {
            return null;
        }
        return clazz.getDeclaredConstructor().newInstance();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (FILTER_EXT_POINT.equals(extensionPoint)) {
            RequestFilterDescriptor des = (RequestFilterDescriptor) contribution;
            requestFilters.put(des.getIndex(), des.getFilterClass());
            log.info("Registered filter: " + des.getFilterClass() + " for index " + des.getIndex());
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (FILTER_EXT_POINT.equals(extensionPoint)) {
            RequestFilterDescriptor des = (RequestFilterDescriptor) contribution;
            Class<? extends SearchRequestFilter> filter = requestFilters.remove(des.getIndex());
            if (filter != null) {
                log.info("Unregistered filter: " + filter + " for index " + des.getIndex());
            }
        }
    }

}
