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
package org.nuxeo.ecm.platform.forms.layout.core.registries;

import java.util.List;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.w3c.dom.Element;

/**
 * Map registry handling aliases as well as categories.
 *
 * @since 11.5
 */
public abstract class AliasMapRegistry extends MapRegistry {

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getInstance(Context ctx, XAnnotatedObject xObject, Element element) {
        T contrib = super.getInstance(ctx, xObject, element);
        return (T) getStoredContribution(contrib);
    }

    @Override
    protected <T> T doRegister(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        T contrib = super.doRegister(ctx, xObject, element, extensionId);
        if (contrib != null) {
            getAliases(contrib).forEach(alias -> contributions.put(alias, contrib));
        }
        return contrib;
    }

    protected abstract <T> List<String> getAliases(T contribution);

    protected <T> Object getStoredContribution(T contribution) {
        return contribution;
    }

}
