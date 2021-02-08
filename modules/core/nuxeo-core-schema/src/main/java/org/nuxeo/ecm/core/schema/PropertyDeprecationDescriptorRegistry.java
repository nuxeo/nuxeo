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

import static org.nuxeo.ecm.core.schema.PropertyDescriptor.DEPRECATED;
import static org.nuxeo.ecm.core.schema.PropertyDescriptor.REMOVED;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.registry.Registry;
import org.nuxeo.runtime.RuntimeMessage;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.RuntimeMessage.Source;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.logging.DeprecationLogger;
import org.w3c.dom.Element;

/**
 * Registry for deprecated {@link PropertyDeprecationDescriptor}. Forwards to the {@link SchemaRegistry}, handling new
 * {@link PropertyDescriptor} contributions.
 *
 * @since 11.5
 */
public class PropertyDeprecationDescriptorRegistry implements Registry {

    protected static XAnnotatedObject xProperty;

    static {
        XMap xmap = new XMap();
        xmap.register(PropertyDescriptor.class);
        xProperty = xmap.getObject(PropertyDescriptor.class);
    }

    protected Registry getTargetRegistry() {
        return Framework.getRuntime()
                        .getComponentManager()
                        .getExtensionPointRegistry(TypeService.COMPONENT_NAME, TypeService.XP_SCHEMA)
                        .orElseThrow(() -> new IllegalArgumentException(
                                String.format("Unknown registry for extension point '%s--%s'",
                                        TypeService.COMPONENT_NAME, TypeService.XP_SCHEMA)));
    }

    @Override
    public void tag(String id) {
        getTargetRegistry().tag(id);
    }

    @Override
    public boolean isTagged(String id) {
        return getTargetRegistry().isTagged(id);
    }

    @Override
    public void register(Context ctx, XAnnotatedObject xObject, Element element, String tag) {
        String message = String.format(
                "Deprecation contribution on extension '%s' should now be contributed to extension point '%s'", tag,
                TypeService.XP_SCHEMA);
        DeprecationLogger.log(message, "11.1");
        Framework.getRuntime()
                 .getMessageHandler()
                 .addMessage(new RuntimeMessage(Level.WARNING, message, Source.EXTENSION, tag));
        String deprecated = element.getAttribute("deprecated");
        element.setAttribute("deprecation", Boolean.parseBoolean(deprecated) ? DEPRECATED : REMOVED);
        getTargetRegistry().register(ctx, xProperty, element, tag);
    }

    @Override
    public void unregister(String tag) {
        getTargetRegistry().unregister(tag);
    }

}
