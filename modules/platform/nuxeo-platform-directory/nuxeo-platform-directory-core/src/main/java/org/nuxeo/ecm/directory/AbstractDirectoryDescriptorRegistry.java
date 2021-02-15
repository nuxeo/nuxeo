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
package org.nuxeo.ecm.directory;

import static org.nuxeo.ecm.directory.DirectoryServiceImpl.COMPONENT_NAME;
import static org.nuxeo.ecm.directory.DirectoryServiceImpl.XP;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.common.xmap.registry.Registry;
import org.nuxeo.runtime.api.Framework;
import org.w3c.dom.Element;

/**
 * Abstract registry forwarding contributions to {@link DirectoryRegistry}.
 * <p>
 * Contributions should extend {@link BaseDirectoryDescriptor} and handle clone/merge for all custom fields.
 *
 * @since 11.5
 */
public abstract class AbstractDirectoryDescriptorRegistry extends MapRegistry {

    private static final Logger log = LogManager.getLogger(AbstractDirectoryDescriptorRegistry.class);

    protected static final String DEFAULT_POINT = "directories";

    protected final String componentName;

    protected final String point;

    protected AbstractDirectoryDescriptorRegistry(String componentName) {
        this(componentName, DEFAULT_POINT);
    }

    protected AbstractDirectoryDescriptorRegistry(String componentName, String point) {
        this.componentName = componentName;
        this.point = point;
    }

    protected Registry getTargetRegistry() {
        return Framework.getRuntime()
                        .getComponentManager()
                        .getExtensionPointRegistry(COMPONENT_NAME, XP)
                        .orElseThrow(() -> new IllegalArgumentException(
                                String.format("Unknown registry for extension point '%s--%s'", COMPONENT_NAME, XP)));
    }

    @Override
    public void register(Context ctx, XAnnotatedObject xObject, Element element, String tag) {
        super.register(ctx, xObject, element, tag);
        getTargetRegistry().register(ctx, DirectoryContributor.getXObject(),
                DirectoryContributor.createElement(element, componentName, point), tag);
    }

    @Override
    public void unregister(String tag) {
        super.unregister(tag);
        getTargetRegistry().unregister(tag);
    }

    @Override
    protected String computeId(Context ctx, XAnnotatedObject xObject, Element element) {
        String id = super.computeId(ctx, xObject, element);
        if (id.contains("/") && log.isWarnEnabled()) {
            log.warn("Directory {} should not contain forward slashes in its name, as they are not supported."
                    + " Operations with the REST API on this directory won't work.", id);
        }
        return id;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getMergedInstance(Context ctx, XAnnotatedObject xObject, Element element, Object existing) {
        BaseDirectoryDescriptor contrib = getInstance(ctx, xObject, element);
        if (existing != null) {
            ((BaseDirectoryDescriptor) existing).merge(contrib);
            return (T) existing;
        } else {
            return (T) contrib;
        }
    }

}
