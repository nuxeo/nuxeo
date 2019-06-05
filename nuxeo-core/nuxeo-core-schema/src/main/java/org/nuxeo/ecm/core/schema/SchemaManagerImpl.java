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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.schema.types.AnyType;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.CompositeTypeImpl;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.TypeException;
import org.nuxeo.runtime.RuntimeServiceException;
import org.xml.sax.SAXException;

/**
 * Schema Manager implementation.
 * <p>
 * Holds basic types (String, Integer, etc.), schemas, document types and facets.
 */
public class SchemaManagerImpl implements SchemaManager {

    private static final Logger log = LogManager.getLogger(SchemaManagerImpl.class);

    /**
     * Whether there have been changes to the registered schemas, facets or document types that require recomputation of
     * the effective ones.
     */
    // volatile to use double-check idiom
    protected volatile boolean dirty = true;

    /** Basic type registry. */
    protected Map<String, Type> types = new HashMap<>();

    /** All the registered configurations (prefetch). */
    protected List<TypeConfiguration> allConfigurations = new ArrayList<>();

    /** All the registered schemas. */
    protected List<SchemaBindingDescriptor> allSchemas = new ArrayList<>();

    /** All the registered facets. */
    protected List<FacetDescriptor> allFacets = new ArrayList<>();

    /** All the registered document types. */
    protected List<DocumentTypeDescriptor> allDocumentTypes = new ArrayList<>();

    /** All the registered proxy descriptors. */
    protected List<ProxiesDescriptor> allProxies = new ArrayList<>();

    /** Effective prefetch info. */
    protected PrefetchInfo prefetchInfo;

    /** Effective clearComplexPropertyBeforeSet flag. */
    protected boolean clearComplexPropertyBeforeSet;

    /**
     * Effective allowVersionWriteForDublinCore flag.
     */
    protected boolean allowVersionWriteForDublinCore;

    /** Effective schemas. */
    protected Map<String, Schema> schemas = new HashMap<>();

    protected final Map<String, Schema> prefixToSchema = new HashMap<>();

    /** Effective facets. */
    protected Map<String, CompositeType> facets = new HashMap<>();

    protected Set<String> noPerDocumentQueryFacets = new HashSet<>();

    /** Effective document types. */
    protected Map<String, DocumentTypeImpl> documentTypes = new HashMap<>();

    protected Map<String, Set<String>> documentTypesExtending = new HashMap<>();

    protected Map<String, Set<String>> documentTypesForFacet = new HashMap<>();

    /** Effective proxy schemas. */
    protected List<Schema> proxySchemas = new ArrayList<>();

    /** Effective proxy schema names. */
    protected Set<String> proxySchemaNames = new HashSet<>();

    /** Fields computed lazily. */
    private Map<String, Field> fields = new ConcurrentHashMap<>();

    private File schemaDir;

    public static final String SCHEMAS_DIR_NAME = "schemas";

    /**
     * Default used for clearComplexPropertyBeforeSet if there is no XML configuration found.
     *
     * @since 9.3
     */
    public static final boolean CLEAR_COMPLEX_PROP_BEFORE_SET_DEFAULT = true;

    protected List<Runnable> recomputeCallbacks;

    /**
     * @since 9.2
     * @deprecated since 11.1, use {@link #propertyCharacteristics} instead
     */
    @Deprecated(since = "11.1")
    protected Map<String, Map<String, String>> deprecatedProperties = new HashMap<>();

    /**
     * @since 9.2
     * @deprecated since 11.1, use {@link #propertyCharacteristics} instead
     */
    @Deprecated(since = "11.1")
    protected Map<String, Map<String, String>> removedProperties = new HashMap<>();

    /**
     * Map holding property characteristics with: schema -> path -> characteristic.
     *
     * @since 11.1
     */
    protected Map<String, Map<String, PropertyDescriptor>> propertyCharacteristics = Map.of();

    public SchemaManagerImpl() {
        recomputeCallbacks = new ArrayList<>();
        schemaDir = new File(Environment.getDefault().getTemp(), SCHEMAS_DIR_NAME);
        if (!schemaDir.mkdirs() && !schemaDir.exists()) {
            throw new RuntimeServiceException("Unable to create schemas directory");
        }
        clearSchemaDir();
        registerBuiltinTypes();
    }

    protected void clearSchemaDir() {
        try {
            org.apache.commons.io.FileUtils.cleanDirectory(schemaDir);
        } catch (IOException e) {
            throw new RuntimeServiceException(e);
        }
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
        if (isNotBlank(config.prefetchInfo)) {
            log.info("Registered global prefetch: {}", config.prefetchInfo);
        }
        if (config.clearComplexPropertyBeforeSet != null) {
            log.info("Registered clearComplexPropertyBeforeSet: {}", config.clearComplexPropertyBeforeSet);
        }
        if (config.allowVersionWriteForDublinCore != null) {
            log.info("Registered allowVersionWriteForDublinCore: {}", config.allowVersionWriteForDublinCore);
        }
    }

    public synchronized void unregisterConfiguration(TypeConfiguration config) {
        if (allConfigurations.remove(config)) {
            dirty = true;
            if (isNotBlank(config.prefetchInfo)) {
                log.info("Unregistered global prefetch: {}", config.prefetchInfo);
            }
            if (config.clearComplexPropertyBeforeSet != null) {
                log.info("Unregistered clearComplexPropertyBeforeSet: {}", config.clearComplexPropertyBeforeSet);
            }
            if (config.allowVersionWriteForDublinCore != null) {
                log.info("Unregistered allowVersionWriteForDublinCore: {}", config.allowVersionWriteForDublinCore);
            }
        } else {
            log.error("Unregistering unknown configuration: {}", config);
        }
    }

    public synchronized void registerSchema(SchemaBindingDescriptor sd) {
        allSchemas.add(sd);
        dirty = true;
        log.info("Registered schema: {}", sd.name);
    }

    public synchronized void unregisterSchema(SchemaBindingDescriptor sd) {
        if (allSchemas.remove(sd)) {
            dirty = true;
            log.info("Unregistered schema: {}", sd.name);
        } else {
            log.error("Unregistering unknown schema: {}", sd.name);
        }
    }

    public synchronized void registerFacet(FacetDescriptor fd) {
        allFacets.removeIf(f -> f.getName().equals(fd.getName()));
        allFacets.add(fd);
        dirty = true;
        log.info("Registered facet: {}", fd.name);
    }

    public synchronized void unregisterFacet(FacetDescriptor fd) {
        if (allFacets.remove(fd)) {
            dirty = true;
            log.info("Unregistered facet: {}", fd.name);
        } else {
            log.error("Unregistering unknown facet: {}", fd.name);
        }
    }

    public synchronized void registerDocumentType(DocumentTypeDescriptor dtd) {
        allDocumentTypes.add(dtd);
        dirty = true;
        log.info("Registered document type: {}", dtd.name);
    }

    public synchronized void unregisterDocumentType(DocumentTypeDescriptor dtd) {
        if (allDocumentTypes.remove(dtd)) {
            dirty = true;
            log.info("Unregistered document type: {}", dtd.name);
        } else {
            log.error("Unregistering unknown document type: {}", dtd.name);
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

    // NXP-14218: used for tests, to be able to unregister it
    public FacetDescriptor getFacetDescriptor(String name) {
        return allFacets.stream().filter(f -> f.getName().equals(name)).reduce((a, b) -> b).orElse(null);
    }

    // NXP-14218: used for tests, to recompute available facets
    public void recomputeDynamicFacets() {
        recomputeFacets();
        dirty = false;
    }

    public synchronized void registerProxies(ProxiesDescriptor pd) {
        allProxies.add(pd);
        dirty = true;
        log.info("Registered proxies descriptor for schemas: {}", pd::getSchemas);
    }

    public synchronized void unregisterProxies(ProxiesDescriptor pd) {
        if (allProxies.remove(pd)) {
            dirty = true;
            log.info("Unregistered proxies descriptor for schemas: {}", pd::getSchemas);
        } else {
            log.error("Unregistering unknown proxies descriptor for schemas: {}", pd::getSchemas);
        }
    }

    /**
     * Checks if something has to be recomputed if a dynamic register/unregister happened.
     */
    // public for tests
    public void checkDirty() {
        // variant of double-check idiom
        if (!dirty) {
            return;
        }
        synchronized (this) {
            if (!dirty) {
                return;
            }
            // call recompute() synchronized
            recompute();
            dirty = false;
            executeRecomputeCallbacks();
        }
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
        prefetchInfo = null;
        clearComplexPropertyBeforeSet = CLEAR_COMPLEX_PROP_BEFORE_SET_DEFAULT;
        allowVersionWriteForDublinCore = false; // default in the absence of any XML config
        for (TypeConfiguration tc : allConfigurations) {
            if (isNotBlank(tc.prefetchInfo)) {
                prefetchInfo = new PrefetchInfo(tc.prefetchInfo);
            }
            if (tc.clearComplexPropertyBeforeSet != null) {
                clearComplexPropertyBeforeSet = tc.clearComplexPropertyBeforeSet.booleanValue();
            }
            if (tc.allowVersionWriteForDublinCore != null) {
                allowVersionWriteForDublinCore = tc.allowVersionWriteForDublinCore.booleanValue();
            }
        }
    }

    /*
     * ===== Schemas =====
     */

    protected void recomputeSchemas() {
        schemas.clear();
        prefixToSchema.clear();
        RuntimeException errors = new RuntimeException("Cannot load schemas");
        // on reload, don't take confuse already-copied schemas with those contributed
        clearSchemaDir();
        // resolve which schemas to actually load depending on overrides
        Map<String, SchemaBindingDescriptor> resolvedSchemas = new LinkedHashMap<>();
        for (SchemaBindingDescriptor sd : allSchemas) {
            String name = sd.name;
            if (resolvedSchemas.containsKey(name)) {
                if (!sd.override) {
                    log.warn("Schema {} is redefined but will not be overridden", name);
                    continue;
                }
                log.debug("Re-registering schema: {} from {}", name, sd.file);
            } else {
                log.debug("Registering schema: {} from {}", name, sd.file);
            }
            resolvedSchemas.put(name, sd);
        }
        for (SchemaBindingDescriptor sd : resolvedSchemas.values()) {
            try {
                copySchema(sd);
            } catch (IOException error) {
                errors.addSuppressed(error);
            }
        }
        for (SchemaBindingDescriptor sd : resolvedSchemas.values()) {
            try {
                loadSchema(sd);
            } catch (IOException | SAXException | TypeException error) {
                errors.addSuppressed(error);
            }
        }
        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
    }

    protected void copySchema(SchemaBindingDescriptor sd) throws IOException {
        if (sd.src == null || sd.src.length() == 0) {
            // INLINE Schemas ARE NOT YET IMPLEMENTED!
            return;
        }
        URL url = sd.context.getLocalResource(sd.src);
        if (url == null) {
            // try asking the class loader
            url = sd.context.getResource(sd.src);
        }
        if (url == null) {
            log.error("XSD Schema not found: {}", sd.src);
            return;
        }
        try (InputStream in = url.openStream()) {
            sd.file = new File(schemaDir, sd.name + ".xsd");
            FileUtils.copyInputStreamToFile(in, sd.file); // may overwrite
        }
    }

    protected void loadSchema(SchemaBindingDescriptor sd) throws IOException, SAXException, TypeException {
        if (sd.file == null) {
            // INLINE Schemas ARE NOT YET IMPLEMENTED!
            return;
        }
        // loadSchema calls this.registerSchema
        XSDLoader schemaLoader = new XSDLoader(this, sd);
        schemaLoader.loadSchema(sd.name, sd.prefix, sd.file, sd.xsdRootElement, sd.isVersionWritable);
        log.info("Registered schema: {} from {}", sd.name, sd.file);
    }

    // called from XSDLoader
    protected void registerSchema(Schema schema) {
        schemas.put(schema.getName(), schema);
        Namespace ns = schema.getNamespace();
        if (!StringUtils.isBlank(ns.prefix)) {
            prefixToSchema.put(ns.prefix, schema);
        }
    }

    @Override
    public Schema[] getSchemas() {
        checkDirty();
        return new ArrayList<>(schemas.values()).toArray(new Schema[0]);
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

    /**
     * @deprecated since 11.1, seems unused
     */
    @Override
    @Deprecated(since = "11.1")
    public Schema getSchemaFromURI(String schemaURI) {
        checkDirty();
        return schemas.values()
                      .stream()
                      .filter(schema -> schema.getNamespace().uri.equals(schemaURI))
                      .findFirst()
                      .orElse(null);
    }

    /*
     * ===== Facets =====
     */

    protected void recomputeFacets() {
        facets.clear();
        noPerDocumentQueryFacets.clear();
        for (FacetDescriptor fd : allFacets) {
            recomputeFacet(fd);
        }
    }

    protected void recomputeFacet(FacetDescriptor fd) {
        Set<String> fdSchemas = SchemaDescriptor.getSchemaNames(fd.schemas);
        registerFacet(fd.name, fdSchemas);
        if (Boolean.FALSE.equals(fd.perDocumentQuery)) {
            noPerDocumentQueryFacets.add(fd.name);
        }
    }

    // also called when a document type references an unknown facet (WARN)
    protected CompositeType registerFacet(String name, Set<String> schemaNames) {
        List<Schema> facetSchemas = new ArrayList<>(schemaNames.size());
        for (String schemaName : schemaNames) {
            Schema schema = schemas.get(schemaName);
            if (schema == null) {
                log.error("Facet: {} uses unknown schema: {}", name, schemaName);
                continue;
            }
            facetSchemas.add(schema);
        }
        CompositeType ct = new CompositeTypeImpl(null, SchemaNames.FACETS, name, facetSchemas);
        facets.put(name, ct);
        return ct;
    }

    @Override
    public CompositeType[] getFacets() {
        checkDirty();
        return new ArrayList<>(facets.values()).toArray(new CompositeType[facets.size()]);
    }

    @Override
    public CompositeType getFacet(String name) {
        checkDirty();
        return facets.get(name);
    }

    @Override
    public Set<String> getNoPerDocumentQueryFacets() {
        checkDirty();
        return Collections.unmodifiableSet(noPerDocumentQueryFacets);
    }

    /*
     * ===== Document types =====
     */

    protected void recomputeDocumentTypes() {
        // effective descriptors with override
        // linked hash map to keep order for reproducibility
        Map<String, DocumentTypeDescriptor> dtds = new LinkedHashMap<>();
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
            LinkedHashSet<String> stack = new LinkedHashSet<>();
            recomputeDocumentType(name, stack, dtds);
        }

        // document types having a given facet
        documentTypesForFacet.clear();
        for (DocumentType docType : documentTypes.values()) {
            for (String facet : docType.getFacets()) {
                documentTypesForFacet.computeIfAbsent(facet, k -> new HashSet<>()).add(docType.getName());
            }
        }

    }

    protected DocumentTypeDescriptor mergeDocumentTypeDescriptors(DocumentTypeDescriptor src,
            DocumentTypeDescriptor dst) {
        return dst.clone().merge(src);
    }

    protected DocumentType recomputeDocumentType(String name, Set<String> stack,
            Map<String, DocumentTypeDescriptor> dtds) {
        DocumentTypeImpl docType = documentTypes.get(name);
        if (docType != null) {
            // already done
            return docType;
        }
        if (stack.contains(name)) {
            log.error("Document type: {} used in parent inheritance loop: {}", name, stack);
            return null;
        }
        DocumentTypeDescriptor dtd = dtds.get(name);
        if (dtd == null) {
            log.error("Document type: {} does not exist, used as parent by type: {}", name, stack);
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

    protected DocumentType recomputeDocumentType(String name, DocumentTypeDescriptor dtd, DocumentType parent) {
        // find the facets and schemas names
        Set<String> facetNames = new HashSet<>(Arrays.asList(dtd.facets));
        Set<String> schemaNames = SchemaDescriptor.getSchemaNames(dtd.schemas);
        Set<String> subtypes = new HashSet<>(Arrays.asList(dtd.subtypes));
        Set<String> forbidden = new HashSet<>(Arrays.asList(dtd.forbiddenSubtypes));

        // inherited
        if (parent != null) {
            facetNames.addAll(parent.getFacets());
            schemaNames.addAll(Arrays.asList(parent.getSchemaNames()));
        }

        // add schemas names from facets
        for (String facetName : facetNames) {
            CompositeType ct = facets.get(facetName);
            if (ct == null) {
                log.warn("Undeclared facet: {} used in document type: {}", facetName, name);
                // register it with no schemas
                ct = registerFacet(facetName, Collections.emptySet());
            }
            schemaNames.addAll(Arrays.asList(ct.getSchemaNames()));
        }

        // find the schemas
        List<Schema> docTypeSchemas = new ArrayList<>();
        for (String schemaName : schemaNames) {
            Schema schema = schemas.get(schemaName);
            if (schema == null) {
                log.error("Document type: {} uses unknown schema: {}", name, schemaName);
                continue;
            }
            docTypeSchemas.add(schema);
        }

        // create doctype
        PrefetchInfo prefetch = dtd.prefetch == null ? prefetchInfo : new PrefetchInfo(dtd.prefetch);
        DocumentTypeImpl docType = new DocumentTypeImpl(name, parent, docTypeSchemas, facetNames, prefetch);
        docType.setSubtypes(subtypes);
        docType.setForbiddenSubtypes(forbidden);
        registerDocumentType(docType);

        return docType;
    }

    protected void registerDocumentType(DocumentTypeImpl docType) {
        String name = docType.getName();
        documentTypes.put(name, docType);
        documentTypesExtending.put(name, new HashSet<>(Collections.singleton(name)));
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
        Set<String> subTypes = getDocumentTypeNamesExtending(superType);
        return subTypes != null && subTypes.contains(docType);
    }

    @Override
    public Set<String> getAllowedSubTypes(String typeName) {
        DocumentType dt = getDocumentType(typeName);
        return dt == null ? null : dt.getAllowedSubtypes();
    }

    /*
     * ===== Proxies =====
     */

    protected void recomputeProxies() {
        List<Schema> list = new ArrayList<>();
        Set<String> nameSet = new HashSet<>();
        for (ProxiesDescriptor pd : allProxies) {
            if (!pd.getType().equals("*")) {
                log.error("Proxy descriptor for specific type not supported: {}", pd);
            }
            for (String schemaName : pd.getSchemas()) {
                if (nameSet.contains(schemaName)) {
                    continue;
                }
                Schema schema = schemas.get(schemaName);
                if (schema == null) {
                    log.error("Proxy schema uses unknown schema: {}", schemaName);
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
        return new ArrayList<>(proxySchemas);
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
    public Field getField(String xpath) {
        checkDirty();
        Field field = null;
        if (xpath != null && xpath.contains("/")) {
            // need to resolve subfields
            String[] properties = xpath.split("/");
            Field resolvedField = getField(properties[0]);
            for (int x = 1; x < properties.length; x++) {
                if (resolvedField == null) {
                    break;
                }
                resolvedField = getField(resolvedField, properties[x], x == properties.length - 1);
            }
            if (resolvedField != null) {
                field = resolvedField;
            }
        } else {
            field = fields.get(xpath);
            if (field == null) {
                QName qname = QName.valueOf(xpath);
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
                        fields.put(xpath, field);
                    }
                }
            }
        }
        return field;
    }

    @Override
    public Field getField(Field parent, String subFieldName) {
        return getField(parent, subFieldName, true);
    }

    protected Field getField(Field parent, String subFieldName, boolean finalCall) {
        if (parent != null) {
            Type type = parent.getType();
            if (type.isListType()) {
                ListType listType = (ListType) type;
                // remove indexes in case of multiple values
                if ("*".equals(subFieldName)) {
                    if (!finalCall) {
                        return parent;
                    } else {
                        return resolveSubField(listType, null, true);
                    }
                }
                try {
                    Integer.valueOf(subFieldName);
                    if (!finalCall) {
                        return parent;
                    } else {
                        return resolveSubField(listType, null, true);
                    }
                } catch (NumberFormatException e) {
                    return resolveSubField(listType, subFieldName, false);
                }
            } else if (type.isComplexType()) {
                return ((ComplexType) type).getField(subFieldName);
            }
        }
        return null;
    }

    protected Field resolveSubField(ListType listType, String subName, boolean fallbackOnSubElement) {
        Type itemType = listType.getFieldType();
        if (itemType.isComplexType() && subName != null) {
            ComplexType complexType = (ComplexType) itemType;
            return complexType.getField(subName);
        }
        if (fallbackOnSubElement) {
            return listType.getField();
        }
        return null;
    }

    public void flushPendingsRegistration() {
        checkDirty();
    }

    /*
     * ===== Recompute Callbacks =====
     */

    /**
     * @since 8.10
     */
    public void registerRecomputeCallback(Runnable callback) {
        recomputeCallbacks.add(callback);
    }

    /**
     * @since 8.10
     */
    public void unregisterRecomputeCallback(Runnable callback) {
        recomputeCallbacks.remove(callback);
    }

    /**
     * @since 8.10
     */
    protected void executeRecomputeCallbacks() {
        recomputeCallbacks.forEach(Runnable::run);
    }

    /*
     * ===== Deprecation API =====
     */

    /**
     * @since 9.2
     * @deprecated since 11.1, use {@link PropertyCharacteristicHandler} methods instead
     */
    @Override
    @Deprecated(since = "11.1")
    public PropertyDeprecationHandler getDeprecatedProperties() {
        return new PropertyDeprecationHandler(deprecatedProperties);
    }

    /**
     * @since 9.2
     * @deprecated since 11.1, use {@link PropertyCharacteristicHandler} methods instead
     */
    @Override
    @Deprecated(since = "11.1")
    public PropertyDeprecationHandler getRemovedProperties() {
        return new PropertyDeprecationHandler(removedProperties);
    }

    @Override
    public boolean getClearComplexPropertyBeforeSet() {
        return clearComplexPropertyBeforeSet;
    }

    @Override
    public boolean getAllowVersionWriteForDublinCore() {
        return allowVersionWriteForDublinCore;
    }

    /*
     * ===== Property API =====
     */

    /**
     * @since 11.1
     */
    protected synchronized void registerPropertyCharacteristics(List<PropertyDescriptor> descriptors) {
        propertyCharacteristics = descriptors.stream()
                                             .collect(groupingBy(PropertyDescriptor::getSchema,
                                                     toMap(PropertyDescriptor::getName, Function.identity())));
        deprecatedProperties = descriptors.stream()
                                          .filter(PropertyDescriptor::isDeprecated)
                                          .collect(groupingBy(PropertyDescriptor::getSchema, Collector.of(HashMap::new,
                                                  (m, d) -> m.put(d.name, d.fallback), (m1, m2) -> {
                                                      m1.putAll(m2);
                                                      return m1;
                                                  })));
        removedProperties = descriptors.stream()
                                       .filter(PropertyDescriptor::isRemoved)
                                       .collect(groupingBy(PropertyDescriptor::getSchema, Collector.of(HashMap::new,
                                               (m, d) -> m.put(d.name, d.fallback), (m1, m2) -> {
                                                   m1.putAll(m2);
                                                   return m1;
                                               })));
    }

    /**
     * @since 11.1
     */
    protected synchronized void clearPropertyCharacteristics() {
        propertyCharacteristics.clear();
        deprecatedProperties.clear();
        removedProperties.clear();
    }

    /**
     * @since 11.1
     */
    @Override
    public boolean isSecured(String schema, String path) {
        return checkPropertyCharacteristic(schema, path, PropertyDescriptor::isSecured);
    }

    @Override
    public boolean isDeprecated(String schema, String path) {
        return checkPropertyCharacteristic(schema, path, PropertyDescriptor::isDeprecated);
    }

    @Override
    public boolean isRemoved(String schema, String path) {
        return checkPropertyCharacteristic(schema, path, PropertyDescriptor::isRemoved);
    }

    @Override
    public Set<String> getDeprecatedProperties(String schema) {
        return getPropertyCharacteristics(schema, PropertyDescriptor::isDeprecated, PropertyDescriptor::getName);
    }

    @Override
    public Set<String> getRemovedProperties(String schema) {
        return getPropertyCharacteristics(schema, PropertyDescriptor::isRemoved, PropertyDescriptor::getName);
    }

    protected <R> Set<R> getPropertyCharacteristics(String schema, Predicate<PropertyDescriptor> predicate,
            Function<PropertyDescriptor, R> function) {
        return propertyCharacteristics.getOrDefault(schema, Map.of())
                                      .values()
                                      .stream()
                                      .filter(predicate)
                                      .map(function)
                                      .collect(Collectors.toSet());
    }

    @Override
    public Optional<String> getFallback(String schema, String path) {
        return Optional.ofNullable(propertyCharacteristics.get(schema))
                       .map(props -> props.get(cleanPath(path)))
                       .map(PropertyDescriptor::getFallback);
    }

    protected boolean checkPropertyCharacteristic(String schema, String path, Predicate<PropertyDescriptor> predicate) {
        Map<String, PropertyDescriptor> properties = propertyCharacteristics.getOrDefault(schema, Map.of());
        // iterate on path to check if a parent matches the given predicate
        return !properties.isEmpty()
                && Stream.iterate(cleanPath(path), StringUtils::isNotBlank,
                        key -> key.substring(0, Math.max(key.lastIndexOf('/'), 0)))
                         .anyMatch(p -> properties.containsKey(p) && predicate.test(properties.get(p)));
    }

    protected String cleanPath(String path) {
        // remove prefix if exist, then
        // remove index from path - we're only interested in sth/index/sth because we can't add info on sth/* property
        return path.substring(path.lastIndexOf(':') + 1).replaceAll("/-?\\d+/", "/*/");
    }
}
