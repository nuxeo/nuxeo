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
package org.nuxeo.ecm.core.schema;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.w3c.dom.Element;

/**
 * Registry for {@link SchemaBindingDescriptor}.
 *
 * @since 11.5
 */
public class SchemaBindingRegistry extends MapRegistry {

    private static final Logger log = LogManager.getLogger(SchemaBindingRegistry.class);

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T doRegister(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        String id = computeId(ctx, xObject, element);

        if (shouldRemove(ctx, xObject, element, extensionId)) {
            contributions.remove(id);
            return null;
        }

        SchemaBindingDescriptor contrib = (SchemaBindingDescriptor) getInstance(ctx, xObject, element);
        SchemaBindingDescriptor existing = (SchemaBindingDescriptor) contributions.get(id);

        // skip redefined schemas that are not overridden, still handling enablement
        if (existing != null && !contrib.override) {
            log.warn("Schema {} is redefined but will not be overridden", id);
        } else {
            if (existing != null) {
                log.debug("Re-registering schema: {} from {}", id, contrib.src);
            } else {
                log.debug("Registering schema: {} from {}", id, contrib.src);
            }
            contributions.put(id, contrib);
        }

        Boolean enable = shouldEnable(ctx, xObject, element, extensionId);
        if (enable != null) {
            if (Boolean.TRUE.equals(enable)) {
                disabled.remove(id);
            } else {
                disabled.add(id);
            }
        }

        return (T) contrib;
    }

}
