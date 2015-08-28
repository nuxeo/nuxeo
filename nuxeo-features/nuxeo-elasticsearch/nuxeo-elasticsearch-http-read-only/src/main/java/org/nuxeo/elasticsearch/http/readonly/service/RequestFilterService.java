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

    protected Map<String, Class> requestFilters;

    @Override
    public void activate(ComponentContext context) {
        requestFilters = new ConcurrentHashMap<String, Class>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        requestFilters.clear();
        requestFilters = null;
    }

    public Map<String, Class> getRequestFilters() {
        return requestFilters;

    }

    public SearchRequestFilter getRequestFilters(String indices) throws InstantiationException, IllegalAccessException {
        Class clazz = requestFilters.get(indices);
        if (clazz == null) {
            return null;
        }
        return (SearchRequestFilter) clazz.newInstance();
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
            Class filter = requestFilters.remove(des.getIndex());
            if (filter != null) {
                log.info("Unregistered filter: " + filter + " for index " + des.getIndex());
            }
        }
    }

}
