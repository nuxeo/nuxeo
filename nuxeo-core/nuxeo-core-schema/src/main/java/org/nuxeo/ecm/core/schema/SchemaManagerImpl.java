/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.schema.registries.DocumentTypeRegistry;
import org.nuxeo.ecm.core.schema.registries.FacetRegistry;
import org.nuxeo.ecm.core.schema.registries.SchemaRegistry;
import org.nuxeo.ecm.core.schema.registries.SchemaTypeRegistry;
import org.nuxeo.ecm.core.schema.types.AnyType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.CompositeTypeImpl;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.runtime.api.Framework;

/**
 * Schema Manager implementation.
 * <p>
 * Holds basic types (String, Integer, etc.), schemas, document types and
 * facets.
 */
public class SchemaManagerImpl implements SchemaManager {

    private static final Log log = LogFactory.getLog(SchemaManagerImpl.class);

    // types reg

    private final SchemaTypeRegistry typeReg;

    private final DocumentTypeRegistry docTypeReg;

    private final FacetRegistry facetReg;

    private final SchemaRegistry schemaReg;

    private final Map<String, List<DocumentTypeDescriptor>> pendingDocTypes;

    private final Map<String, Field> fields = new HashMap<String, Field>();

    private File schemaDir;

    // global prefetch info
    private PrefetchInfo prefetchInfo;

    public SchemaManagerImpl() throws Exception {
        pendingDocTypes = new HashMap<String, List<DocumentTypeDescriptor>>();
        schemaDir = new File(Framework.getRuntime().getHome(), "schemas");
        typeReg = new SchemaTypeRegistry();
        docTypeReg = new DocumentTypeRegistry();
        facetReg = new FacetRegistry();
        schemaReg = new SchemaRegistry();
        if (!schemaDir.isDirectory()) {
            schemaDir.mkdirs();
        }
        for (Type type : XSDTypes.getTypes()) {
            registerType(type);
        }
        registerBuiltinTypes();
        TypeProvider provider = Framework.getService(TypeProvider.class);
        if (provider != this && provider != null) {
            // should be a remote provider
            importTypes(provider);
        }
    }

    protected void registerBuiltinTypes() {
        registerDocumentType(new DocumentTypeImpl(null, TypeConstants.DOCUMENT,
                null, null, DocumentTypeImpl.T_DOCUMENT));
        registerType(AnyType.INSTANCE);
    }

    /**
     * Initializes initial types using a remote provider if any was specified.
     * <p>
     * Should be called when a provider is registered.
     */
    public synchronized void importTypes(TypeProvider provider) {
        // import remote types
        DocumentType[] docTypes = provider.getDocumentTypes();
        for (DocumentType docType : docTypes) {
            registerDocumentType(docType);
        }
        Schema[] schemas = provider.getSchemas();
        for (Schema schema : schemas) {
            registerSchema(schema);
        }
        Type[] types = provider.getTypes();
        for (Type type : types) {
            registerType(type);
        }
        for (CompositeType facet : provider.getFacets()) {
            registerFacet(facet);
        }
    }

    @Override
    public Type getType(String schema, String name) {
        if (SchemaNames.BUILTIN.equals(schema)) {
            return typeReg.getType(name);
        } else if (SchemaNames.DOCTYPES.equals(schema)) {
            return docTypeReg.getType(name);
        } else if (SchemaNames.SCHEMAS.equals(schema)) {
            return schemaReg.getSchema(name);
        } else if (SchemaNames.FACETS.equals(schema)) {
            return facetReg.getFacet(name);
        } else {
            Schema ownerSchema = schemaReg.getSchema(schema);
            if (ownerSchema != null) {
                return ownerSchema.getType(name);
            }
            return null;
        }
    }

    @Override
    public void registerType(Type type) {
        String schema = type.getSchemaName();
        if (SchemaNames.BUILTIN.equals(schema)) {
            typeReg.addContribution(type);
        } else if (SchemaNames.SCHEMAS.equals(schema)) {
            schemaReg.addContribution((Schema) type);
        } else if (SchemaNames.DOCTYPES.equals(schema)) {
            docTypeReg.addContribution((DocumentType) type);
        } else if (SchemaNames.FACETS.equals(schema)) {
            facetReg.addContribution((CompositeType) type);
        } else {
            Schema ownerSchema = schemaReg.getSchema(schema);
            if (ownerSchema != null) {
                ownerSchema.registerType(type);
            }
        }
    }

    @Override
    public Type unregisterType(String name) {
        Type type = getType(name);
        typeReg.removeContribution(type);
        return type;
    }

    @Override
    public Type getType(String name) {
        return typeReg.getType(name);
    }

    @Override
    public Type[] getTypes() {
        return typeReg.getTypes();
    }

    @Override
    public Type[] getTypes(String schema) {
        Schema ownerSchema = schemaReg.getSchema(schema);
        if (schema != null) {
            return ownerSchema.getTypes();
        }
        return null;
    }

    @Override
    public int getTypesCount() {
        return typeReg.size();
    }

    @Override
    public void registerSchema(Schema schema) {
        synchronized (schemaReg) {
            schemaReg.addContribution(schema);
        }
    }

    @Override
    public Schema unregisterSchema(String name) {
        Schema schema = schemaReg.getSchema(name);
        if (schema == null) {
            return null;
        }
        Namespace ns = schema.getNamespace();
        log.info("Unregister schema: " + name);
        synchronized (schemaReg) {
            schemaReg.removeContribution(schema);
            return schema;
        }
    }

    @Override
    public Schema getSchema(String name) {
        synchronized (schemaReg) {
            return schemaReg.getSchema(name);
        }
    }

    @Override
    public Schema getSchemaFromPrefix(String schemaPrefix) {
        synchronized (schemaReg) {
            return schemaReg.getSchemaFromPrefix(schemaPrefix);
        }
    }

    @Override
    public Schema getSchemaFromURI(String schemaURI) {
        synchronized (schemaReg) {
            return schemaReg.getSchemaFromURI(schemaURI);
        }
    }

    @Override
    public Field getField(String prefixedName) {
        Field field = fields.get(prefixedName);
        if (field == null) {
            QName qname = QName.valueOf(prefixedName);
            String prefix = qname.getPrefix();
            Schema schema = getSchemaFromPrefix(prefix);
            if (schema == null) {
                // try using the name
                schema = getSchema(prefix);
            }
            if (schema != null) {
                field = schema.getField(qname.getLocalName());
                if (field != null) {
                    fields.put(prefixedName, field);
                }
            }
        }
        return field;
    }

    @Override
    public Schema[] getSchemas() {
        synchronized (schemaReg) {
            return schemaReg.getSchemas();
        }
    }

    @Override
    public int getSchemasCount() {
        synchronized (schemaReg) {
            return schemaReg.size();
        }
    }

    public void setPrefetchInfo(PrefetchInfo prefetchInfo) {
        this.prefetchInfo = prefetchInfo;
    }

    public PrefetchInfo getPrefetchInfo() {
        return prefetchInfo;
    }

    // Document Types

    @Override
    public void registerDocumentType(DocumentType docType) {
        log.info("Register document type: " + docType.getName());
        synchronized (docTypeReg) {
            docTypeReg.addContribution(docType);
        }
    }

    public void registerDocumentType(DocumentTypeDescriptor dtd) {
        synchronized (docTypeReg) {
            DocumentType superType = null;
            if (dtd.superTypeName != null) {
                superType = docTypeReg.getType(dtd.superTypeName);
                if (superType == null) {
                    postponeDocTypeRegistration(dtd);
                    return;
                }
            }
            registerDocumentType(superType, dtd);
        }
    }

    private DocumentType registerDocumentType(DocumentType superType,
            DocumentTypeDescriptor dtd) {
        synchronized (docTypeReg) {
            try {
                Set<String> schemaNames = SchemaDescriptor.getSchemaNames(dtd.schemas);
                // add schemas from facets
                for (String facetName : dtd.facets) {
                    CompositeType facet = getFacet(facetName);
                    if (facet != null) {
                        schemaNames.addAll(Arrays.asList(facet.getSchemaNames()));
                    } else {
                        log.warn("Document type " + dtd.name
                                + " uses undeclared facet: " + facetName);
                        // register it with no schemas
                        CompositeType ct = new CompositeTypeImpl(
                                (TypeRef<CompositeType>) null,
                                SchemaNames.FACETS, facetName, null);
                        registerFacet(ct);
                    }
                }
                DocumentType docType = new DocumentTypeImpl(superType,
                        dtd.name, schemaNames.toArray(new String[0]),
                        dtd.facets);
                docType.setChildrenTypes(dtd.childrenTypes);
                // use global prefetch info if not a local one was defined
                docType.setPrefetchInfo(dtd.prefetch != null ? new PrefetchInfo(
                        dtd.prefetch) : prefetchInfo);
                docTypeReg.addContribution(docType);
                log.info("Registered document type: " + dtd.name);
                registerPendingDocTypes(docType);
                return docType;
            } catch (Exception e) {
                log.error("Error registering document type: " + dtd.name, e);
                // TODO: use component dependencies instead?
            }
            return null;
        }
    }

    private void registerPendingDocTypes(DocumentType superType) {
        List<DocumentTypeDescriptor> list = pendingDocTypes.remove(superType.getName());
        if (list == null) {
            return;
        }
        for (DocumentTypeDescriptor dtd : list) {
            registerDocumentType(superType, dtd);
        }
    }

    @Override
    public DocumentType unregisterDocumentType(String name) {
        log.info("Unregister document type: " + name);
        // TODO handle the case when the doctype to unreg is in the reg.
        // pending queue
        synchronized (docTypeReg) {
            DocumentType docType = docTypeReg.getType(name);
            docTypeReg.removeContribution(docType);
            return docType;
        }
    }

    private void postponeDocTypeRegistration(DocumentTypeDescriptor dtd) {
        List<DocumentTypeDescriptor> list = pendingDocTypes.get(dtd.superTypeName);
        if (list == null) {
            list = new ArrayList<DocumentTypeDescriptor>();
            pendingDocTypes.put(dtd.superTypeName, list);
        }
        list.add(dtd);
    }

    @Override
    public DocumentType getDocumentType(String name) {
        synchronized (docTypeReg) {
            return docTypeReg.getType(name);
        }
    }

    @Override
    public DocumentType[] getDocumentTypes() {
        synchronized (docTypeReg) {
            return docTypeReg.getDocumentTypes();
        }
    }

    @Override
    public int getDocumentTypesCount() {
        synchronized (docTypeReg) {
            return docTypeReg.size();
        }
    }

    @Override
    public void registerFacet(CompositeType facet) {
        synchronized (facetReg) {
            facetReg.addContribution(facet);
            log.info("Registered facet: " + facet.getName());
        }
    }

    public void registerFacet(FacetDescriptor fd) {
        Set<String> schemas = SchemaDescriptor.getSchemaNames(fd.schemas);
        CompositeType ct = new CompositeTypeImpl((TypeRef<CompositeType>) null,
                SchemaNames.FACETS, fd.name, schemas.toArray(new String[0]));
        registerFacet(ct);
    }

    @Override
    public CompositeType unregisterFacet(String name) {
        synchronized (facetReg) {
            log.info("Unregistered facet: " + name);
            CompositeType facet = facetReg.getFacet(name);
            facetReg.removeContribution(facet);
            return facet;
        }
    }

    @Override
    public CompositeType getFacet(String name) {
        synchronized (facetReg) {
            return facetReg.getFacet(name);
        }
    }

    @Override
    public CompositeType[] getFacets() {
        synchronized (facetReg) {
            return facetReg.getFacets();
        }
    }

    // Misc

    @Override
    public void clear() {
        synchronized (docTypeReg) {
            docTypeReg.clear();
        }
        synchronized (schemaReg) {
            schemaReg.clear();
        }
        typeReg.clear();
        facetReg.clear();
    }

    public void setSchemaDirectory(File dir) {
        schemaDir = dir;
    }

    public File getSchemaDirectory() {
        return schemaDir;
    }

    public File getSchemaFile(String name) {
        return new File(schemaDir, name + ".xsd");
    }

    public URL resolveSchemaLocation(String location) {
        if (location.startsWith("schema://")) {
            try {
                return new File(schemaDir, location).toURI().toURL();
            } catch (MalformedURLException e) {
                log.error("failed to resolve schema location: " + location, e);
            }
        }
        return null;
    }

    /**
     * Implementation details: there is a cache on each server for this.
     * <p>
     * Assumes that types never change in the lifespan of this server process
     * and that the Core server has finished loading its types.
     */
    @Override
    public Set<String> getDocumentTypeNamesForFacet(String facet) {
        return docTypeReg.getDocumentTypeNamesForFacet(facet);
    }

    /**
     * Implementation details: there is a cache on each server for this.
     * <p>
     * Assumes that types never change in the lifespan of this server process
     * and that the Core server has finished loading its types.
     */
    @Override
    public Set<String> getDocumentTypeNamesExtending(String docTypeName) {
        return docTypeReg.getDocumentTypeNamesExtending(docTypeName);
    }

    @Override
    public String getXmlSchemaDefinition(String name) {
        File file = getSchemaFile(name);
        if (file != null) {
            try {
                return FileUtils.readFile(file);
            } catch (IOException e) {
                log.error(
                        String.format("Could not read xsd file for '%s'", name),
                        e);
                return null;
            }
        }
        return null;
    }

}
