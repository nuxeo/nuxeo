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

import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * The TypeService is the component dealing with registration of schemas and document types (and facets and prefetch
 * configuration).
 * <p>
 * The implementation is delegated to the SchemaManager.
 */
public class TypeService extends DefaultComponent {

    private static final String XP_SCHEMA = "schema";

    private static final String XP_DOCTYPE = "doctype";

    private static final String XP_CONFIGURATION = "configuration";

    private static final String XP_DEPRECATION = "deprecation";

    private SchemaManagerImpl schemaManager;

    @Override
    public void activate(ComponentContext context) {
        schemaManager = new SchemaManagerImpl();
    }

    @Override
    public void deactivate(ComponentContext context) {
        schemaManager = null;
    }

    @Override
    public void registerContribution(Object contribution, String xp, ComponentInstance component) {
        switch (xp) {
        case XP_DOCTYPE:
            if (contribution instanceof DocumentTypeDescriptor) {
                schemaManager.registerDocumentType((DocumentTypeDescriptor) contribution);
            } else if (contribution instanceof FacetDescriptor) {
                schemaManager.registerFacet((FacetDescriptor) contribution);
            } else if (contribution instanceof ProxiesDescriptor) {
                schemaManager.registerProxies((ProxiesDescriptor) contribution);
            }
            break;
        case XP_SCHEMA:
            // use the context of the bundle contributing the extension to load schemas
            SchemaBindingDescriptor sbd = (SchemaBindingDescriptor) contribution;
            sbd.context = component.getContext();
            schemaManager.registerSchema(sbd);
            break;
        case XP_CONFIGURATION:
            schemaManager.registerConfiguration((TypeConfiguration) contribution);
            break;
        case XP_DEPRECATION:
            schemaManager.registerPropertyDeprecation((PropertyDeprecationDescriptor) contribution);
            break;
        default:
            throw new RuntimeServiceException("Unknown extension point: " + xp);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String xp, ComponentInstance component) {
        switch (xp) {
        case XP_DOCTYPE:
            if (contribution instanceof DocumentTypeDescriptor) {
                schemaManager.unregisterDocumentType((DocumentTypeDescriptor) contribution);
            } else if (contribution instanceof FacetDescriptor) {
                schemaManager.unregisterFacet((FacetDescriptor) contribution);
            } else if (contribution instanceof ProxiesDescriptor) {
                schemaManager.unregisterProxies((ProxiesDescriptor) contribution);
            }
            break;
        case XP_SCHEMA:
            schemaManager.unregisterSchema((SchemaBindingDescriptor) contribution);
            break;
        case XP_CONFIGURATION:
            schemaManager.unregisterConfiguration((TypeConfiguration) contribution);
            break;
        case XP_DEPRECATION:
            schemaManager.unregisterPropertyDeprecation((PropertyDeprecationDescriptor) contribution);
            break;
        default:
            throw new RuntimeServiceException("Unknown extension point: " + xp);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (SchemaManager.class.isAssignableFrom(adapter) || TypeProvider.class.isAssignableFrom(adapter)) {
            return (T) schemaManager;
        }
        return null;
    }

    @Override
    public void start(ComponentContext context) {
        schemaManager.flushPendingsRegistration();
    }

    @Override
    public int getApplicationStartedOrder() {
        return -100;
    }
}
