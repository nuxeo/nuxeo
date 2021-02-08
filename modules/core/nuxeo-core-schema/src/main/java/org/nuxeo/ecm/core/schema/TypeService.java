/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.schema;

import org.nuxeo.common.xmap.registry.SingleRegistry;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * The TypeService is the component dealing with registration of schemas and document types (and facets and prefetch
 * configuration).
 * <p>
 * The implementation is delegated to the SchemaManager.
 */
public class TypeService extends DefaultComponent {

    /** @since 11.5 */
    public static final String COMPONENT_NAME = "org.nuxeo.ecm.core.schema.TypeService";

    /** @since 11.5 */
    public static final String XP_DOCTYPE = "doctype";

    protected static final String XP_SCHEMA = "schema";

    private static final String XP_CONFIGURATION = "configuration";

    private SchemaManagerImpl schemaManager;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (SchemaManager.class.isAssignableFrom(adapter)
                || PropertyCharacteristicHandler.class.isAssignableFrom(adapter)
                || TypeProvider.class.isAssignableFrom(adapter)) {
            return (T) schemaManager;
        }
        return null;
    }

    @Override
    public int getApplicationStartedOrder() {
        return -100;
    }

    @Override
    public void start(ComponentContext context) {
        SingleRegistry confReg = getExtensionPointRegistry(XP_CONFIGURATION);
        SchemaRegistry schemaReg = getExtensionPointRegistry(XP_SCHEMA);
        DocTypeRegistry docTypeReg = getExtensionPointRegistry(XP_DOCTYPE);
        schemaManager = new SchemaManagerImpl(confReg, schemaReg, docTypeReg);

    }

    @Override
    public void stop(ComponentContext context) {
        schemaManager = null;
    }

}
