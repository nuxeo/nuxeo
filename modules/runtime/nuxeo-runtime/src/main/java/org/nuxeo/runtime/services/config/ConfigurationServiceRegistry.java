/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.services.config;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.logging.DeprecationLogger;
import org.w3c.dom.Element;

/**
 * Custom registry to accomodate for {@link ConfigurationPropertyDescriptor} merge logic.
 *
 * @since 11.5
 */
public class ConfigurationServiceRegistry extends MapRegistry {

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T doRegister(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        String id = computeId(ctx, xObject, element);
        if (shouldRemove(ctx, xObject, element, extensionId)) {
            contributions.remove(id);
            return null;
        }
        ConfigurationPropertyDescriptor contrib = getInstance(ctx, xObject, element);
        if (Framework.getProperties().containsKey(id)) {
            String message = String.format(
                    "Property '%s', contributed by '%s', should now be contributed to extension "
                            + "point 'org.nuxeo.runtime.ConfigurationService', using target 'configuration'",
                    id, extensionId);
            DeprecationLogger.log(message, "7.4");
        }
        ConfigurationPropertyDescriptor existing = (ConfigurationPropertyDescriptor) contributions.get(id);
        if (existing != null) {
            ConfigurationPropertyDescriptor merged = existing.merge(contrib);
            contributions.put(id, merged);
        } else {
            contributions.put(id, contrib);
        }
        return (T) contrib;
    }

}
