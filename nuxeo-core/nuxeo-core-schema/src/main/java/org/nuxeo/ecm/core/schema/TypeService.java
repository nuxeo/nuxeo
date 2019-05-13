/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

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
        super.activate(context);
        schemaManager = new SchemaManagerImpl();
    }

    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
        schemaManager = null;
    }

    @Override
    public void registerExtension(Extension extension) {
        String xp = extension.getExtensionPoint();
        Object[] contribs = extension.getContributions();
        switch (xp) {
        case XP_DOCTYPE:
            for (Object contrib : contribs) {
                if (contrib instanceof DocumentTypeDescriptor) {
                    schemaManager.registerDocumentType((DocumentTypeDescriptor) contrib);
                } else if (contrib instanceof FacetDescriptor) {
                    schemaManager.registerFacet((FacetDescriptor) contrib);
                } else if (contrib instanceof ProxiesDescriptor) {
                    schemaManager.registerProxies((ProxiesDescriptor) contrib);
                }
            }
            break;
        case XP_SCHEMA:
            for (Object contrib : contribs) {
                if (contrib instanceof SchemaBindingDescriptor) {
                    // use the context of the bundle contributing the extension
                    // to load schemas
                    SchemaBindingDescriptor sbd = (SchemaBindingDescriptor) contrib;
                    sbd.context = extension.getContext();
                    schemaManager.registerSchema(sbd);
                } else if (contrib instanceof PropertyDescriptor) {
                    xp = computeSchemaExtensionPoint(contrib.getClass());
                    super.registerContribution(contrib, xp, extension.getComponent());
                }
            }
            break;
        case XP_CONFIGURATION:
            for (Object contrib : contribs) {
                schemaManager.registerConfiguration((TypeConfiguration) contrib);
            }
            break;
        case XP_DEPRECATION:
            for (Object contrib : contribs) {
                schemaManager.registerPropertyDeprecation((PropertyDeprecationDescriptor) contrib);
            }
            break;
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
        String xp = extension.getExtensionPoint();
        Object[] contribs = extension.getContributions();
        switch (xp) {
        case XP_DOCTYPE:
            for (Object contrib : contribs) {
                if (contrib instanceof DocumentTypeDescriptor) {
                    schemaManager.unregisterDocumentType((DocumentTypeDescriptor) contrib);
                } else if (contrib instanceof FacetDescriptor) {
                    schemaManager.unregisterFacet((FacetDescriptor) contrib);
                } else if (contrib instanceof ProxiesDescriptor) {
                    schemaManager.unregisterProxies((ProxiesDescriptor) contrib);
                }
            }
            break;
        case XP_SCHEMA:
            for (Object contrib : contribs) {
                if (contrib instanceof SchemaBindingDescriptor) {
                    schemaManager.unregisterSchema((SchemaBindingDescriptor) contrib);
                } else if (contrib instanceof PropertyDescriptor) {
                    xp = computeSchemaExtensionPoint(contrib.getClass());
                    super.unregisterContribution(contrib, xp, extension.getComponent());
                }
            }
            break;
        case XP_CONFIGURATION:
            for (Object contrib : contribs) {
                schemaManager.unregisterConfiguration((TypeConfiguration) contrib);
            }
            break;
        case XP_DEPRECATION:
            for (Object contrib : contribs) {
                schemaManager.unregisterPropertyDeprecation((PropertyDeprecationDescriptor) contrib);
            }
            break;
        }
    }

    protected String computeSchemaExtensionPoint(Class<?> klass) {
        return String.format("%s-%s", XP_SCHEMA, klass.getSimpleName());
    }

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
    public void start(ComponentContext context) {
        schemaManager.registerSecuredProperty(getDescriptors(computeSchemaExtensionPoint(PropertyDescriptor.class)));
        schemaManager.flushPendingsRegistration();
    }

    @Override
    public void stop(ComponentContext context) {
        schemaManager.clearSecuredProperty();
    }

    @Override
    public int getApplicationStartedOrder() {
        return -100;
    }
}
