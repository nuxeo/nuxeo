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
package org.nuxeo.ecm.platform.filemanager.service;

import static org.nuxeo.ecm.platform.filemanager.service.FileManagerService.PLUGINS_EP;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.common.xmap.registry.Registry;
import org.w3c.dom.Element;

/**
 * Custom registry handling multiple "plugin" registries.
 *
 * @since 11.5
 */
public class FileManagerRegistry implements Registry {

    protected Map<String, MapRegistry> registries = new ConcurrentHashMap<>();

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

    protected String computePluginsExtensionPoint(Class<?> klass) {
        return String.format("%s-%s", PLUGINS_EP, klass.getSimpleName());
    }

    @Override
    public void register(Context ctx, XAnnotatedObject xObject, Element element, String tag) {
        String rid = computePluginsExtensionPoint(xObject.getKlass());
        registries.computeIfAbsent(rid, k -> new MapRegistry()).register(ctx, xObject, element, tag);
    }

    @Override
    public void unregister(String tag) {
        registries.values().stream().filter(r -> r.isTagged(tag)).forEach(r -> r.unregister(tag));
    }

    public <T> List<T> getContributionValues(Class<?> klass) {
        String rid = computePluginsExtensionPoint(klass);
        MapRegistry registry = registries.get(rid);
        return registry == null ? List.of() : registry.getContributionValues();
    }

}
