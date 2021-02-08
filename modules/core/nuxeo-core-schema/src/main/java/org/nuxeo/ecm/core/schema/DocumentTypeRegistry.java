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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.w3c.dom.Element;

/**
 * Registry for descriptors {@link DocumentTypeDescriptor} to the "doctype" extension point.
 * <p>
 * Handles custom merges.
 *
 * @since 11.5
 */
public class DocumentTypeRegistry extends MapRegistry {

    // additional registry to synchronize allowed/denied subtypes definition from higher-level TypeService
    protected MapRegistry programmaticTypes = new MapRegistry();

    @Override
    public void initialize() {
        super.initialize();
        // merge with programmatic types registrations
        List<DocumentTypeDescriptor> types = programmaticTypes.getContributionValues();
        types.forEach(type -> {
            DocumentTypeDescriptor localType = (DocumentTypeDescriptor) contributions.get(type.name);
            if (localType == null) {
                return;
            }
            localType.subtypes = mergeSubTypes(localType.subtypes, type.subtypes);
            localType.forbiddenSubtypes = mergeSubTypes(localType.forbiddenSubtypes, type.forbiddenSubtypes);
        });
    }

    protected String[] mergeSubTypes(String[] orig, String[] other) {
        return Stream.concat(Arrays.stream(orig), Arrays.stream(other)).distinct().toArray(String[]::new);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getMergedInstance(Context ctx, XAnnotatedObject xObject, Element element, Object existing) {
        // handle prefetch merge
        String existingPrefetch = existing != null ? ((DocumentTypeDescriptor) existing).prefetch : null;
        DocumentTypeDescriptor type = super.getMergedInstance(ctx, xObject, element, existing);
        if (StringUtils.isNotBlank(existingPrefetch)) {
            type.prefetch = String.join(" ", existingPrefetch, type.prefetch);
        }
        return (T) type;
    }

    // custom API

    /**
     * Register method used to synchronize allowed/denied subtypes definition from higher-level TypeService.
     */
    public void registerDocumentType(Context ctx, XAnnotatedObject xObject, Element element, String tag) {
        programmaticTypes.register(ctx, xObject, element, tag);
    }

    /**
     * Unregister method used to synchronize allowed/denied subtypes definition from higher-level TypeService.
     */
    public void unregisterDocumentType(String tag) {
        programmaticTypes.unregister(tag);
    }

}
