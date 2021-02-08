/*
 * (C) Copyright 2006-2021 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *     Anahide Tchertchian
 */

package org.nuxeo.ecm.platform.types;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.ecm.core.schema.DocumentTypeDescriptor;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.SchemaManagerImpl;
import org.nuxeo.runtime.api.Framework;
import org.w3c.dom.Element;

public class TypeRegistry extends MapRegistry {

    protected Map<String, DocumentTypeDescriptor> dtds = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getMergedInstance(Context ctx, XAnnotatedObject xObject, Element element, Object existing) {
        Type merged = super.getMergedInstance(ctx, xObject, element, existing);
        // delete denied subtypes from allowed subtypes
        Set<String> denied = Set.of(merged.getDeniedSubTypes());
        merged.getAllowedSubTypes().keySet().removeIf(Predicate.not(denied::contains));
        return (T) merged;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T doRegister(Context ctx, XAnnotatedObject xObject, Element element, String extensionId) {
        Type type = super.doRegister(ctx, xObject, element, extensionId);
        if (type == null) {
            removeCoreContribution(computeId(ctx, xObject, element));
        } else {
            updateCoreContribution(type.getId(), type);
        }
        return (T) type;
    }

    public boolean hasType(String id) {
        return getContribution(id).isPresent();
    }

    public Collection<Type> getTypes() {
        return getContributionValues();
    }

    public Type getType(String id) {
        return this.<Type> getContribution(id).orElse(null);
    }

    /**
     * @since 8.10
     */
    protected void recomputeTypes() {
        List<Type> types = getContributionValues();
        for (Type type : types) {
            type.setAllowedSubTypes(getCoreAllowedSubtypes(type));
            // do not need to add denied subtypes because allowed subtypes already come filtered from core
            type.setDeniedSubTypes(new String[0]);
        }
    }

    /**
     * @since 8.10
     */
    protected Map<String, SubType> getCoreAllowedSubtypes(Type type) {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        Collection<String> coreAllowedSubtypes = schemaManager.getAllowedSubTypes(type.getId());
        if (coreAllowedSubtypes == null) {
            // there are no subtypes to take care of
            return Collections.emptyMap();
        }

        Map<String, SubType> res = new HashMap<>();
        Map<String, SubType> subTypes = type.getAllowedSubTypes();
        for (String name : coreAllowedSubtypes) {
            SubType subtype = subTypes.get(name);
            if (subtype == null) {
                res.put(name, new SubType(name, null));
            } else {
                res.put(name, subtype);
            }
        }

        return res;
    }

    /**
     * @since 8.4
     */
    protected void updateCoreContribution(String id, Type contrib) {
        SchemaManagerImpl schemaManager = (SchemaManagerImpl) Framework.getService(SchemaManager.class);

        // if there's already a core contribution, unregister it and register a new one
        if (dtds.containsKey(id)) {
            schemaManager.unregisterDocumentType(dtds.get(id));
            dtds.remove(id);
        }

        DocumentTypeDescriptor dtd = new DocumentTypeDescriptor();
        dtd.name = contrib.getId();
        dtd.subtypes = contrib.getAllowedSubTypes().keySet().toArray(new String[contrib.getAllowedSubTypes().size()]);
        dtd.forbiddenSubtypes = contrib.getDeniedSubTypes();
        dtd.append = true;

        // only make a core contrib if there are changes on subtypes
        if (dtd.subtypes.length > 0 || dtd.forbiddenSubtypes.length > 0) {
            dtds.put(id, dtd);
            schemaManager.registerDocumentType(dtd);
        }
    }

    /**
     * @since 8.4
     */
    protected void removeCoreContribution(String id) {
        if (dtds.containsKey(id)) {
            SchemaManagerImpl schemaManager = (SchemaManagerImpl) Framework.getService(SchemaManager.class);
            schemaManager.unregisterDocumentType(dtds.get(id));
            dtds.remove(id);
        }
    }

}
