/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.schema;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * The TypeService is the component dealing with registration of schemas and
 * document types (and facets and prefetch configuration).
 * <p>
 * The implementation is delegated to the SchemaManager.
 */
public class TypeService extends DefaultComponent {

    /**
     * @deprecated since 5.7 (unused)
     */
    @Deprecated
    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.core.schema.TypeService");

    private static final Log log = LogFactory.getLog(TypeService.class);

    private static final String XP_SCHEMA = "schema";

    private static final String XP_DOCTYPE = "doctype";

    private static final String XP_CONFIGURATION = "configuration";

    private SchemaManagerImpl schemaManager;

    /**
     * @deprecated since 5.7, use
     *             {@code Framework.getLocalService(SchemaManager.class)}
     *             instead.
     */
    @Deprecated
    public static SchemaManager getSchemaManager() {
        return Framework.getLocalService(SchemaManager.class);
    }

    /**
     * @deprecated since 5.7, use
     *             {@code Framework.getLocalService(SchemaManager.class)}
     *             instead.
     */
    @Deprecated
    public SchemaManager getTypeManager() {
        return schemaManager;
    }

    /**
     * @deprecated since 5.7 (unused)
     */
    @Deprecated
    public XSDLoader getSchemaLoader() {
        return new XSDLoader(schemaManager);
    }

    @Override
    public void activate(ComponentContext context) {
        schemaManager = new SchemaManagerImpl();
    }

    @Override
    public void deactivate(ComponentContext context) {
        schemaManager = null;
    }

    @Override
    public void registerExtension(Extension extension) {
        String xp = extension.getExtensionPoint();
        if (XP_DOCTYPE.equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                if (contrib instanceof DocumentTypeDescriptor) {
                    schemaManager.registerDocumentType((DocumentTypeDescriptor) contrib);
                } else if (contrib instanceof FacetDescriptor) {
                    schemaManager.registerFacet((FacetDescriptor) contrib);
                } else if (contrib instanceof ProxiesDescriptor) {
                    schemaManager.registerProxies((ProxiesDescriptor) contrib);
                }
            }
        } else if (XP_SCHEMA.equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                try {
                    // use the context of the bundle contributing the extension
                    // to load schemas
                    SchemaBindingDescriptor sbd = (SchemaBindingDescriptor) contrib;
                    sbd.context = extension.getContext();
                    schemaManager.registerSchema(sbd);
                } catch (Exception e) {
                    log.error(e, e);
                }
            }
        } else if (XP_CONFIGURATION.equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                schemaManager.registerConfiguration((TypeConfiguration) contrib);
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
        String xp = extension.getExtensionPoint();
        if (XP_DOCTYPE.equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                if (contrib instanceof DocumentTypeDescriptor) {
                    schemaManager.unregisterDocumentType((DocumentTypeDescriptor) contrib);
                } else if (contrib instanceof FacetDescriptor) {
                    schemaManager.unregisterFacet((FacetDescriptor) contrib);
                } else if (contrib instanceof ProxiesDescriptor) {
                    schemaManager.unregisterProxies((ProxiesDescriptor) contrib);
                }
            }
        } else if (XP_SCHEMA.equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                schemaManager.unregisterSchema((SchemaBindingDescriptor) contrib);
            }
        } else if (XP_CONFIGURATION.equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                schemaManager.unregisterConfiguration((TypeConfiguration) contrib);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (SchemaManager.class.isAssignableFrom(adapter)) {
            return (T) schemaManager;
        } else if (TypeProvider.class.isAssignableFrom(adapter)) {
            return (T) schemaManager;
        }
        return null;
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        schemaManager.flushPendingsRegistration();
    }

    @Override
    public int getApplicationStartedOrder() {
        return -100;
    }
}
