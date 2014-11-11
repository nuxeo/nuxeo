/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.schema;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.TypeException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.ServiceGroup;
import org.nuxeo.runtime.api.ServiceManager;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.RuntimeContext;
import org.xml.sax.SAXException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TypeService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.core.schema.TypeService");

    private static final Log log = LogFactory.getLog(TypeService.class);

    private SchemaManagerImpl typeManager;

    private XSDLoader schemaLoader;

    private ComponentContext context;

    private TypeConfiguration configuration;

    private static SchemaManager schemaManagerInstance;

    //TODO: use a static Services class in runtime to use as only entry point to service lookups
    // and register singleton services there
    // or use a ServiceRef<T> for each singleton service we need to get quickly
    public static SchemaManager getSchemaManager() {
        return schemaManagerInstance;
    }

    public SchemaManager getTypeManager() {
        return typeManager;
    }

    public XSDLoader getSchemaLoader() {
        return schemaLoader;
    }

    @Override
    public void activate(ComponentContext context) {
        this.context = context;
        try {
            typeManager = new SchemaManagerImpl();
            schemaLoader = new XSDLoader(typeManager);
            schemaManagerInstance = typeManager;
        } catch (Exception e) {
            log.error(e, e);
        }
    }

    @Override
    public void deactivate(ComponentContext context) {
        typeManager.clear();
        typeManager = null;
        schemaManagerInstance = null;
    }

    @Override
    public void registerExtension(Extension extension) {
        String xp = extension.getExtensionPoint();
        if ("doctype".equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                if (contrib instanceof DocumentTypeDescriptor) {
                    typeManager.registerDocumentType((DocumentTypeDescriptor) contrib);
                } else if (contrib instanceof FacetDescriptor) {
                    typeManager.registerFacet((FacetDescriptor) contrib);
                }
            }
        } else if ("schema".equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                try {
                    // use the context of the bundle contributing the extension
                    // to load schemas
                    SchemaBindingDescriptor sbd = (SchemaBindingDescriptor) contrib;
                    sbd.context = extension.getContext();
                    registerSchema(sbd);
                } catch (Exception e) {
                    log.error(e, e);
                }
            }
        } else if ("configuration".equals(xp)) {
            Object[] contribs = extension.getContributions();
            if (contribs.length > 0) {
                setConfiguration((TypeConfiguration) contribs[0]);
            }
        } else if ("helper".equals(xp)) {
            Object[] contribs = extension.getContributions();
            if (contribs.length > 0) {
                TypeHelperDescriptor thd = (TypeHelperDescriptor) contribs[0];
                try {
                    typeManager.registerHelper(thd.schema, thd.type, thd.helperClass.newInstance());
                } catch (Exception e) {
                    log.error("Failed to instantiate type helper: "
                            + thd.helperClass, e);
                }
            }
        } else if ("provider".equals(xp)) {
            Object[] contribs = extension.getContributions();
            if (contribs.length > 0) {
                TypeProviderDescriptor tpd = (TypeProviderDescriptor) contribs[0];
                ServiceManager sm = Framework.getLocalService(
                        ServiceManager.class);
                // the JNDI lookup should be done in the ear class loader context
                // otherwise we can have class loading problems (e.g. TypeProvider not found)
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(TypeService.class.getClassLoader());
                    TypeProvider provider = null;
                    if (tpd.uri != null) {
                        provider = (TypeProvider) sm.getService(tpd.uri);
                    } else if (tpd.group != null) {
                        ServiceGroup group = sm.getGroup(tpd.group);
                        if (group != null) {
                            provider = group.getService(TypeProvider.class);
                        } else {
                            log.warn("Invalid type provider extension contribued by: "
                                    + extension.getComponent().getName()
                                    + ". no such service group: " + tpd.group);
                        }
                    } else {
                        log.warn("Invalid type provider extension contribued by: "
                                + extension.getComponent().getName());
                    }
                    if (provider != null) {
                        if (provider != typeManager) {
                            log.info("Importing types from external provider");
                            typeManager.importTypes(provider);
                        }
                    } else {
                        log.warn("Could not instatiate or locate the type provider contributed by: "
                                + extension.getComponent().getName());
                    }
                } catch (Exception e) {
                    log.error("Failed to register type provider", e);
                } finally {
                    Thread.currentThread().setContextClassLoader(cl); //restore initial class loader
                }
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
        String xp = extension.getExtensionPoint();
        if ("doctype".equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                if (contrib instanceof DocumentTypeDescriptor) {
                    typeManager.unregisterDocumentType(((DocumentTypeDescriptor) contrib).name);
                } else if (contrib instanceof FacetDescriptor) {
                    typeManager.unregisterFacet(((FacetDescriptor) contrib).name);
                }
            }
        } else if ("schema".equals(xp)) {
            Object[] contribs = extension.getContributions();
            for (Object contrib : contribs) {
                typeManager.unregisterSchema(((SchemaBindingDescriptor) contrib).name);
            }
        } else if ("helper".equals(xp)) {
            Object[] contribs = extension.getContributions();
            if (contribs.length > 0) {
                TypeHelperDescriptor thd = (TypeHelperDescriptor) contribs[0];
                typeManager.unregisterHelper(thd.schema, thd.type);
            }
        } else if ("provider".equals(xp)) {
            // ignore provider removal
        }
    }

    protected void registerSchema(SchemaBindingDescriptor sd)
            throws IOException, TypeException, SAXException {
        if (sd.src != null && sd.src.length() > 0) {
            RuntimeContext schemaContext = sd.context == null ? context.getRuntimeContext()
                    : sd.context;
            URL url = schemaContext.getLocalResource(sd.src);
            if (url == null) { // try asking the class loader
                url = schemaContext.getResource(sd.src);
            }
            if (url != null) {
                InputStream in = url.openStream();
                try {
                    File file = new File(typeManager.getSchemaDirectory(),
                            sd.name + ".xsd");
                    FileUtils.copyToFile(in, file); // may overwrite
                    Schema oldschema = typeManager.getSchema(sd.name);
                    // loadSchema also (re)registers it with the typeManager
                    schemaLoader.loadSchema(sd.name, sd.prefix, file, sd.override);
                    if (oldschema == null) {
                        log.info("Registered schema: " + sd.name + " from " + url.toString());
                    } else {
                        log.info("Reregistered schema: " + sd.name);
                    }
                } finally {
                    in.close();
                }
            } else {
                log.error("XSD Schema not found: " + sd.src);
            }
        } else {
            log.error("INLINE Schemas ARE NOT YET IMPLEMENTED!");
        }
    }

    public void setConfiguration(TypeConfiguration configuration) {
        this.configuration = configuration;
        if (typeManager != null) {
            typeManager.setPrefetchInfo(new PrefetchInfo(configuration.prefetchInfo));
        }
    }

    public TypeConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (SchemaManager.class.isAssignableFrom(adapter)) {
            return (T) typeManager;
        } else if (TypeProvider.class.isAssignableFrom(adapter)) {
            return (T) typeManager;
        }
        return null;
    }

}
