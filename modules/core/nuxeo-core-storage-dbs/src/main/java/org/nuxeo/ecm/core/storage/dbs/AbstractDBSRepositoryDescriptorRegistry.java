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
package org.nuxeo.ecm.core.storage.dbs;

import static org.nuxeo.ecm.core.storage.dbs.DBSRepositoryRegistry.COMPONENT_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSRepositoryRegistry.POINT_NAME;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.common.xmap.registry.Registry;
import org.nuxeo.runtime.api.Framework;
import org.w3c.dom.Element;

/**
 * Abstract registry forwarding contributions to {@link DBSRepositoryRegistry}.
 * <p>
 * Also handles custom merge: contributions should extend {@link DBSRepositoryDescriptor} and implement merge for any
 * custom fields.
 *
 * @since 11.5
 */
public abstract class AbstractDBSRepositoryDescriptorRegistry extends MapRegistry {

    protected final String componentName;

    protected final String point;

    protected final Class<? extends DBSRepositoryFactory> factoryClass;

    protected AbstractDBSRepositoryDescriptorRegistry(String componentName, String point,
            Class<? extends DBSRepositoryFactory> factoryClass) {
        this.componentName = componentName;
        this.point = point;
        this.factoryClass = factoryClass;
    }

    protected Registry getTargetRegistry() {
        return Framework.getRuntime()
                        .getComponentManager()
                        .getExtensionPointRegistry(COMPONENT_NAME, POINT_NAME)
                        .orElseThrow(() -> new IllegalArgumentException(String.format(
                                "Unknown registry for extension point '%s--%s'", COMPONENT_NAME, POINT_NAME)));
    }

    @Override
    public void register(Context ctx, XAnnotatedObject xObject, Element element, String tag) {
        super.register(ctx, xObject, element, tag);
        getTargetRegistry().register(ctx, DBSRepositoryContributor.getXObject(),
                DBSRepositoryContributor.createElement(element, componentName, point, factoryClass), tag);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getMergedInstance(Context ctx, XAnnotatedObject xObject, Element element, Object existing) {
        DBSRepositoryDescriptor contrib = getInstance(ctx, xObject, element);
        if (existing != null) {
            ((DBSRepositoryDescriptor) existing).merge(contrib);
            return (T) existing;
        } else {
            return (T) contrib;
        }
    }

    @Override
    public void unregister(String tag) {
        super.unregister(tag);
        getTargetRegistry().unregister(tag);
    }

}
