/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.forms.layout.core.registries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.common.xmap.registry.Registry;
import org.w3c.dom.Element;

/**
 * Registry handling contributions by category from registries extending {@link AliasMapRegistry}.
 *
 * @since 11.5
 */
public abstract class AbstractCategoryMapRegistry implements Registry {

    private static final Logger log = LogManager.getLogger(AbstractCategoryMapRegistry.class);

    protected Map<String, MapRegistry> registries = new ConcurrentHashMap<>();

    // specific API

    protected abstract List<String> getCategories(Context ctx, XAnnotatedObject xObject, Element element);

    protected <T> List<String> getContributionAliases(T contribution) {
        return Collections.emptyList();
    }

    protected <T> Object getConvertedContribution(T contribution) {
        return contribution;
    }

    public List<String> getCategories() {
        return new ArrayList<>(registries.keySet());
    }

    public <T> Map<String, T> getContributions(String category) {
        MapRegistry registry = registries.get(category);
        if (registry != null) {
            return registry.getContributions();
        }
        return Collections.emptyMap();
    }

    public <T> List<T> getContributionValues(String category) {
        MapRegistry registry = registries.get(category);
        if (registry != null) {
            return registry.getContributionValues();
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public <T> T getContribution(String category, String id) {
        MapRegistry registry = registries.get(category);
        if (registry == null) {
            return null;
        }
        return (T) registry.getContribution(id).orElse(null);
    }

    // registry API

    @Override
    public void initialize() {
        registries.values().forEach(MapRegistry::initialize);
    }

    @Override
    public void tag(String id) {
        registries.values().forEach(r -> r.tag(id));
    }

    @Override
    public boolean isTagged(String id) {
        return registries.values().stream().anyMatch(r -> r.isTagged(id));
    }

    @Override
    public void register(Context ctx, XAnnotatedObject xObject, Element element, String tag) {
        List<String> categories = getCategories(ctx, xObject, element);
        if (categories.isEmpty()) {
            log.error("Cannot register contribution '{}': no category found", xObject.newInstance(ctx, element));
            return;
        }
        categories.forEach(cat -> {
            registries.computeIfAbsent(cat, k -> new AliasMapRegistry() {
                @Override
                protected <T> List<String> getAliases(T contribution) {
                    return getContributionAliases(contribution);
                }

                @Override
                protected <T> Object getStoredContribution(T contribution) {
                    return getConvertedContribution(contribution);
                }

            }).register(ctx, xObject, element, tag);
        });
    }

    @Override
    public void unregister(String tag) {
        registries.values().stream().filter(r -> r.isTagged(tag)).forEach(r -> r.unregister(tag));
    }

}
