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
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.schema.types.AnyType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.CompositeTypeImpl;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.TypeException;
import org.nuxeo.runtime.api.Framework;
import org.xml.sax.SAXException;

/**
 * Schema Manager implementation.
 * <p>
 * Holds basic types (String, Integer, etc.), schemas, document types and
 * facets.
 */
public class SchemaManagerImpl implements SchemaManager {

    private static final Log log = LogFactory.getLog(SchemaManagerImpl.class);

    /**
     * Whether there have been changes to the registered schemas, facets or
     * document types that require recomputation of the effective ones.
     */
    protected boolean dirty = true;

    /** Basic type registry. */
    protected Map<String, Type> types = new HashMap<String, Type>();

    /** All the registered configurations (prefetch). */
    protected List<TypeConfiguration> allConfigurations = new ArrayList<TypeConfiguration>();

    /** All the registered schemas. */
    protected List<SchemaBindingDescriptor> allSchemas = new ArrayList<SchemaBindingDescriptor>();

    /** All the registered facets. */
    protected List<FacetDescriptor> allFacets = new ArrayList<FacetDescriptor>();

    /** All the registered document types. */
    protected List<DocumentTypeDescriptor> allDocumentTypes = new ArrayList<DocumentTypeDescriptor>();

    /** All the registered proxy descriptors. */
    protected List<ProxiesDescriptor> allProxies = new ArrayList<ProxiesDescriptor>();

    /** Effective prefetch info. */
    protected PrefetchInfo prefetchInfo;

    /** Effective schemas. */
    protected Map<String, Schema> schemas = new HashMap<String, Schema>();

    protected final Map<String, Schema> uriToSchema = new HashMap<String, Schema>();

    protected final Map<String, Schema> prefixToSchema = new HashMap<String, Schema>();

    /** Effective facets. */
    protected Map<String, CompositeType> facets = new HashMap<String, CompositeType>();

    /** Effective document types. */
    protected Map<String, DocumentTypeImpl> documentTypes = new HashMap<String, DocumentTypeImpl>();

    protected Map<String, Set<String>> documentTypesExtending = new HashMap<String, Set<String>>();

    protected Map<String, Set<String>> documentTypesForFacet = new HashMap<String, Set<String>>();

    /** Effective proxy schemas. */
    protected List<Schema> proxySchemas = new ArrayList<Schema>();

    /** Effective proxy schema names. */
    protected Set<String> proxySchemaNames = new HashSet<String>();

    /** Fields computed lazily. */
    private Map<String, Field> fields = new ConcurrentHashMap<String, Field>();

    private File schemaDir;

    public static final String SCHEMAS_DIR_NAME = "schemas";

    public SchemaManagerImpl() {
        schemaDir = new File(Framework.getRuntime().getHome(), SCHEMAS_DIR_NAME);
        if (!schemaDir.isDirectory()) {
            schemaDir.mkdirs();
        }
        registerBuiltinTypes();
    }

    public File getSchemasDir() {
        return schemaDir;
    }

    protected void registerBuiltinTypes() {
        for (Type type : XSDTypes.getTypes()) {
            registerType(type);
        }
        registerType(AnyType.INSTANCE);
    }

    protected void registerType(Type type) {
        types.put(type.getName(), type);
    }

    // called by XSDLoader
    protected Type getType(String name) {
        return types.get(name);
    }

    // for tests
    protected Collection<Type> getTypes() {
        return types.values();
    }

    public synchronized void registerConfiguration(TypeConfiguration config) {
        allConfigurations.add(config);
        dirty = true;
        log.info("Registered global prefetch: " + config.prefetchInfo);
    }

    public synchronized void unregisterConfiguration(TypeConfiguration config) {
        if (allConfigurations.remove(config)) {
            dirty = true;
            log.info("Unregistered global prefetch: " + config.prefetchInfo);
        } else {
            log.error("Unregistering unknown prefetch: " + config.prefetchInfo);

        }
    }

    public synchronized void registerSchema(SchemaBindingDescriptor sd) {
        allSchemas.add(sd);
        dirty = true;
        log.info("Registered schema: " + sd.name); // TODO from foo.xsd
    }

    public synchronized void unregisterSchema(SchemaBindingDescriptor sd) {
        if (allSchemas.remove(sd)) {
            dirty = true;
            log.info("Unregistered schema: " + sd.name);
        } else {
            log.error("Unregistering unknown schema: " + sd.name);
        }
    }

    public synchronized void registerFacet(FacetDescriptor fd) {
        allFacets.add(fd);
        dirty = true;
        log.info("Registered facet: " + fd.name);
    }

    public synchronized void unregisterFacet(FacetDescriptor fd) {
        if (allFacets.remove(fd)) {
            dirty = true;
            log.info("Unregistered facet: " + fd.name);
        } else {
            log.error("Unregistering unknown facet: " + fd.name);
        }
    }

    public synchronized void registerDocumentType(DocumentTypeDescriptor dtd) {
        allDocumentTypes.add(dtd);
        dirty = true;
        log.info("Registered document type: " + dtd.name);
    }

    public synchronized void unregisterDocumentType(DocumentTypeDescriptor dtd) {
        if (allDocumentTypes.remove(dtd)) {
            dirty = true;
            log.info("Unregistered document type: " + dtd.name);
        } else {
            log.error("Unregistering unknown document type: " + dtd.name);
        }
    }

    // for tests
    public DocumentTypeDescriptor getDocumentTypeDescriptor(String name) {
        DocumentTypeDescriptor last = null;
        for (DocumentTypeDescriptor dtd : allDocumentTypes) {
            if (dtd.name.equals(name)) {
                last = dtd;
            }
        }
        return last;
    }

    public synchronized void registerProxies(ProxiesDescriptor pd) {
        allProxies.add(pd);
        dirty = true;
        log.info("Registered proxies descriptor for schemas: " + pd.getSchemas());
    }

    public synchronized void unregisterProxies(ProxiesDescriptor pd) {
        if (allProxies.remove(pd)) {
            dirty = true;
            log.info("Unregistered proxies descriptor for schemas: "
                    + pd.getSchemas());
        } else {
            log.error("Unregistering unknown proxies descriptor for schemas: "
                    + pd.getSchemas());
        }
    }

    /**
     * Checks if something has to be recomputed if a dynamic register/unregister
     * happened.
     */
    protected synchronized void checkDirty() {
        if (!dirty) {
            return;
        }
        recompute();
        dirty = false;
    }

    /**
     * Recomputes effective registries for schemas, facets and document types.
     */
    protected void recompute() {
        recomputeConfiguration();
        recomputeSchemas();
        recomputeFacets(); // depend on schemas
        recomputeDocumentTypes(); // depend on schemas and facets
        recomputeProxies(); // depend on schemas
        fields.clear(); // re-filled lazily
    }

    /*
     * ===== Configuration =====
     */

    protected void recomputeConfiguration() {
        if (allConfigurations.isEmpty()) {
            prefetchInfo = null;
        } else {
            TypeConfiguration last = allConfigurations.get(allConfigurations.size() - 1);
            prefetchInfo = new PrefetchInfo(last.prefetchInfo);
        }
    }

    /*
     * ===== Schemas =====
     */

    protected void recomputeSchemas() {
        schemas.clear();
        uriToSchema.clear();
        prefixToSchema.clear();
        for (SchemaBindingDescriptor sd : allSchemas) {
            try {
                recomputeSchema(sd);
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    // restore interrupted status
                    Thread.currentThread().interrupt();
                }
                log.error(e);
            }
        }
    }

    protected void recomputeSchema(SchemaBindingDescriptor sd)
            throws IOException, SAXException, TypeException {
        if (sd.src == null || sd.src.length() == 0) {
            // log.error("INLINE Schemas ARE NOT YET IMPLEMENTED!");
            return;
        }
        URL url = sd.context.getLocalResource(sd.src);
        if (url == null) {
            // try asking the class loader
            url = sd.context.getResource(sd.src);
        }
        if (url == null) {
            log.error("XSD Schema not found: " + sd.src);
            return;
        }
        InputStream in = url.openStream();
        try {
            File file = new File(schemaDir, sd.name + ".xsd");
            FileUtils.copyToFile(in, file); // may overwrite
            Schema oldschema = schemas.get(sd.name);
            // loadSchema calls this.registerSchema
            XSDLoader schemaLoader = new XSDLoader(this, sd);
            schemaLoader.loadSchema(sd.name, sd.prefix, file, sd.override,
                    sd.xsdRootElement);

            if (oldschema == null) {
                log.info("Registered schema: " + sd.name + " from "
                        + url.toString());
            } else {
                log.info("Reregistered schema: " + sd.name);
            }
        } finally {
            in.close();
        }
    }

    // called from XSDLoader, does not do the checkDirty call
    protected Schema getSchemaInternal(String name) {
        return schemas.get(name);
    }

    // called from XSDLoader
    protected void registerSchema(Schema schema) {
        schemas.put(schema.getName(), schema);
        Namespace ns = schema.getNamespace();
        uriToSchema.put(ns.uri, schema);
        prefixToSchema.put(ns.prefix, schema);
    }

    @Override
    public Schema[] getSchemas() {
        checkDirty();
        return new ArrayList<Schema>(schemas.values()).toArray(new Schema[0]);
    }

    @Override
    public Schema getSchema(String name) {
        checkDirty();
        return schemas.get(name);
    }

    @Override
    public Schema getSchemaFromPrefix(String schemaPrefix) {
        checkDirty();
        return prefixToSchema.get(schemaPrefix);
    }

    @Override
    public Schema getSchemaFromURI(String schemaURI) {
        checkDirty();
        return uriToSchema.get(schemaURI);
    }

    /*
     * ===== Facets =====
     */

    protected void recomputeFacets() {
        facets.clear();
        for (FacetDescriptor fd : allFacets) {
            recomputeFacet(fd);
        }
    }

    protected void recomputeFacet(FacetDescriptor fd) {
        Set<String> schemas = SchemaDescriptor.getSchemaNames(fd.schemas);
        registerFacet(fd.name, schemas);
    }

    // also called when a document type references an unknown facet (WARN)
    protected CompositeType registerFacet(String name, Set<String> schemaNames) {
        List<Schema> facetSchemas = new ArrayList<Schema>(schemaNames.size());
        for (String schemaName : schemaNames) {
            Schema schema = schemas.get(schemaName);
            if (schema == null) {
                log.error("Facet: " + name + " uses unknown schema: "
                        + schemaName);
                continue;
            }
            facetSchemas.add(schema);
        }
        CompositeType ct = new CompositeTypeImpl(null, SchemaNames.FACETS,
                name, facetSchemas);
        facets.put(name, ct);
        return ct;
    }

    @Override
    public CompositeType[] getFacets() {
        checkDirty();
        return new ArrayList<CompositeType>(facets.values()).toArray(new CompositeType[facets.size()]);
    }

    @Override
    public CompositeType getFacet(String name) {
        checkDirty();
        return facets.get(name);
    }

    /*
     * ===== Document types =====
     */

    protected void recomputeDocumentTypes() {
        // effective descriptors with override
        // linked hash map to keep order for reproducibility
        Map<String, DocumentTypeDescriptor> dtds = new LinkedHashMap<String, DocumentTypeDescriptor>();
        for (DocumentTypeDescriptor dtd : allDocumentTypes) {
            String name = dtd.name;
            DocumentTypeDescriptor newDtd = dtd;
            if (dtd.append && dtds.containsKey(dtd.name)) {
                newDtd = mergeDocumentTypeDescriptors(dtd, dtds.get(name));
            }
            dtds.put(name, newDtd);
        }
        // recompute all types, parents first
        documentTypes.clear();
        documentTypesExtending.clear();
        registerDocumentType(new DocumentTypeImpl(TypeConstants.DOCUMENT)); // Document
        for (String name : dtds.keySet()) {
            LinkedHashSet<String> stack = new LinkedHashSet<String>();
            recomputeDocumentType(name, stack, dtds);
        }

        // document types having a given facet
        documentTypesForFacet.clear();
        for (DocumentType docType : documentTypes.values()) {
            for (String facet : docType.getFacets()) {
                Set<String> set = documentTypesForFacet.get(facet);
                if (set == null) {
                    documentTypesForFacet.put(facet,
                            set = new HashSet<String>());
                }
                set.add(docType.getName());
            }
        }

    }

    protected DocumentTypeDescriptor mergeDocumentTypeDescriptors(
            DocumentTypeDescriptor src, DocumentTypeDescriptor dst) {
        return dst.clone().merge(src);
    }

    protected DocumentType recomputeDocumentType(String name,
            Set<String> stack, Map<String, DocumentTypeDescriptor> dtds) {
        DocumentTypeImpl docType = documentTypes.get(name);
        if (docType != null) {
            // already done
            return docType;
        }
        if (stack.contains(name)) {
            log.error("Document type: " + name
                    + " used in parent inheritance loop: " + stack);
            return null;
        }
        DocumentTypeDescriptor dtd = dtds.get(name);
        if (dtd == null) {
            log.error("Document type: " + name
                    + " does not exist, used as parent by type: " + stack);
            return null;
        }

        // find and recompute the parent first
        DocumentType parent;
        String parentName = dtd.superTypeName;
        if (parentName == null) {
            parent = null;
        } else {
            parent = documentTypes.get(parentName);
            if (parent == null) {
                stack.add(name);
                parent = recomputeDocumentType(parentName, stack, dtds);
                stack.remove(name);
            }
        }

        // what it extends
        for (Type p = parent; p != null; p = p.getSuperType()) {
            Set<String> set = documentTypesExtending.get(p.getName());
            set.add(name);
        }

        return recomputeDocumentType(name, dtd, parent);
    }

    protected DocumentType recomputeDocumentType(String name,
            DocumentTypeDescriptor dtd, DocumentType parent) {
        // find the facets and schemas names
        Set<String> facetNames = new HashSet<String>();
        Set<String> schemaNames = SchemaDescriptor.getSchemaNames(dtd.schemas);
        facetNames.addAll(Arrays.asList(dtd.facets));

        // inherited
        if (parent != null) {
            facetNames.addAll(parent.getFacets());
            schemaNames.addAll(Arrays.asList(parent.getSchemaNames()));
        }

        // add schemas names from facets
        for (String facetName : facetNames) {
            CompositeType ct = facets.get(facetName);
            if (ct == null) {
                log.warn("Undeclared facet: " + facetName
                        + " used in document type: " + name);
                // register it with no schemas
                ct = registerFacet(facetName, Collections.<String> emptySet());
            }
            schemaNames.addAll(Arrays.asList(ct.getSchemaNames()));
        }

        // find the schemas
        List<Schema> docTypeSchemas = new ArrayList<Schema>();
        for (String schemaName : schemaNames) {
            Schema schema = schemas.get(schemaName);
            if (schema == null) {
                log.error("Document type: " + name + " uses unknown schema: "
                        + schemaName);
                continue;
            }
            docTypeSchemas.add(schema);
        }

        // create doctype
        PrefetchInfo prefetch = dtd.prefetch == null ? prefetchInfo
                : new PrefetchInfo(dtd.prefetch);
        DocumentTypeImpl docType = new DocumentTypeImpl(name, parent,
                docTypeSchemas, facetNames, prefetch);
        registerDocumentType(docType);

        return docType;
    }

    protected void registerDocumentType(DocumentTypeImpl docType) {
        String name = docType.getName();
        documentTypes.put(name, docType);
        documentTypesExtending.put(name,
                new HashSet<String>(Collections.singleton(name)));
    }

    @Override
    public DocumentType getDocumentType(String name) {
        checkDirty();
        return documentTypes.get(name);
    }

    @Override
    public Set<String> getDocumentTypeNamesForFacet(String facet) {
        checkDirty();
        return documentTypesForFacet.get(facet);
    }

    @Override
    public Set<String> getDocumentTypeNamesExtending(String docTypeName) {
        checkDirty();
        return documentTypesExtending.get(docTypeName);
    }

    @Override
    public DocumentType[] getDocumentTypes() {
        checkDirty();
        return new ArrayList<DocumentType>(documentTypes.values()).toArray(new DocumentType[0]);
    }

    @Override
    public int getDocumentTypesCount() {
        checkDirty();
        return documentTypes.size();
    }

    @Override
    public boolean hasSuperType(String docType, String superType) {
        if (docType == null || superType == null) {
            return false;
        }
        Set<String> types = getDocumentTypeNamesExtending(superType);
        return types != null && types.contains(docType);
    }

    /*
     * ===== Proxies =====
     */

    protected void recomputeProxies() {
        List<Schema> list = new ArrayList<Schema>();
        Set<String> nameSet = new HashSet<String>();
        for (ProxiesDescriptor pd : allProxies) {
            if (!pd.getType().equals("*")) {
                log.error("Proxy descriptor for specific type not supported: "
                        + pd);
            }
            for (String schemaName : pd.getSchemas()) {
                if (nameSet.contains(schemaName)) {
                    continue;
                }
                Schema schema = schemas.get(schemaName);
                if (schema == null) {
                    log.error("Proxy schema uses unknown schema: " + schemaName);
                    continue;
                }
                list.add(schema);
                nameSet.add(schemaName);
            }
        }
        proxySchemas = list;
        proxySchemaNames = nameSet;
    }

    @Override
    public List<Schema> getProxySchemas(String docType) {
        // docType unused for now
        checkDirty();
        return new ArrayList<Schema>(proxySchemas);
    }

    @Override
    public boolean isProxySchema(String schema, String docType) {
        // docType unused for now
        checkDirty();
        return proxySchemaNames.contains(schema);
    }

    /*
     * ===== Fields =====
     */

    @Override
    public Field getField(String prefixedName) {
        checkDirty();
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
                    // map is concurrent so parallelism is ok
                    fields.put(prefixedName, field);
                }
            }
        }
        return field;
    }

    public void flushPendingsRegistration() {
        checkDirty();
    }

}
