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
package org.nuxeo.runtime.datasource;

import java.util.List;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.common.xmap.registry.Registry;
import org.w3c.dom.Element;

/**
 * Registry for datasources, handling two different descriptors.
 *
 * @since 11.5
 */
public class DataSourceRegistry implements Registry {

    protected MapRegistry registry = new MapRegistry();

    protected MapRegistry linkRegistry = new MapRegistry();

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public void initialize() {
        registry.initialize();
        linkRegistry.initialize();
    }

    @Override
    public void tag(String id) {
        registry.tag(id);
        linkRegistry.tag(id);
    }

    @Override
    public boolean isTagged(String id) {
        return registry.isTagged(id) || linkRegistry.isTagged(id);
    }

    @Override
    public void register(Context ctx, XAnnotatedObject xObject, Element element, String tag) {
        Class<?> klass = xObject.getKlass();
        if (DataSourceDescriptor.class.equals(klass)) {
            registry.register(ctx, xObject, element, tag);
        } else if (DataSourceLinkDescriptor.class.equals(klass)) {
            linkRegistry.register(ctx, xObject, element, tag);
        } else {
            throw new IllegalArgumentException("Unsupported class " + klass);
        }
    }

    @Override
    public void unregister(String tag) {
        registry.unregister(tag);
        linkRegistry.unregister(tag);
    }

    public List<DataSourceDescriptor> getDataSources() {
        return registry.getContributionValues();
    }

    public List<DataSourceLinkDescriptor> getDataSourceLinks() {
        return linkRegistry.getContributionValues();
    }

}
