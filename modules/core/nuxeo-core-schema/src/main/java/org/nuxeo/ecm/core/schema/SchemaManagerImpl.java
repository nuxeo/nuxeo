/*
 * (C) Copyright 2006-2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
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
import org.nuxeo.common.xmap.registry.SingleRegistry;
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

    protected static final TypeConfiguration DEFAULT_CONFIGURATION = new TypeConfiguration();

    /** Effective prefetch info. */
    protected PrefetchInfo prefetchInfo;

    /** Effective clearComplexPropertyBeforeSet flag. */
    // public for tests
    public boolean clearComplexPropertyBeforeSet;

    /**
     * Effective allowVersionWriteForDublinCore flag.
     */
    protected boolean allowVersionWriteForDublinCore;

    /** Basic type registry. */
    protected Map<String, Type> types = new LinkedHashMap<>();

    /** Effective schemas. */
    protected Map<String, Schema> schemas = new LinkedHashMap<>();

    protected Set<String> disabledSchemas = new HashSet<>();

    protected final Map<String, Schema> prefixToSchema = new HashMap<>();

    /** Effective facets. */
    // public for tests
    public Map<String, CompositeType> facets = new LinkedHashMap<>();

    protected Set<String> noPerDocumentQueryFacets = new HashSet<>();

    protected Set<String> disabledFacets = new HashSet<>();

    /** Effective document types. */
    protected Map<String, DocumentTypeImpl> documentTypes = new LinkedHashMap<>();

    protected Set<String> specialDocumentTypes = new HashSet<>();

    protected Map<String, Set<String>> documentTypesExtending = new HashMap<>();

    protected Map<String, Set<String>> documentTypesForFacet = new HashMap<>();

    /** Effective proxy schemas. */
    protected List<Schema> proxySchemas = new ArrayList<>();

    /** Effective proxy schema names. */
    protected Set<String> proxySchemaNames = new LinkedHashSet<>();

    /** Fields computed lazily. */
    private Map<String, Field> fields = new ConcurrentHashMap<>();

    private File schemaDir;

    public static final String SCHEMAS_DIR_NAME = "schemas";

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
     * Map holding property characteristics with: schema -&gt; path -&gt; characteristic.
     *
     * @since 11.1
     */
    protected Map<String, Map<String, PropertyDescriptor>> propertyCharacteristics = Map.of();

    public SchemaManagerImpl(SingleRegistry configRegistry, SchemaRegistry schemaRegistry,
            DocTypeRegistry doctypeRegistry) {
        schemaDir = new File(Environment.getDefault().getTemp(), SCHEMAS_DIR_NAME);
        if (!schemaDir.mkdirs() && !schemaDir.exists()) {
            throw new RuntimeServiceException("Unable to create schemas directory");
        }
        clearSchemaDir();
        registerBuiltinTypes();

        computeConfiguration((TypeConfiguration) configRegistry.getContribution().orElse(DEFAULT_CONFIGURATION));
        this.disabledSchemas = schemaRegistry.getDisabledSchemas();
        computeSchemas(schemaRegistry.getSchemas());
        this.disabledFacets = doctypeRegistry.getDisabledFacets();
        computeFacets(doctypeRegistry.getFacets()); // depends on schemas
        computeDocumentTypes(doctypeRegistry.getDocumentTypes()); // depends on schemas and facets
        computeProxies(doctypeRegistry.getProxies()); // depends on schemas
        computePropertyCharacteristics(schemaRegistry.getProperties());
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

    protected void computeConfiguration(TypeConfiguration config) {
        log.info("Using global prefetch: {}", config.prefetchInfo);
        if (isNotBlank(config.prefetchInfo)) {
            prefetchInfo = new PrefetchInfo(config.prefetchInfo);
        }
        log.info("Using clearComplexPropertyBeforeSet: {}", config.clearComplexPropertyBeforeSet);
        clearComplexPropertyBeforeSet = config.clearComplexPropertyBeforeSet;
        log.info("Using allowVersionWriteForDublinCore: {}", config.allowVersionWriteForDublinCore);
        allowVersionWriteForDublinCore = config.allowVersionWriteForDublinCore;
    }

    /*
     * ===== Schemas =====
     */

    protected void computeSchemas(Map<String, SchemaBindingDescriptor> resolvedSchemas) {
        schemas.clear();
        prefixToSchema.clear();
        RuntimeException errors = new RuntimeException("Cannot load schemas");

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
        if (sd.src == null) {
            return;
        }
        URL url = sd.src.toURL();
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
        return new ArrayList<>(schemas.values()).toArray(new Schema[0]);
    }

    @Override
    public Schema getSchema(String name) {
        return schemas.get(name);
    }

    @Override
    public Schema getSchemaFromPrefix(String schemaPrefix) {
        return prefixToSchema.get(schemaPrefix);
    }

    /**
     * @deprecated since 11.1, seems unused
     */
    @Override
    @Deprecated(since = "11.1")
    public Schema getSchemaFromURI(String schemaURI) {
        return schemas.values()
                      .stream()
                      .filter(schema -> schema.getNamespace().uri.equals(schemaURI))
                      .findFirst()
                      .orElse(null);
    }

    /*
     * ===== Facets =====
     */

    protected void computeFacets(List<FacetDescriptor> allFacets) {
        facets.clear();
        noPerDocumentQueryFacets.clear();
        for (FacetDescriptor fd : allFacets) {
            Set<String> fdSchemas = SchemaDescriptor.getSchemaNames(fd.schemas);
            registerFacet(fd.name, fdSchemas);
            if (Boolean.FALSE.equals(fd.perDocumentQuery)) {
                noPerDocumentQueryFacets.add(fd.name);
            }
        }
    }

    // also called when a document type references an unknown facet (WARN)
    protected CompositeType registerFacet(String name, Set<String> schemaNames) {
        List<Schema> facetSchemas = new ArrayList<>(schemaNames.size());
        for (String schemaName : schemaNames) {
            Schema schema = schemas.get(schemaName);
            if (schema == null) {
                if (disabledSchemas.contains(schemaName)) {
                    // schema is disabled, don't log as ERROR
                    log.debug("Facet: {} uses disabled schema: {}", name, schemaName);
                    continue;
                }
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
        return new ArrayList<>(facets.values()).toArray(new CompositeType[facets.size()]);
    }

    @Override
    public CompositeType getFacet(String name) {
        return facets.get(name);
    }

    @Override
    public Set<String> getNoPerDocumentQueryFacets() {
        return Collections.unmodifiableSet(noPerDocumentQueryFacets);
    }

    /*
     * ===== Document types =====
     */

    protected void computeDocumentTypes(Map<String, DocumentTypeDescriptor> dtds) {
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

        // special document types (excluded from copy)
        specialDocumentTypes = dtds.values()
                                   .stream()
                                   .filter(d -> Boolean.TRUE.equals(d.special))
                                   .map(d -> d.name)
                                   .collect(Collectors.toSet());
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
                if (disabledFacets.contains(facetName)) {
                    // facet is disabled, don't WARN about it
                    log.debug("Disabled facet: {} used in document type: {}", facetName, name);
                    continue;
                }
                log.warn("Undeclared facet: {} used in document type: {}", facetName, name);
                // register it with no schemas
                ct = registerFacet(facetName, Collections.emptySet());
            }
            schemaNames.addAll(Arrays.asList(ct.getSchemaNames()));
        }
        facetNames.removeAll(disabledFacets);

        // find the schemas
        List<Schema> docTypeSchemas = new ArrayList<>();
        for (String schemaName : schemaNames) {
            Schema schema = schemas.get(schemaName);
            if (schema == null) {
                if (disabledSchemas.contains(schemaName)) {
                    // schema is disabled, don't log as ERROR
                    log.debug("Document type: {} uses disabled schema: {}", name, schemaName);
                    continue;
                }
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
        return documentTypes.get(name);
    }

    @Override
    public Set<String> getDocumentTypeNamesForFacet(String facet) {
        return documentTypesForFacet.get(facet);
    }

    @Override
    public Set<String> getDocumentTypeNamesExtending(String docTypeName) {
        return documentTypesExtending.get(docTypeName);
    }

    @Override
    public DocumentType[] getDocumentTypes() {
        return new ArrayList<DocumentType>(documentTypes.values()).toArray(new DocumentType[0]);
    }

    @Override
    public int getDocumentTypesCount() {
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

    protected void computeProxies(List<ProxiesDescriptor> proxies) {
        for (ProxiesDescriptor pd : proxies) {
            if (!ProxiesDescriptor.DEFAULT_TYPE.equals(pd.getType())) {
                log.error("Proxy descriptor for specific type not supported: {}", pd);
                continue;
            }
            for (String schemaName : pd.getSchemas()) {
                if (proxySchemaNames.contains(schemaName)) {
                    continue;
                }
                Schema schema = schemas.get(schemaName);
                if (schema == null) {
                    log.error("Proxy schema uses unknown schema: {}", schemaName);
                    continue;
                }
                proxySchemas.add(schema);
                proxySchemaNames.add(schemaName);
            }
        }
    }

    @Override
    public List<Schema> getProxySchemas(String docType) {
        // docType unused for now
        return new ArrayList<>(proxySchemas);
    }

    @Override
    public boolean isProxySchema(String schema, String docType) {
        // docType unused for now
        return proxySchemaNames.contains(schema);
    }

    /*
     * ===== Fields =====
     */

    @Override
    public Field getField(String xpath) {
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
    protected synchronized void computePropertyCharacteristics(List<PropertyDescriptor> descriptors) {
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

    @Override
    public Set<String> getSpecialDocumentTypes() {
        return Collections.unmodifiableSet(specialDocumentTypes);
    }
}
