/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thierry Delprat
 */

package org.nuxeo.wss.servlet.config;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.wss.fprpc.FPRPCConts;

public class FilterBindingResolver {

    // @GuardedBy("itself")
    protected static final Map<String, FilterBindingConfig> bindingCache = new LRUCachingMap<String, FilterBindingConfig>(
            50);

    public static FilterBindingConfig getBinding(HttpServletRequest request)
            throws Exception {

        String UA = request.getHeader("User-Agent");
        if (FPRPCConts.MSOFFICE_USERAGENT.equals(UA)) {
            FilterBindingConfig config = new FilterBindingConfig();
            config.setRequestType(FilterBindingConfig.GET_REQUEST_TYPE);
            config.setTargetService("VtiHandler");
            return config;
        }
        String uri = request.getRequestURI();
        return getBinding(uri);
    }

    public static FilterBindingConfig getBinding(String uri) throws Exception {
        FilterBindingConfig binding;
        synchronized(bindingCache) {
            binding = bindingCache.get(uri);
        }
        if (binding == null) {
            binding = computeBindingForRequest(uri);
            synchronized(bindingCache) {
                bindingCache.put(uri, binding);
            }
        }
        return binding;
    }

    protected static synchronized FilterBindingConfig computeBindingForRequest(String uri)
            throws Exception {
        List<FilterBindingConfig> bindings = XmlConfigHandler.getConfigEntries();
        for (FilterBindingConfig binding : bindings) {
            Pattern pat = binding.getUrlPattern();
            Matcher m = pat.matcher(uri);

            if (m.matches()) {
                if (m.groupCount() > 0) {
                    String site = m.group(1);
                    return new FilterBindingConfig(binding, site);
                } else {
                    return binding;
                }
            }
        }
        return null;
    }

}
