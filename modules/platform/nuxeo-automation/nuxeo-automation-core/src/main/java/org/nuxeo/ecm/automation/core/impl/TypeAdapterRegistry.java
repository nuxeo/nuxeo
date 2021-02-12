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
package org.nuxeo.ecm.automation.core.impl;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.AbstractRegistry;
import org.nuxeo.ecm.automation.TypeAdapter;
import org.nuxeo.ecm.automation.core.TypeAdapterContribution;
import org.nuxeo.runtime.RuntimeServiceException;
import org.w3c.dom.Element;

/**
 * Registry for {@link TypeAdapterContribution} contributions.
 *
 * @since 11.5
 */
public class TypeAdapterRegistry extends AbstractRegistry {

    protected AdapterKeyedRegistry adapters;

    @Override
    public void initialize() {
        adapters = new AdapterKeyedRegistry();
        super.initialize();
    }

    @Override
    protected <T> T doRegister(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        TypeAdapterContribution tac = getInstance(ctx, xObject, element);
        TypeAdapter adapter;
        try {
            adapter = tac.clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeServiceException(e);
        }
        adapters.put(new TypeAdapterKey(tac.accept, tac.produce), adapter);
        return null;
    }

    public TypeAdapter getTypeAdapter(Class<?> accept, Class<?> produce) {
        return adapters.get(new TypeAdapterKey(accept, produce));
    }

}
