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
package org.nuxeo.runtime.model.registry;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.Registry;
import org.nuxeo.runtime.RuntimeMessage;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.RuntimeMessage.Source;
import org.nuxeo.runtime.api.Framework;
import org.w3c.dom.Element;

/**
 * @since 11.5
 */
public abstract class AbstractRuntimeRegistry<T extends Registry> implements Registry {

    protected final String component;

    protected final String point;

    protected final T registry;

    protected AbstractRuntimeRegistry(String component, String point, T registry) {
        this.component = component;
        this.point = point;
        this.registry = registry;
    }

    public String getComponent() {
        return component;
    }

    public String getPoint() {
        return point;
    }

    // helper API

    protected void addRuntimeMessage(Level level, String message, String extensionId) {
        Framework.getRuntime()
                 .getMessageHandler()
                 .addMessage(new RuntimeMessage(level, message, Source.EXTENSION, extensionId));
    }

    // decorator wrappings

    @Override
    public void initialize() {
        registry.initialize();
    }

    @Override
    public void tag(String id) {
        registry.tag(id);
    }

    @Override
    public boolean isTagged(String id) {
        return registry.isTagged(id);
    }

    @Override
    public void register(Context ctx, XAnnotatedObject xObject, Element element, String tag) {
        register(ctx, xObject, element, tag);
    }

    @Override
    public void unregister(String tag) {
        unregister(tag);
    }

}
