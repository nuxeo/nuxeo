/*
 * (C) Copyright 2013-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.web.common.requestcontroller.service;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Pattern;

import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.Descriptor;

import com.thetransactioncompany.cors.CORSFilter;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.7.2
 */
@XObject(value = "corsConfig")
public class NuxeoCorsFilterDescriptor implements Descriptor {

    private static final String PROPERTIES_PREFIX = "cors.";

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected Boolean enabled = true;

    @XNode("@allowGenericHttpRequests")
    protected Boolean allowGenericHttpRequests = true;

    @XNode("@allowOrigin")
    protected String allowOrigin;

    @XNode("@allowSubdomains")
    protected boolean allowSubdomains = false;

    @XNode("@supportedMethods")
    protected String supportedMethods;

    @XNode("@supportedHeaders")
    protected String supportedHeaders;

    @XNode("@exposedHeaders")
    protected String exposedHeaders;

    @XNode("@supportsCredentials")
    protected Boolean supportsCredentials = true;

    @XNode("@maxAge")
    protected int maxAge = -1;

    protected Pattern pattern;

    @XNode("pattern")
    protected void setPattern(String patternString) {
        patternString = Framework.expandVars(patternString);
        if (!StringUtils.isBlank(patternString)) {
            pattern = Pattern.compile(patternString);
        }
    }

    // volatile for double-checked locking
    protected volatile CORSFilter filter;

    @Override
    public String getId() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public CORSFilter getFilter() {
        if (filter == null) {
            synchronized (this) {
                if (filter == null) {
                    CORSFilter corsFilter = new CORSFilter();
                    try {
                        corsFilter.init(buildFilterConfig());
                    } catch (ServletException e) {
                        throw new NuxeoException(e);
                    }
                    filter = corsFilter;
                }
            }
        }
        return filter;
    }

    protected FilterConfig buildFilterConfig() {
        Dictionary<String, String> parameters = buildDictionary();

        return new FilterConfig() {
            @Override
            public String getFilterName() {
                return "NuxeoCorsFilterDescriptor";
            }

            @Override
            public ServletContext getServletContext() {
                // Not used with @see CORSFilter
                return null;
            }

            @Override
            public String getInitParameter(String name) {
                return parameters.get(name);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return parameters.keys();
            }
        };
    }

    protected Dictionary<String, String> buildDictionary() {
        Dictionary<String, String> params = new Hashtable<>();
        params.put(PROPERTIES_PREFIX + "allowGenericHttpRequests", Boolean.toString(allowGenericHttpRequests));

        if (!isEmpty(allowOrigin)) {
            params.put(PROPERTIES_PREFIX + "allowOrigin", allowOrigin);
        }

        params.put(PROPERTIES_PREFIX + "allowSubdomains", Boolean.toString(allowSubdomains));

        if (!isEmpty(supportedMethods)) {
            params.put(PROPERTIES_PREFIX + "supportedMethods", supportedMethods);
        }

        if (!isEmpty(supportedHeaders)) {
            params.put(PROPERTIES_PREFIX + "supportedHeaders", supportedHeaders);
        }

        if (!isEmpty(exposedHeaders)) {
            params.put(PROPERTIES_PREFIX + "exposedHeaders", exposedHeaders);
        }

        params.put(PROPERTIES_PREFIX + "supportsCredentials", Boolean.toString(supportsCredentials));
        params.put(PROPERTIES_PREFIX + "maxAge", Integer.toString(maxAge));

        return params;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (NuxeoCorsFilterDescriptor) o;
        var merged = new NuxeoCorsFilterDescriptor();
        merged.name = getIfNull(other.name, name);
        merged.enabled = getIfNull(other.enabled, enabled);
        merged.allowGenericHttpRequests = getIfNull(other.allowGenericHttpRequests, allowGenericHttpRequests);
        merged.allowOrigin = defaultIfEmpty(other.allowOrigin, allowOrigin);
        merged.allowSubdomains = other.allowSubdomains;
        merged.supportedMethods = defaultIfEmpty(other.supportedMethods, supportedMethods);
        merged.supportedHeaders = defaultIfEmpty(other.supportedHeaders, supportedHeaders);
        merged.exposedHeaders = defaultIfEmpty(other.exposedHeaders, exposedHeaders);
        merged.maxAge = maxAge == -1 ? other.maxAge : maxAge;
        merged.pattern = getIfNull(other.pattern, pattern);
        return merged;
    }
}
