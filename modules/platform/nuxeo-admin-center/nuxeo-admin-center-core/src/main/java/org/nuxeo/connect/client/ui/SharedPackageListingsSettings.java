/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.connect.client.ui;

import static org.nuxeo.ecm.platform.web.common.RequestContext.getActiveContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Provide contextual access to the {@link ListingFilterSetting} for each listing. Use HttpSession to store a map of
 * {@link ListingFilterSetting} This class is used to share state between the WebEngine and the JSF parts
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class SharedPackageListingsSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * @since 8.10
     */
    private static final Map<String, RequestResolver> resolvers;

    protected Map<String, ListingFilterSetting> settings = new HashMap<>();

    public static final String SESSION_KEY = "org.nuxeo.connect.client.ui.PackageListingSettings";

    static {
        resolvers = new ConcurrentHashMap<>();
        addRequestResolver(new RequestResolver() {
            @Override
            public boolean isActive() {
                return getActiveContext() != null;
            }

            @Override
            public HttpServletRequest resolve() {
                return getActiveContext().getRequest();
            }

            @Override
            public String getId() {
                return "webengine";
            }
        });
    }

    public static void addRequestResolver(RequestResolver resolver) {
        resolvers.put(resolver.getId(), resolver);
    }

    public static void removeRequestResolver(String id) {
        resolvers.remove(id);
    }

    public ListingFilterSetting get(String listName) {
        if (!settings.containsKey(listName)) {
            settings.put(listName, new ListingFilterSetting());
        }
        return settings.get(listName);
    }

    public static SharedPackageListingsSettings instance() {
        return resolvers.values()
                        .stream()
                        .filter(RequestResolver::isActive)
                        .findFirst()
                        .map(RequestResolver::resolve)
                        .map(SharedPackageListingsSettings::instance)
                        .orElse(null);
    }

    public static SharedPackageListingsSettings instance(HttpServletRequest request) {
        return instance(request.getSession(true));
    }

    public static SharedPackageListingsSettings instance(HttpSession session) {
        Object val = session.getAttribute(SESSION_KEY);
        if (val == null || !(val instanceof SharedPackageListingsSettings)) {
            val = new SharedPackageListingsSettings();
            session.setAttribute(SESSION_KEY, val);
        }
        return (SharedPackageListingsSettings) val;
    }

    /**
     * @since 8.10
     */
    public interface RequestResolver {
        boolean isActive();

        HttpServletRequest resolve();

        String getId();
    }
}
