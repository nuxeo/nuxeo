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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.auth.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.Registry;
import org.nuxeo.common.xmap.registry.SingleRegistry;
import org.w3c.dom.Element;

/**
 * Registry handling hot-reload and custom merge of {@link LoginScreenConfig} contributions.
 * <p>
 * Also accepts programmatic configurations for provider links.
 * <p>
 * Modified as of 11.5 to implement {@link Registry}.
 *
 * @since 7.10
 */
public class LoginScreenConfigRegistry extends SingleRegistry {

    protected Map<String, LoginProviderLink> programmaticLinks = Collections.synchronizedMap(new LinkedHashMap<>());

    @Override
    public void initialize() {
        super.initialize();
        this.<LoginScreenConfig> getContribution()
            .ifPresent(conf -> programmaticLinks.values().forEach(l -> conf.merge(new LoginScreenConfig(l))));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getMergedInstance(Context ctx, XAnnotatedObject xObject, Element element, Object existing) {
        LoginScreenConfig contrib = getInstance(ctx, xObject, element);
        if (existing != null) {
            ((LoginScreenConfig) existing).merge(contrib);
            return (T) existing;
        } else {
            return (T) contrib;
        }
    }

    /**
     * Adds provider links held by given {@link LoginScreenConfig} to the global login configuration.
     */
    public void addContribution(LoginScreenConfig config) {
        List<LoginProviderLink> links = config.getProviders();
        if (!links.isEmpty()) {
            links.forEach(link -> programmaticLinks.put(link.getName(), link));
            initialize();
        }
    }

    /**
     * Removes provider links held by given {@link LoginScreenConfig} from the global login configuration.
     */
    public void removeContribution(LoginScreenConfig config) {
        List<LoginProviderLink> links = config.getProviders();
        if (!links.isEmpty()) {
            links.stream().map(LoginProviderLink::getName).forEach(programmaticLinks::remove);
            initialize();
        }
    }

}
