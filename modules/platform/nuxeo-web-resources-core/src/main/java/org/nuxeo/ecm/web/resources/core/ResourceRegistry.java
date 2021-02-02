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
package org.nuxeo.ecm.web.resources.core;

import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.w3c.dom.Element;

/**
 * Custom registry to resolve resource URI.
 *
 * @since 11.5
 */
public class ResourceRegistry extends MapRegistry {

    private static final Logger log = LogManager.getLogger(ResourceRegistry.class);

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T doRegister(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        ResourceDescriptor resource = super.doRegister(ctx, xObject, element, extensionId);
        computeResourceUri(resource, ctx);
        return (T) resource;
    }

    protected void computeResourceUri(ResourceDescriptor resource, Context context) {
        String uri = resource.getURI();
        if (uri == null) {
            // build it from local classpath
            // XXX: hacky wildcard support
            String path = resource.getPath();
            if (path != null) {
                boolean hasWildcard = false;
                if (path.endsWith("*")) {
                    hasWildcard = true;
                    path = path.substring(0, path.length() - 1);
                }
                URL url = context.getResource(path);
                if (url == null) {
                    log.error("Cannot resolve URL for resource '{}' with path '{}'", resource.getName(),
                            resource.getPath());
                } else {
                    String builtUri = url.toString();
                    if (hasWildcard) {
                        builtUri += "*";
                    }
                    resource.setURI(builtUri);
                }
            }
        }
    }

}
