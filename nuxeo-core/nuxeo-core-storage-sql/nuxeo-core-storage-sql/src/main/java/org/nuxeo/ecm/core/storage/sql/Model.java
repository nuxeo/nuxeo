/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.PrefetchInfo;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.FieldDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.FulltextIndexDescriptor;
import org.nuxeo.ecm.core.storage.sql.RowMapper.IdWithTypes;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo;

/**
 * The {@link Model} is the link between high-level types and SQL-level objects
 * (entity tables, collections). It defines all policies relating to the choice
 * of structure (what schema are grouped together in for optimization) and names
 * in the SQL database (table names, column names), and to what entity names
 * (type name, field name) they correspond.
 * <p>
 * A Nuxeo schema or type is mapped to a SQL-level table. Several types can be
 * aggregated in the same table. In theory, a type could even be split into
 * different tables.
 *
 * @author Florent Guillaume
 */
public class Model {

    private static final Log log = LogFactory.getLog(Model.class);

    // change to have deterministic pseudo-UUID generation for debugging
    private static final boolean DEBUG_UUIDS = false;

    public static final String ROOT_TYPE = "Root";

    public static final String REPOINFO_TABLE_NAME = "repositories";

    public static final String REPOINFO_REPONAME_KEY = "name";

    public static final String MAIN_KEY = "id";

    public static final String CLUSTER_NODES_TABLE_NAME = "cluster_nodes";

    public static final String CLUSTER_NODES_NODEID_KEY = "nodeid";

    public static final String CLUSTER_NODES_CREATED_KEY = "created";

    public static final String CLUSTER_INVALS_TABLE_NAME = "cluster_invals";

    public static final String CLUSTER_INVALS_NODEID_KEY = "nodeid";

    public static final String CLUSTER_INVALS_ID_KEY = "id";

    public static final String CLUSTER_INVALS_FRAGMENTS_KEY = "fragments";

    public static final String CLUSTER_INVALS_KIND_KEY = "kind";

    public static final String MAIN_PRIMARY_TYPE_PROP = "ecm:primaryType";

    public static final String MAIN_PRIMARY_TYPE_KEY = "primarytype";

    public static final String MAIN_MIXIN_TYPES_PROP = "ecm:mixinTypes";

    public static final String MAIN_MIXIN_TYPES_KEY = "mixintypes";

    public static final String MAIN_BASE_VERSION_PROP = "ecm:baseVersion";

    public static final String MAIN_BASE_VERSION_KEY = "baseversionid";

    public static final String MAIN_CHECKED_IN_PROP = "ecm:isCheckedIn";

    public static final String MAIN_CHECKED_IN_KEY = "ischeckedin";

    public static final String MAIN_MAJOR_VERSION_PROP = "ecm:majorVersion";

    public static final String MAIN_MAJOR_VERSION_KEY = "majorversion";

    public static final String MAIN_MINOR_VERSION_PROP = "ecm:minorVersion";

    public static final String MAIN_MINOR_VERSION_KEY = "minorversion";

    public static final String MAIN_IS_VERSION_PROP = "ecm:isVersion";

    public static final String MAIN_IS_VERSION_KEY = "isversion";

    public static final String UID_SCHEMA_NAME = "uid";

    public static final String UID_MAJOR_VERSION_KEY = "major_version";

    public static final String UID_MINOR_VERSION_KEY = "minor_version";

    public static final String HIER_TABLE_NAME = "hierarchy";

    public static final String HIER_PARENT_KEY = "parentid";

    public static final String HIER_CHILD_NAME_KEY = "name";

    public static final String HIER_CHILD_POS_KEY = "pos";

    public static final String HIER_CHILD_ISPROPERTY_KEY = "isproperty";

    public static final String ANCESTORS_TABLE_NAME = "ancestors";

    public static final String ANCESTORS_ANCESTOR_KEY = "ancestors";

    public static final String COLL_TABLE_POS_KEY = "pos";

    public static final String COLL_TABLE_VALUE_KEY = "item";

    public static final String MISC_TABLE_NAME = "misc";

    public static final String MISC_LIFECYCLE_POLICY_PROP = "ecm:lifeCyclePolicy";

    public static final String MISC_LIFECYCLE_POLICY_KEY = "lifecyclepolicy";

    public static final String MISC_LIFECYCLE_STATE_PROP = "ecm:lifeCycleState";

    public static final String MISC_LIFECYCLE_STATE_KEY = "lifecyclestate";

    public static final String ACL_TABLE_NAME = "acls";

    public static final String ACL_PROP = "ecm:acl";

    public static final String ACL_POS_KEY = "pos";

    public static final String ACL_NAME_KEY = "name";

    public static final String ACL_GRANT_KEY = "grant";

    public static final String ACL_PERMISSION_KEY = "permission";

    public static final String ACL_USER_KEY = "user";

    public static final String ACL_GROUP_KEY = "group";

    public static final String VERSION_TABLE_NAME = "versions";

    public static final String VERSION_VERSIONABLE_PROP = "ecm:versionableId";

    public static final String VERSION_VERSIONABLE_KEY = "versionableid";

    public static final String VERSION_CREATED_PROP = "ecm:versionCreated";

    public static final String VERSION_CREATED_KEY = "created";

    public static final String VERSION_LABEL_PROP = "ecm:versionLabel";

    public static final String VERSION_LABEL_KEY = "label";

    public static final String VERSION_DESCRIPTION_PROP = "ecm:versionDescription";

    public static final String VERSION_DESCRIPTION_KEY = "description";

    public static final String VERSION_IS_LATEST_PROP = "ecm:isLatestVersion";

    public static final String VERSION_IS_LATEST_KEY = "islatest";

    public static final String VERSION_IS_LATEST_MAJOR_PROP = "ecm:isLatestMajorVersion";

    public static final String VERSION_IS_LATEST_MAJOR_KEY = "islatestmajor";

    public static final String PROXY_TYPE = "ecm:proxy";

    public static final String PROXY_TABLE_NAME = "proxies";

    public static final String PROXY_TARGET_PROP = "ecm:proxyTargetId";

    public static final String PROXY_TARGET_KEY = "targetid";

    public static final String PROXY_VERSIONABLE_PROP = "ecm:proxyVersionableId";

    public static final String PROXY_VERSIONABLE_KEY = "versionableid";

    public static final String LOCK_TABLE_NAME = "locks";

    public static final String LOCK_OWNER_PROP = "ecm:lockOwner";

    public static final String LOCK_OWNER_KEY = "owner";

    public static final String LOCK_CREATED_PROP = "ecm:lockCreated";

    public static final String LOCK_CREATED_KEY = "created";

    public static final String FULLTEXT_DEFAULT_INDEX = "default"; // not
                                                                   // config

    public static final String FULLTEXT_TABLE_NAME = "fulltext";

    public static final String FULLTEXT_JOBID_PROP = "ecm:fulltextJobId";

    public static final String FULLTEXT_JOBID_KEY = "jobid";

    public static final String FULLTEXT_FULLTEXT_PROP = "ecm:fulltext";

    public static final String FULLTEXT_FULLTEXT_KEY = "fulltext";

    public static final String FULLTEXT_SIMPLETEXT_PROP = "ecm:simpleText";

    public static final String FULLTEXT_SIMPLETEXT_KEY = "simpletext";

    public static final String FULLTEXT_BINARYTEXT_PROP = "ecm:binaryText";

    public static final String FULLTEXT_BINARYTEXT_KEY = "binarytext";

    public static final String HIER_READ_ACL_TABLE_NAME = "hierarchy_read_acl";

    public static final String HIER_READ_ACL_ID = "id";

    public static final String HIER_READ_ACL_ACL_ID = "acl_id";

    /** Specified in ext. point to use CLOBs. */
    public static final String FIELD_TYPE_LARGETEXT = "largetext";

    /**
     * Special (non-schema-based) simple fragments present in all types.
     * {@link #FULLTEXT_TABLE_NAME} is added to it if not disabled.
     */
    public static final List<String> COMMON_SIMPLE_FRAGMENTS = Arrays.asList(
            VERSION_TABLE_NAME, MISC_TABLE_NAME);

    /** Special (non-schema-based) collection fragments present in all types. */
    public static final String[] COMMON_COLLECTION_FRAGMENTS = { ACL_TABLE_NAME };

    /** Fragments that are always prefetched. */
    public static final String[] ALWAYS_PREFETCHED_FRAGMENTS = {
            ACL_TABLE_NAME, VERSION_TABLE_NAME, MISC_TABLE_NAME };

    protected final RepositoryDescriptor repositoryDescriptor;

    private final AtomicLong temporaryIdCounter = new AtomicLong(0);

    /** Per-doctype list of schemas. */
    private final Map<String, Set<String>> documentTypesSchemas;

    /** Per-mixin list of document types. */
    private final Map<String, Set<String>> mixinsDocumentTypes;

    /** Per-mixin list of schemas. */
    private final Map<String, Set<String>> mixinsSchemas;

    /** Shared high-level properties that don't come from the schema manager. */
    private final Map<String, Type> specialPropertyTypes;

    /** Per-schema/type info about properties, using their key. */
    private final HashMap<String, Map<String, ModelProperty>> schemaPropertyKeyInfos;

    /** Per-schema/type info about properties. */
    private final HashMap<String, Map<String, ModelProperty>> schemaPropertyInfos;

    /** Per-mixin info about properties. */
    private final HashMap<String, Map<String, ModelProperty>> mixinPropertyInfos;

    /** Shared properties. */
    private final Map<String, ModelProperty> sharedPropertyInfos;

    /** Merged properties (all schemas together + shared). */
    private final Map<String, ModelProperty> mergedPropertyInfos;

    /** Per-schema map of path to property info. */
    private final Map<String, Map<String, ModelProperty>> schemaPathPropertyInfos;

    /** Per-schema set of path to simple fulltext properties. */
    private final Map<String, Set<String>> schemaSimpleTextPaths;

    /**
     * Map of path (from all doc types) to property info. Value is NONE for
     * valid complex property path prefixes.
     */
    private final Map<String, ModelProperty> allPathPropertyInfos;

    /** Per-table info about fragments keys type. */
    private final Map<String, Map<String, ColumnType>> fragmentsKeys;

    /** All binary columns, fragment name -> keys. */
    private final Map<String, List<String>> binaryPropertyInfos;

    /** Maps collection table names to their type. */
    private final Map<String, PropertyType> collectionTables;

    /** Column ordering for collections. */
    private final Map<String, String> collectionOrderBy;

    /**
     * The fragment for each schema, or {@code null} if the schema doesn't have
     * a fragment.
     */
    private final Map<String, String> schemaFragment;

    /** Maps schema to collection fragments. */
    protected final Map<String, Set<String>> typeCollectionFragments;

    /** Maps schema to simple+collection fragments. */
    protected final Map<String, Set<String>> typeFragments;

    /** Maps mixin to simple+collection fragments. */
    protected final Map<String, Set<String>> mixinFragments;

    /** Maps document type or schema to prefetched fragments. */
    protected final Map<String, Set<String>> typePrefetchedFragments;

    /** Map of doc types to facets, for search. */
    protected final Map<String, Set<String>> documentTypesFacets;

    /** Map of doc type to its supertype, for search. */
    protected final Map<String, String> documentSuperTypes;

    /** Map of doc type to its subtypes (including itself), for search. */
    protected final Map<String, Set<String>> documentSubTypes;

    /** Map of field name to fragments holding them */
    protected final Map<String, Set<String>> fieldFragments;

    public final ModelFulltext fulltextInfo;

    private final boolean materializeFulltextSyntheticColumn;

    public Model(ModelSetup modelSetup) throws StorageException {
        repositoryDescriptor = modelSetup.repositoryDescriptor;
        materializeFulltextSyntheticColumn = modelSetup.materializeFulltextSyntheticColumn;

        documentTypesSchemas = new HashMap<String, Set<String>>();
        mixinsDocumentTypes = new HashMap<String, Set<String>>();
        mixinsSchemas = new HashMap<String, Set<String>>();

        schemaPropertyKeyInfos = new HashMap<String, Map<String, ModelProperty>>();
        schemaPropertyInfos = new HashMap<String, Map<String, ModelProperty>>();
        mixinPropertyInfos = new HashMap<String, Map<String, ModelProperty>>();
        sharedPropertyInfos = new HashMap<String, ModelProperty>();
        mergedPropertyInfos = new HashMap<String, ModelProperty>();
        schemaPathPropertyInfos = new HashMap<String, Map<String, ModelProperty>>();
        schemaSimpleTextPaths = new HashMap<String, Set<String>>();
        allPathPropertyInfos = new HashMap<String, ModelProperty>();
        fulltextInfo = new ModelFulltext();
        fragmentsKeys = new HashMap<String, Map<String, ColumnType>>();
        binaryPropertyInfos = new HashMap<String, List<String>>();

        collectionTables = new HashMap<String, PropertyType>();
        collectionOrderBy = new HashMap<String, String>();

        schemaFragment = new HashMap<String, String>();
        typeFragments = new HashMap<String, Set<String>>();
        mixinFragments = new HashMap<String, Set<String>>();
        typeCollectionFragments = new HashMap<String, Set<String>>();
        typePrefetchedFragments = new HashMap<String, Set<String>>();
        fieldFragments = new HashMap<String, Set<String>>();

        documentTypesFacets = new HashMap<String, Set<String>>();
        documentSuperTypes = new HashMap<String, String>();
        documentSubTypes = new HashMap<String, Set<String>>();

        specialPropertyTypes = new HashMap<String, Type>();

        initMainModel();
        initVersionsModel();
        initProxiesModel();
        initLocksModel();
        initAclModel();
        initMiscModel();
        initModels(modelSetup.schemaManager);
        if (!repositoryDescriptor.fulltextDisabled) {
            initFullTextModel();
        }
    }

    /**
     * Gets the repository descriptor used for this model.
     *
     * @return the repository descriptor
     */
    public RepositoryDescriptor getRepositoryDescriptor() {
        return repositoryDescriptor;
    }

    /**
     * Computes a new unique id.
     * <p>
     * If actual ids are computed by the database, this will be a temporary id,
     * otherwise the final one.
     *
     * @return a new id, which may be temporary
     */
    public Serializable generateNewId() {
        if (DEBUG_UUIDS) {
            return "UUID_" + temporaryIdCounter.incrementAndGet();
        } else {
            return UUID.randomUUID().toString();
        }
    }

    /**
     * Fixup an id that has been turned into a string for high-level Nuxeo APIs.
     *
     * @param id the id to fixup
     * @return the fixed up id
     */
    public Serializable unHackStringId(String id) {
        return id;
        // if (id.startsWith("T")) {
        // return id;
        // }
        // Document ids coming from higher level have been turned into strings
        // (by SQLDocument.getUUID) but are really longs for the backend.
        // return Long.valueOf(id);
    }

    /**
     * Records info about one property, in a schema-based structure and in a
     * table-based structure.
     * <p>
     * If {@literal schemaName} is {@code null}, then the property applies to
     * all types (system properties).
     */
    private void addPropertyInfo(String schemaName, String propertyName,
            PropertyType propertyType, String fragmentName, String fragmentKey,
            boolean readonly, Type coreType, ColumnType type) {
        // per-type
        Map<String, ModelProperty> propertyKeyInfos;
        Map<String, ModelProperty> propertyInfos;
        if (schemaName == null) {
            propertyKeyInfos = null;
            propertyInfos = sharedPropertyInfos;
        } else {
            propertyKeyInfos = schemaPropertyKeyInfos.get(schemaName);
            if (propertyKeyInfos == null) {
                propertyKeyInfos = new HashMap<String, ModelProperty>();
                schemaPropertyKeyInfos.put(schemaName, propertyKeyInfos);
            }
            propertyInfos = schemaPropertyInfos.get(schemaName);
            if (propertyInfos == null) {
                propertyInfos = new HashMap<String, ModelProperty>();
                schemaPropertyInfos.put(schemaName, propertyInfos);
            }
        }
        ModelProperty propertyInfo = new ModelProperty(propertyType,
                fragmentName, fragmentKey, readonly);
        propertyInfos.put(propertyName, propertyInfo);
        if (propertyKeyInfos != null && fragmentKey != null) {
            propertyKeyInfos.put(fragmentKey, propertyInfo);
        }

        // per-fragment keys type
        if (fragmentKey != null) {
            Map<String, ColumnType> fragmentKeys = fragmentsKeys.get(fragmentName);
            if (fragmentKeys == null) {
                fragmentsKeys.put(fragmentName,
                        fragmentKeys = new LinkedHashMap<String, ColumnType>());
            }
            fragmentKeys.put(fragmentKey, type);

            // record binary columns for the GC
            if (type.spec == ColumnSpec.BLOBID) {
                List<String> keys = binaryPropertyInfos.get(fragmentName);
                if (keys == null) {
                    binaryPropertyInfos.put(fragmentName,
                            keys = new ArrayList<String>(1));
                }
                keys.add(fragmentKey);
            }
        }

        // system properties
        if (coreType != null) {
            specialPropertyTypes.put(propertyName, coreType);
        }

        // merged properties
        ModelProperty previous = mergedPropertyInfos.get(propertyName);
        if (previous == null) {
            mergedPropertyInfos.put(propertyName, propertyInfo);
        } else {
            log.debug(String.format(
                    "Schemas '%s' and '%s' both have a property '%s', "
                            + "unqualified reference in queries will use schema '%1$s'",
                    previous.fragmentName, fragmentName, propertyName));
        }
        // compatibility for use of schema name as prefix
        if (!propertyName.contains(":")) {
            // allow schema name as prefix
            propertyName = schemaName + ':' + propertyName;
            previous = mergedPropertyInfos.get(propertyName);
            if (previous == null) {
                mergedPropertyInfos.put(propertyName, propertyInfo);
            }
        }
    }

    /**
     * Infers type property information from all its schemas.
     */
    private void inferTypePropertyInfos(String typeName, String[] schemaNames) {
        Map<String, ModelProperty> propertyInfos = schemaPropertyInfos.get(typeName);
        if (propertyInfos == null) {
            propertyInfos = new HashMap<String, ModelProperty>();
            schemaPropertyInfos.put(typeName, propertyInfos);
        }
        for (String schemaName : schemaNames) {
            Map<String, ModelProperty> infos = schemaPropertyInfos.get(schemaName);
            if (infos == null) {
                // schema with no properties (complex list)
                continue;
            }
            for (Entry<String, ModelProperty> info : infos.entrySet()) {
                propertyInfos.put(info.getKey(), info.getValue());
            }
        }
    }

    /**
     * Infers mixin property information from all its schemas.
     */
    private void inferMixinPropertyInfos(String mixin, String[] schemaNames) {
        Map<String, ModelProperty> propertyInfos = schemaPropertyInfos.get(mixin);
        if (propertyInfos == null) {
            mixinPropertyInfos.put(mixin,
                    propertyInfos = new HashMap<String, ModelProperty>());
        }
        for (String schemaName : schemaNames) {
            Map<String, ModelProperty> infos = schemaPropertyInfos.get(schemaName);
            if (infos == null) {
                // schema with no properties (complex list)
                continue;
            }
            for (Entry<String, ModelProperty> info : infos.entrySet()) {
                propertyInfos.put(info.getKey(), info.getValue());
            }
        }
    }

    /**
     * Infers all possible paths for properties in a document type.
     */
    private void inferTypePropertyPaths(DocumentType documentType) {
        for (Schema schema : documentType.getSchemas()) {
            if (schema == null) {
                // happens when a type refers to a nonexistent schema
                // TODO log and avoid nulls earlier
                continue;
            }
            inferSchemaPropertyPaths(schema);
        }
    }

    /**
     * Infers all possible paths for properties in a schema.
     */
    private void inferSchemaPropertyPaths(Schema schema) {
        String schemaName = schema.getName();
        if (schemaPathPropertyInfos.containsKey(schemaName)) {
            return;
        }
        Map<String, ModelProperty> propertyInfoByPath = new HashMap<String, ModelProperty>();
        inferTypePropertyPaths(schema, "", propertyInfoByPath, null);
        schemaPathPropertyInfos.put(schemaName, propertyInfoByPath);
        // allow schema-as-prefix if schemas has no prefix, if non-complex
        Map<String, ModelProperty> alsoWithPrefixes = new HashMap<String, ModelProperty>(
                propertyInfoByPath);
        if (schema.getNamespace().prefix.isEmpty()) {
            for (Entry<String, ModelProperty> e : propertyInfoByPath.entrySet()) {
                String key = e.getKey();
                if (!key.contains("/")) {
                    alsoWithPrefixes.put(schemaName + ':' + key, e.getValue());
                }
            }
        }
        allPathPropertyInfos.putAll(alsoWithPrefixes);
        // those for simpletext properties
        Set<String> simplePaths = new HashSet<String>();
        for (Entry<String, ModelProperty> entry : propertyInfoByPath.entrySet()) {
            ModelProperty pi = entry.getValue();
            if (pi == ModelProperty.NONE) {
                continue;
            }
            if (pi.propertyType != PropertyType.STRING
                    && pi.propertyType != PropertyType.ARRAY_STRING) {
                continue;
            }
            simplePaths.add(entry.getKey());
        }
        schemaSimpleTextPaths.put(schemaName, simplePaths);
    }

    // recurses in a complex type
    private void inferTypePropertyPaths(ComplexType complexType, String prefix,
            Map<String, ModelProperty> propertyInfoByPath, Set<String> done) {
        if (done == null) {
            done = new LinkedHashSet<String>();
        }
        String typeName = complexType.getName();
        if (done.contains(typeName)) {
            log.warn("Complex type " + typeName
                    + " refers to itself recursively: " + done);
            // stop recursion
            return;
        }
        done.add(typeName);

        for (Field field : complexType.getFields()) {
            String propertyName = field.getName().getPrefixedName();
            String path = prefix + propertyName;
            Type fieldType = field.getType();
            if (fieldType.isComplexType()) {
                // complex type
                propertyInfoByPath.put(path, ModelProperty.NONE);
                inferTypePropertyPaths((ComplexType) fieldType, path + '/',
                        propertyInfoByPath, done);
                continue;
            } else if (fieldType.isListType()) {
                Type listFieldType = ((ListType) fieldType).getFieldType();
                if (!listFieldType.isSimpleType()) {
                    // complex list
                    propertyInfoByPath.put(path + "/*", ModelProperty.NONE);
                    inferTypePropertyPaths((ComplexType) listFieldType, path
                            + "/*/", propertyInfoByPath, done);
                    continue;
                }
                // else array
            }
            // else primitive type
            ModelProperty pi = schemaPropertyInfos.get(typeName).get(
                    propertyName);
            propertyInfoByPath.put(path, pi);
            if (pi.propertyType.isArray()) {
                // also add the propname/* path for array elements
                propertyInfoByPath.put(path + "/*", pi);
            }
        }
        done.remove(typeName);
    }

    /**
     * Infers fulltext info for all schemas.
     */
    @SuppressWarnings("unchecked")
    private void inferFulltextInfo(SchemaManager schemaManager) {
        List<FulltextIndexDescriptor> descs = repositoryDescriptor.fulltextIndexes;
        if (descs == null) {
            descs = new ArrayList<FulltextIndexDescriptor>(1);
        }
        if (descs.isEmpty()) {
            descs.add(new FulltextIndexDescriptor());
        }
        for (FulltextIndexDescriptor desc : descs) {
            String name = desc.name == null ? FULLTEXT_DEFAULT_INDEX
                    : desc.name;
            fulltextInfo.indexNames.add(name);
            fulltextInfo.indexAnalyzer.put(
                    name,
                    desc.analyzer == null ? repositoryDescriptor.fulltextAnalyzer
                            : desc.analyzer);
            fulltextInfo.indexCatalog.put(name,
                    desc.catalog == null ? repositoryDescriptor.fulltextCatalog
                            : desc.catalog);
            if (desc.fields == null) {
                desc.fields = new HashSet<String>();
            }
            if (desc.excludeFields == null) {
                desc.excludeFields = new HashSet<String>();
            }
            if (desc.fields.size() == 1 && desc.excludeFields.isEmpty()) {
                fulltextInfo.fieldToIndexName.put(
                        desc.fields.iterator().next(), name);
            }

            if (desc.fieldType != null) {
                if (desc.fieldType.equals(ModelFulltext.PROP_TYPE_STRING)) {
                    fulltextInfo.indexesAllSimple.add(name);
                } else if (desc.fieldType.equals(ModelFulltext.PROP_TYPE_BLOB)) {
                    fulltextInfo.indexesAllBinary.add(name);
                } else {
                    log.error("Ignoring unknow repository fulltext configuration fieldType: "
                            + desc.fieldType);
                }

            }
            if (desc.fields.isEmpty() && desc.fieldType == null) {
                // no fields specified and no field type -> all of them
                fulltextInfo.indexesAllSimple.add(name);
                fulltextInfo.indexesAllBinary.add(name);
            }

            if (repositoryDescriptor.fulltextExcludedTypes != null) {
                fulltextInfo.excludedTypes.addAll(repositoryDescriptor.fulltextExcludedTypes);
            }
            if (repositoryDescriptor.fulltextIncludedTypes != null) {
                fulltextInfo.includedTypes.addAll(repositoryDescriptor.fulltextIncludedTypes);
            }

            for (Set<String> fields : Arrays.asList(desc.fields,
                    desc.excludeFields)) {
                for (String path : fields) {
                    ModelProperty pi = allPathPropertyInfos.get(path);
                    if (pi == null) {
                        log.error(String.format(
                                "Ignoring unknown property '%s' in fulltext configuration: %s",
                                path, name));
                        continue;
                    }
                    Map<String, Set<String>> indexesByPropPath;
                    Map<String, Set<String>> propPathsByIndex;
                    if (pi.propertyType == PropertyType.STRING
                            || pi.propertyType == PropertyType.ARRAY_STRING) {
                        indexesByPropPath = fields == desc.fields ? fulltextInfo.indexesByPropPathSimple
                                : fulltextInfo.indexesByPropPathExcludedSimple;
                        propPathsByIndex = fields == desc.fields ? fulltextInfo.propPathsByIndexSimple
                                : fulltextInfo.propPathsExcludedByIndexSimple;
                    } else if (pi.propertyType == PropertyType.BINARY) {
                        indexesByPropPath = fields == desc.fields ? fulltextInfo.indexesByPropPathBinary
                                : fulltextInfo.indexesByPropPathExcludedBinary;
                        propPathsByIndex = fields == desc.fields ? fulltextInfo.propPathsByIndexBinary
                                : fulltextInfo.propPathsExcludedByIndexBinary;
                    } else {
                        log.error(String.format(
                                "Ignoring property '%s' with bad type %s in fulltext configuration: %s",
                                path, pi.propertyType, name));
                        continue;
                    }
                    Set<String> indexes = indexesByPropPath.get(path);
                    if (indexes == null) {
                        indexesByPropPath.put(path,
                                indexes = new HashSet<String>());
                    }
                    indexes.add(name);
                    Set<String> paths = propPathsByIndex.get(name);
                    if (paths == null) {
                        propPathsByIndex.put(name,
                                paths = new LinkedHashSet<String>());
                    }
                    paths.add(path);
                }
            }
        }

        // Add document types with the NotFulltextIndexable facet
        for (DocumentType documentType : schemaManager.getDocumentTypes()) {
            if (documentType.hasFacet(FacetNames.NOT_FULLTEXT_INDEXABLE)) {
                fulltextInfo.excludedTypes.add(documentType.getName());
            }
        }
    }

    public ModelProperty getPropertyInfo(String typeName, String propertyName) {
        Map<String, ModelProperty> propertyInfos = schemaPropertyInfos.get(typeName);
        if (propertyInfos == null) {
            // no such type/schema
            return null;
        }
        ModelProperty propertyInfo = propertyInfos.get(propertyName);
        return propertyInfo != null ? propertyInfo
                : sharedPropertyInfos.get(propertyName);
    }

    public Map<String, ModelProperty> getMixinPropertyInfos(String mixin) {
        return mixinPropertyInfos.get(mixin);
    }

    public ModelProperty getMixinPropertyInfo(String mixin, String propertyName) {
        Map<String, ModelProperty> propertyInfos = mixinPropertyInfos.get(mixin);
        if (propertyInfos == null) {
            // no such mixin
            return null;
        }
        return propertyInfos.get(propertyName);
    }

    public ModelProperty getPropertyInfo(String propertyName) {
        return mergedPropertyInfos.get(propertyName);
    }

    /** returns {@link ModelProperty#NONE} if legal prefix */
    public ModelProperty getPathPropertyInfo(String xpath) {
        return allPathPropertyInfos.get(xpath);
    }

    public Set<String> getPropertyInfoNames() {
        return mergedPropertyInfos.keySet();
    }

    public ModelProperty getPathPropertyInfo(String primaryType,
            String[] mixinTypes, String path) {
        for (String schema : getAllSchemas(primaryType, mixinTypes)) {
            Map<String, ModelProperty> propertyInfoByPath = schemaPathPropertyInfos.get(schema);
            if (propertyInfoByPath != null) {
                ModelProperty pi = propertyInfoByPath.get(path);
                if (pi != null) {
                    return pi;
                }
            }
        }
        return null;
    }

    public Set<String> getSimpleTextPropertyPaths(String primaryType,
            String[] mixinTypes) {
        Set<String> paths = new HashSet<String>();
        for (String schema : getAllSchemas(primaryType, mixinTypes)) {
            Set<String> p = schemaSimpleTextPaths.get(schema);
            if (p != null) {
                paths.addAll(p);
            }
        }
        return paths;
    }

    public Set<String> getAllSchemas(String primaryType, String[] mixinTypes) {
        Set<String> schemas = new LinkedHashSet<String>();
        Set<String> s = documentTypesSchemas.get(primaryType);
        if (s != null) {
            schemas.addAll(s);
        }
        for (String mixin : mixinTypes) {
            s = mixinsSchemas.get(mixin);
            if (s != null) {
                schemas.addAll(s);
            }
        }
        return schemas;
    }

    public ModelFulltext getFulltextInfo() {
        return fulltextInfo;
    }

    /**
     * Finds out if a field is to be indexed as fulltext.
     *
     * @param fragmentName
     * @param fragmentKey the key or {@code null} for a collection
     * @return {@link PropertyType#STRING} or {@link PropertyType#BINARY} if
     *         this field is to be indexed as fulltext
     */
    public PropertyType getFulltextFieldType(String fragmentName,
            String fragmentKey) {
        if (fragmentKey == null) {
            PropertyType type = collectionTables.get(fragmentName);
            if (type == PropertyType.ARRAY_STRING
                    || type == PropertyType.ARRAY_BINARY) {
                return type.getArrayBaseType();
            }
            return null;
        } else {
            Map<String, ModelProperty> infos = schemaPropertyKeyInfos.get(fragmentName);
            if (infos == null) {
                return null;
            }
            ModelProperty info = infos.get(fragmentKey);
            if (info != null && info.fulltext) {
                return info.propertyType;
            }
            return null;
        }
    }

    private void addCollectionFragmentInfos(String fragmentName,
            PropertyType propertyType, String orderBy,
            Map<String, ColumnType> keysType) {
        collectionTables.put(fragmentName, propertyType);
        collectionOrderBy.put(fragmentName, orderBy);
        // set all keys types
        Map<String, ColumnType> old = fragmentsKeys.get(fragmentName);
        if (old == null) {
            fragmentsKeys.put(fragmentName, keysType);
        } else {
            old.putAll(keysType);
        }
    }

    public Type getSpecialPropertyType(String propertyName) {
        return specialPropertyTypes.get(propertyName);
    }

    public PropertyType getCollectionFragmentType(String fragmentName) {
        return collectionTables.get(fragmentName);
    }

    public boolean isCollectionFragment(String fragmentName) {
        return collectionTables.containsKey(fragmentName);
    }

    public String getCollectionOrderBy(String fragmentName) {
        return collectionOrderBy.get(fragmentName);
    }

    public Set<String> getFragmentNames() {
        return fragmentsKeys.keySet();
    }

    public Map<String, ColumnType> getFragmentKeysType(String fragmentName) {
        return fragmentsKeys.get(fragmentName);
    }

    public Map<String, List<String>> getBinaryPropertyInfos() {
        return binaryPropertyInfos;
    }

    protected void addTypeCollectionFragment(String typeName,
            String fragmentName) {
        Set<String> fragments = typeCollectionFragments.get(typeName);
        if (fragments == null) {
            fragments = new HashSet<String>();
            typeCollectionFragments.put(typeName, fragments);
        }
        fragments.add(fragmentName);
        addTypeFragment(typeName, fragmentName);
    }

    protected void addTypeFragment(String typeName, String fragmentName) {
        Set<String> fragments = typeFragments.get(typeName);
        if (fragments == null) {
            typeFragments.put(typeName, fragments = new HashSet<String>());
        }
        // fragmentName may be null, to just create the entry
        if (fragmentName != null) {
            fragments.add(fragmentName);
        }
    }

    protected void addMixinFragment(String mixin, String fragmentName) {
        Set<String> fragments = mixinFragments.get(mixin);
        if (fragments == null) {
            mixinFragments.put(mixin, fragments = new HashSet<String>());
        }
        fragments.add(fragmentName);
    }

    protected void addFieldFragment(Field field, String fragmentName) {
        String fieldName = field.getName().toString();
        Set<String> fragments = fieldFragments.get(fieldName);
        if (fragments == null) {
            fieldFragments.put(fieldName, fragments = new HashSet<String>());
        }
        fragments.add(fragmentName);
    }

    protected void addTypePrefetchedFragment(String typeName,
            String fragmentName) {
        Set<String> fragments = typePrefetchedFragments.get(typeName);
        if (fragments == null) {
            typePrefetchedFragments.put(typeName,
                    fragments = new HashSet<String>());
        }
        fragments.add(fragmentName);
    }

    public Set<String> getTypeFragments(String typeName) {
        return typeFragments.get(typeName);
    }

    public Set<String> getMixinFragments(String mixin) {
        return mixinFragments.get(mixin);
    }

    protected Set<String> getFieldFragments(String fieldName) {
        return fieldFragments.get(fieldName);
    }

    public Set<String> getTypePrefetchedFragments(String typeName) {
        return typePrefetchedFragments.get(typeName);
    }

    public boolean isType(String typeName) {
        return typeFragments.containsKey(typeName);
    }

    public boolean isDocumentType(String typeName) {
        return documentTypesFacets.containsKey(typeName);
    }

    public String getDocumentSuperType(String typeName) {
        return documentSuperTypes.get(typeName);
    }

    public Set<String> getDocumentSubTypes(String typeName) {
        return documentSubTypes.get(typeName);
    }

    public Set<String> getDocumentTypes() {
        return documentTypesFacets.keySet();
    }

    public Set<String> getDocumentTypeFacets(String typeName) {
        Set<String> facets = documentTypesFacets.get(typeName);
        return facets == null ? Collections.<String> emptySet() : facets;
    }

    public Set<String> getMixinDocumentTypes(String mixin) {
        Set<String> types = mixinsDocumentTypes.get(mixin);
        return types == null ? Collections.<String> emptySet() : types;
    }

    /**
     * Given a map of id to types, returns a map of fragment names to ids.
     */
    public Map<String, Set<Serializable>> getPerFragmentIds(
            Map<Serializable, IdWithTypes> idToTypes) {
        Map<String, Set<Serializable>> allFragmentIds = new HashMap<String, Set<Serializable>>();
        for (Entry<Serializable, IdWithTypes> e : idToTypes.entrySet()) {
            Serializable id = e.getKey();
            IdWithTypes typeInfo = e.getValue();
            for (String fragmentName : getTypeFragments(typeInfo)) {
                Set<Serializable> fragmentIds = allFragmentIds.get(fragmentName);
                if (fragmentIds == null) {
                    allFragmentIds.put(fragmentName,
                            fragmentIds = new HashSet<Serializable>());
                }
                fragmentIds.add(id);
            }
        }
        return allFragmentIds;
    }

    /**
     * Gets the type fragments for a primary type and mixin types. Hierarchy is
     * included.
     */
    public Set<String> getTypeFragments(IdWithTypes typeInfo) {
        Set<String> fragmentNames = new HashSet<String>();
        fragmentNames.add(HIER_TABLE_NAME);
        Set<String> tf = getTypeFragments(typeInfo.primaryType);
        if (tf != null) {
            // null if unknown type left in the database
            fragmentNames.addAll(tf);
        }
        String[] mixins = typeInfo.mixinTypes;
        if (mixins != null) {
            for (String mixin : mixins) {
                Set<String> mf = getMixinFragments(mixin);
                if (mf != null) {
                    fragmentNames.addAll(mf);
                }
            }
        }
        return fragmentNames;
    }

    private PropertyType mainIdType() {
        return PropertyType.STRING;
        // return PropertyType.LONG;
    }

    /**
     * Creates all the models.
     */
    private void initModels(SchemaManager schemaManager)
            throws StorageException {
        log.debug("Schemas fields from descriptor: "
                + repositoryDescriptor.schemaFields);
        for (DocumentType documentType : schemaManager.getDocumentTypes()) {
            String typeName = documentType.getName();
            addTypeFragment(typeName, null); // create entry

            Set<String> docTypeSchemas = new HashSet<String>();
            for (Schema schema : documentType.getSchemas()) {
                if (schema == null) {
                    // happens when a type refers to a nonexistent schema
                    // TODO log and avoid nulls earlier
                    continue;
                }
                try {
                    docTypeSchemas.add(schema.getName());
                    String fragmentName = initTypeModel(schema);
                    addTypeFragment(typeName, fragmentName); // may be null
                    // collection fragments too for this schema
                    Set<String> cols = typeCollectionFragments.get(schema.getName());
                    if (cols != null) {
                        for (String colFrag : cols) {
                            addTypeCollectionFragment(typeName, colFrag);
                        }
                    }
                } catch (Exception e) {
                    throw new StorageException(String.format(
                            "Error initializing schema '%s' "
                                    + "for document type '%s'",
                            schema.getName(), typeName), e);
                }
            }

            try {
                documentTypesSchemas.put(typeName, docTypeSchemas);
                inferTypePropertyInfos(typeName, documentType.getSchemaNames());
                inferTypePropertyPaths(documentType);
            } catch (Exception e) {
                throw new StorageException(String.format(
                        "Error initializing schemas for document type '%s'",
                        typeName), e);
            }

            for (String fragmentName : getCommonSimpleFragments(typeName)) {
                addTypeFragment(typeName, fragmentName);
            }
            for (String fragmentName : COMMON_COLLECTION_FRAGMENTS) {
                addTypeCollectionFragment(typeName, fragmentName);
            }

            // find fragments to prefetch
            PrefetchInfo prefetch = documentType.getPrefetchInfo();
            if (prefetch != null) {
                Set<String> typeFragments = getTypeFragments(typeName);
                for (String fieldName : prefetch.getFields()) {
                    // prefetch all the relevant fragments
                    // TODO deal with full xpath
                    Set<String> fragments = getFieldFragments(fieldName);
                    if (fragments != null) {
                        for (String fragment : fragments) {
                            if (typeFragments.contains(fragment)) {
                                addTypePrefetchedFragment(typeName, fragment);
                            }
                        }
                    }
                }
                for (String schemaName : prefetch.getSchemas()) {
                    String fragment = schemaFragment.get(schemaName);
                    if (fragment != null) {
                        addTypePrefetchedFragment(typeName, fragment);
                    }
                    Set<String> collectionFragments = typeCollectionFragments.get(typeName);
                    if (collectionFragments != null) {
                        for (String fragmentName : collectionFragments) {
                            addTypePrefetchedFragment(typeName, fragmentName);
                        }
                    }
                }
            }
            // always prefetch ACLs, versions, misc (for lifecycle), locks
            for (String fragmentName : ALWAYS_PREFETCHED_FRAGMENTS) {
                addTypePrefetchedFragment(typeName, fragmentName);
            }

            log.debug("Fragments for type " + typeName + ": "
                    + getTypeFragments(typeName) + ", prefetch: "
                    + getTypePrefetchedFragments(typeName));

            // record doc type and facets, super type, sub types
            Set<String> mixins = documentType.getFacets();
            documentTypesFacets.put(typeName, new HashSet<String>(mixins));
            for (String mixin : mixins) {
                Set<String> mixinTypes = mixinsDocumentTypes.get(mixin);
                if (mixinTypes == null) {
                    mixinsDocumentTypes.put(mixin,
                            mixinTypes = new HashSet<String>());
                }
                mixinTypes.add(typeName);
            }
            Type superType = documentType.getSuperType();
            if (superType != null) {
                String superTypeName = superType.getName();
                documentSuperTypes.put(typeName, superTypeName);
            }
        }

        // compute subtypes for all types
        for (String type : getDocumentTypes()) {
            String superType = type;
            do {
                Set<String> subTypes = documentSubTypes.get(superType);
                if (subTypes == null) {
                    subTypes = new HashSet<String>();
                    documentSubTypes.put(superType, subTypes);
                }
                subTypes.add(type);
                superType = documentSuperTypes.get(superType);
            } while (superType != null);
        }

        if (!repositoryDescriptor.fulltextDisabled) {
            // infer fulltext info
            inferFulltextInfo(schemaManager);
        }

        // mixins
        for (CompositeType type : schemaManager.getFacets()) {
            String mixin = type.getName();
            // some schemas may be referenced only by mixins,
            // register them here
            Set<String> mixinSchemas = new HashSet<String>();
            for (Schema schema : type.getSchemas()) {
                if (schema == null) {
                    // happens when a type refers to a nonexistent schema
                    // TODO log and avoid nulls earlier
                    continue;
                }
                mixinSchemas.add(schema.getName());
                String fragmentName = initTypeModel(schema);
                if (fragmentName != null) {
                    addMixinFragment(mixin, fragmentName);
                }
                // collection fragments too
                Set<String> cols = typeCollectionFragments.get(schema.getName());
                if (cols != null) {
                    for (String colFrag : cols) {
                        addMixinFragment(mixin, colFrag);
                    }
                }
                inferSchemaPropertyPaths(schema);
            }
            mixinsSchemas.put(mixin, mixinSchemas);
            inferMixinPropertyInfos(mixin, type.getSchemaNames());
            log.debug("Fragments for facet " + mixin + ": "
                    + getMixinFragments(mixin));
        }
    }

    protected List<String> getCommonSimpleFragments(String typeName) {
        List<String> fragments = COMMON_SIMPLE_FRAGMENTS;
        if (!repositoryDescriptor.fulltextDisabled
                && fulltextInfo.isFulltextIndexable(typeName)) {
            fragments = new ArrayList<String>(fragments);
            fragments.add(FULLTEXT_TABLE_NAME);
        }
        return fragments;
    }

    /**
     * Special model for the main table (the one containing the primary type
     * information).
     * <p>
     * If the main table is not separate from the hierarchy table, then it's
     * will not really be instantiated by itself but merged into the hierarchy
     * table.
     */
    private void initMainModel() {
        addPropertyInfo(null, MAIN_PRIMARY_TYPE_PROP, PropertyType.STRING,
                HIER_TABLE_NAME, MAIN_PRIMARY_TYPE_KEY, true, null,
                ColumnType.SYSNAME);
        addPropertyInfo(null, MAIN_MIXIN_TYPES_PROP, PropertyType.STRING,
                HIER_TABLE_NAME, MAIN_MIXIN_TYPES_KEY, false, null,
                ColumnType.SYSNAMEARRAY);
        addPropertyInfo(null, MAIN_CHECKED_IN_PROP, PropertyType.BOOLEAN,
                HIER_TABLE_NAME, MAIN_CHECKED_IN_KEY, false,
                BooleanType.INSTANCE, ColumnType.BOOLEAN);
        addPropertyInfo(null, MAIN_BASE_VERSION_PROP, mainIdType(),
                HIER_TABLE_NAME, MAIN_BASE_VERSION_KEY, false,
                StringType.INSTANCE, ColumnType.NODEVAL);
        addPropertyInfo(null, MAIN_MAJOR_VERSION_PROP, PropertyType.LONG,
                HIER_TABLE_NAME, MAIN_MAJOR_VERSION_KEY, false,
                LongType.INSTANCE, ColumnType.INTEGER);
        addPropertyInfo(null, MAIN_MINOR_VERSION_PROP, PropertyType.LONG,
                HIER_TABLE_NAME, MAIN_MINOR_VERSION_KEY, false,
                LongType.INSTANCE, ColumnType.INTEGER);
        addPropertyInfo(null, MAIN_IS_VERSION_PROP, PropertyType.BOOLEAN,
                HIER_TABLE_NAME, MAIN_IS_VERSION_KEY, false,
                BooleanType.INSTANCE, ColumnType.BOOLEAN);
    }

    /**
     * Special model for the "misc" table (lifecycle, dirty.).
     */
    private void initMiscModel() {
        addPropertyInfo(null, MISC_LIFECYCLE_POLICY_PROP, PropertyType.STRING,
                MISC_TABLE_NAME, MISC_LIFECYCLE_POLICY_KEY, false,
                StringType.INSTANCE, ColumnType.SYSNAME);
        addPropertyInfo(null, MISC_LIFECYCLE_STATE_PROP, PropertyType.STRING,
                MISC_TABLE_NAME, MISC_LIFECYCLE_STATE_KEY, false,
                StringType.INSTANCE, ColumnType.SYSNAME);
    }

    /**
     * Special model for the versions table.
     */
    private void initVersionsModel() {
        addPropertyInfo(null, VERSION_VERSIONABLE_PROP, mainIdType(),
                VERSION_TABLE_NAME, VERSION_VERSIONABLE_KEY, false,
                StringType.INSTANCE, ColumnType.NODEVAL);
        addPropertyInfo(null, VERSION_CREATED_PROP, PropertyType.DATETIME,
                VERSION_TABLE_NAME, VERSION_CREATED_KEY, false,
                DateType.INSTANCE, ColumnType.TIMESTAMP);
        addPropertyInfo(null, VERSION_LABEL_PROP, PropertyType.STRING,
                VERSION_TABLE_NAME, VERSION_LABEL_KEY, false,
                StringType.INSTANCE, ColumnType.SYSNAME);
        addPropertyInfo(null, VERSION_DESCRIPTION_PROP, PropertyType.STRING,
                VERSION_TABLE_NAME, VERSION_DESCRIPTION_KEY, false,
                StringType.INSTANCE, ColumnType.STRING);
        addPropertyInfo(null, VERSION_IS_LATEST_PROP, PropertyType.BOOLEAN,
                VERSION_TABLE_NAME, VERSION_IS_LATEST_KEY, false,
                BooleanType.INSTANCE, ColumnType.BOOLEAN);
        addPropertyInfo(null, VERSION_IS_LATEST_MAJOR_PROP,
                PropertyType.BOOLEAN, VERSION_TABLE_NAME,
                VERSION_IS_LATEST_MAJOR_KEY, false, BooleanType.INSTANCE,
                ColumnType.BOOLEAN);
    }

    /**
     * Special model for the proxies table.
     */
    private void initProxiesModel() {
        addPropertyInfo(PROXY_TYPE, PROXY_TARGET_PROP, mainIdType(),
                PROXY_TABLE_NAME, PROXY_TARGET_KEY, false, StringType.INSTANCE,
                ColumnType.NODEIDFKNP);
        addPropertyInfo(PROXY_TYPE, PROXY_VERSIONABLE_PROP, mainIdType(),
                PROXY_TABLE_NAME, PROXY_VERSIONABLE_KEY, false,
                StringType.INSTANCE, ColumnType.NODEVAL);
        addTypeFragment(PROXY_TYPE, PROXY_TABLE_NAME);
    }

    /**
     * Special model for the locks table (also, primary key has no foreign key,
     * see {@link SQLInfo#initFragmentSQL}.
     */
    private void initLocksModel() {
        addPropertyInfo(null, LOCK_OWNER_PROP, PropertyType.STRING,
                LOCK_TABLE_NAME, LOCK_OWNER_KEY, false, StringType.INSTANCE,
                ColumnType.SYSNAME);
        addPropertyInfo(null, LOCK_CREATED_PROP, PropertyType.DATETIME,
                LOCK_TABLE_NAME, LOCK_CREATED_KEY, false, DateType.INSTANCE,
                ColumnType.TIMESTAMP);
    }

    /**
     * Special model for the fulltext table.
     */
    private void initFullTextModel() {
        addPropertyInfo(null, FULLTEXT_JOBID_PROP, PropertyType.STRING,
                FULLTEXT_TABLE_NAME, FULLTEXT_JOBID_KEY, false,
                StringType.INSTANCE, ColumnType.SYSNAME);
        for (String indexName : fulltextInfo.indexNames) {
            String suffix = getFulltextIndexSuffix(indexName);
            if (materializeFulltextSyntheticColumn) {
                addPropertyInfo(null, FULLTEXT_FULLTEXT_PROP + suffix,
                        PropertyType.STRING, FULLTEXT_TABLE_NAME,
                        FULLTEXT_FULLTEXT_KEY + suffix, false,
                        StringType.INSTANCE, ColumnType.FTINDEXED);
            }
            addPropertyInfo(null, FULLTEXT_SIMPLETEXT_PROP + suffix,
                    PropertyType.STRING, FULLTEXT_TABLE_NAME,
                    FULLTEXT_SIMPLETEXT_KEY + suffix, false,
                    StringType.INSTANCE, ColumnType.FTSTORED);
            addPropertyInfo(null, FULLTEXT_BINARYTEXT_PROP + suffix,
                    PropertyType.STRING, FULLTEXT_TABLE_NAME,
                    FULLTEXT_BINARYTEXT_KEY + suffix, false,
                    StringType.INSTANCE, ColumnType.FTSTORED);
        }
    }

    public String getFulltextIndexSuffix(String indexName) {
        return indexName.equals(FULLTEXT_DEFAULT_INDEX) ? "" : '_' + indexName;
    }

    /**
     * Special collection-like model for the ACL table.
     */
    private void initAclModel() {
        Map<String, ColumnType> keysType = new LinkedHashMap<String, ColumnType>();
        keysType.put(ACL_POS_KEY, ColumnType.INTEGER);
        keysType.put(ACL_NAME_KEY, ColumnType.SYSNAME);
        keysType.put(ACL_GRANT_KEY, ColumnType.BOOLEAN);
        keysType.put(ACL_PERMISSION_KEY, ColumnType.SYSNAME);
        keysType.put(ACL_USER_KEY, ColumnType.SYSNAME);
        keysType.put(ACL_GROUP_KEY, ColumnType.SYSNAME);
        String fragmentName = ACL_TABLE_NAME;
        addCollectionFragmentInfos(fragmentName, PropertyType.COLL_ACL,
                ACL_POS_KEY, keysType);
        addPropertyInfo(null, ACL_PROP, PropertyType.COLL_ACL, fragmentName,
                null, false, null, null);
    }

    /**
     * Creates the model for one schema or complex type.
     *
     * @return the fragment table name for this type, or {@code null} if this
     *         type doesn't directly hold data
     */
    private String initTypeModel(ComplexType complexType)
            throws StorageException {
        String typeName = complexType.getName();
        if (schemaFragment.containsKey(typeName)) {
            return schemaFragment.get(typeName); // may be null
        }

        log.debug("Making model for type " + typeName);

        /** Initialized if this type has a table associated. */
        String thisFragmentName = null;
        for (Field field : complexType.getFields()) {
            Type fieldType = field.getType();
            if (fieldType.isComplexType()) {
                /*
                 * Complex type.
                 */
                ComplexType fieldComplexType = (ComplexType) fieldType;
                String subTypeName = fieldComplexType.getName();
                String subFragmentName = initTypeModel(fieldComplexType);
                addTypeFragment(subTypeName, subFragmentName);
            } else {
                String propertyName = field.getName().getPrefixedName();
                if (fieldType.isListType()) {
                    Type listFieldType = ((ListType) fieldType).getFieldType();
                    if (listFieldType.isSimpleType()) {
                        /*
                         * Array: use a collection table.
                         */
                        String fragmentName = collectionFragmentName(propertyName);
                        PropertyType propertyType = PropertyType.fromFieldType(
                                listFieldType, true);
                        ColumnType type = ColumnType.fromFieldType(listFieldType);
                        if (type.spec == ColumnSpec.STRING) {
                            for (FieldDescriptor fd : repositoryDescriptor.schemaFields) {
                                if (propertyName.equals(fd.field)
                                        && FIELD_TYPE_LARGETEXT.equals(fd.type)) {
                                    type = ColumnType.CLOB;
                                    break;
                                }
                            }
                            log.debug("  String array field '" + propertyName
                                    + "' using column type " + type);
                        }
                        addPropertyInfo(typeName, propertyName, propertyType,
                                fragmentName, COLL_TABLE_VALUE_KEY, false,
                                null, type);

                        Map<String, ColumnType> keysType = new LinkedHashMap<String, ColumnType>();
                        keysType.put(COLL_TABLE_POS_KEY, ColumnType.INTEGER);
                        keysType.put(COLL_TABLE_VALUE_KEY, type);
                        addCollectionFragmentInfos(fragmentName, propertyType,
                                COLL_TABLE_POS_KEY, keysType);

                        addTypeCollectionFragment(typeName, fragmentName);
                        addFieldFragment(field, fragmentName);
                    } else {
                        /*
                         * Complex list.
                         */
                        String subFragmentName = initTypeModel((ComplexType) listFieldType);
                        addTypeFragment(listFieldType.getName(),
                                subFragmentName);
                    }
                } else {
                    /*
                     * Primitive type.
                     */
                    String fragmentName = typeFragmentName(complexType);
                    PropertyType propertyType = PropertyType.fromFieldType(
                            fieldType, false);
                    ColumnType type = ColumnType.fromField(field);
                    if (type.spec == ColumnSpec.STRING) {
                        // backward compat with largetext, since 5.4.2
                        for (FieldDescriptor fd : repositoryDescriptor.schemaFields) {
                            if (propertyName.equals(fd.field)
                                    && FIELD_TYPE_LARGETEXT.equals(fd.type)) {
                                if (!type.isUnconstrained() && !type.isClob()) {
                                    log.warn("  String field '" + propertyName
                                            + "' has a schema constraint to "
                                            + type
                                            + " but is specified as largetext,"
                                            + " using CLOB for it");
                                }
                                type = ColumnType.CLOB;
                                break;
                            }
                        }
                    }
                    String fragmentKey = field.getName().getLocalName();
                    if (MAIN_KEY.equalsIgnoreCase(fragmentKey)) {
                        String msg = "A property cannot be named '"
                                + fragmentKey
                                + "' because this is a reserved name, in type: "
                                + typeName;
                        throw new StorageException(msg);
                    }
                    if (fragmentName.equals(UID_SCHEMA_NAME)
                            && (fragmentKey.equals(UID_MAJOR_VERSION_KEY) || fragmentKey.equals(UID_MINOR_VERSION_KEY))) {
                        // workaround: special-case the "uid" schema, put
                        // major/minor
                        // in the hierarchy table
                        fragmentKey = fragmentKey.equals(UID_MAJOR_VERSION_KEY) ? MAIN_MAJOR_VERSION_KEY
                                : MAIN_MINOR_VERSION_KEY;
                        addPropertyInfo(typeName, propertyName, propertyType,
                                HIER_TABLE_NAME, fragmentKey, false, null, type);
                    } else {
                        addPropertyInfo(typeName, propertyName, propertyType,
                                fragmentName, fragmentKey, false, null, type);
                        // note that this type has a fragment
                        thisFragmentName = fragmentName;
                    }
                    addFieldFragment(field, fragmentName);
                }
            }
        }

        schemaFragment.put(typeName, thisFragmentName); // may be null
        return thisFragmentName;
    }

    private static String typeFragmentName(ComplexType type) {
        return type.getName();
    }

    private String collectionFragmentName(String propertyName) {
        return propertyName;
    }
}
