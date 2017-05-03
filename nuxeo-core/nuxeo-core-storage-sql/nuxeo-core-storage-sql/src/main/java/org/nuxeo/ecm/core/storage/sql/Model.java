/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.schema.DocumentType;
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
import org.nuxeo.ecm.core.storage.FulltextConfiguration;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.FieldDescriptor;
import org.nuxeo.ecm.core.storage.sql.RowMapper.IdWithTypes;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo;
import org.nuxeo.runtime.api.Framework;

/**
 * The {@link Model} is the link between high-level types and SQL-level objects (entity tables, collections). It defines
 * all policies relating to the choice of structure (what schema are grouped together in for optimization) and names in
 * the SQL database (table names, column names), and to what entity names (type name, field name) they correspond.
 * <p>
 * A Nuxeo schema or type is mapped to a SQL-level table. Several types can be aggregated in the same table. In theory,
 * a type could even be split into different tables.
 *
 * @author Florent Guillaume
 */
public class Model {

    private static final Log log = LogFactory.getLog(Model.class);

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

    public static final String MAIN_SYS_VERSION_PROP = "ecm:sysVersion";

    public static final String MAIN_SYS_VERSION_KEY = "sysversion";

    public static final String MAIN_CHANGE_TOKEN_PROP = "ecm:changeToken";

    public static final String MAIN_CHANGE_TOKEN_KEY = "changetoken";

    // for soft-delete
    public static final String MAIN_IS_DELETED_PROP = "ecm:isDeleted";

    // for soft-delete
    public static final String MAIN_IS_DELETED_KEY = "isdeleted";

    // for soft-delete
    public static final String MAIN_DELETED_TIME_PROP = "ecm:deletedTime";

    // for soft-delete
    public static final String MAIN_DELETED_TIME_KEY = "deletedtime";

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

    public static final String ACL_CREATOR_KEY = "creator";

    public static final String ACL_BEGIN_KEY = "begin";

    public static final String ACL_END_KEY = "end";

    public static final String ACL_STATUS_KEY = "status";

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

    public static final String ACLR_USER_MAP_TABLE_NAME = "aclr_user_map";

    public static final String ACLR_USER_MAP_USER_ID = "user_id";

    public static final String ACLR_USER_MAP_ACL_ID = "acl_id";

    /** Specified in ext. point to use CLOBs. */
    public static final String FIELD_TYPE_LARGETEXT = "largetext";

    /** Specified in ext. point to use array instead of collection table. */
    public static final String FIELD_TYPE_ARRAY = "array";

    /** Specified in ext. point to use CLOB array instead of collection table. */
    public static final String FIELD_TYPE_ARRAY_LARGETEXT = "array_largetext";

    // some random long that's not in the database
    // first half of md5 of "nosuchlongid"
    public static final Long NO_SUCH_LONG_ID = Long.valueOf(0x3153147dd69fcea4L);

    public static final Long INITIAL_CHANGE_TOKEN = Long.valueOf(0);

    protected final boolean softDeleteEnabled;

    protected final boolean proxiesEnabled;

    protected final boolean changeTokenEnabled;

    /** Type of ids as seen by the VCS Java layer. */
    public enum IdType {
        STRING, //
        LONG, //
    }

    // type of id seen by the VCS Java layer
    protected final IdType idType;

    // type for VCS row storage
    protected final PropertyType idPropertyType;

    // type for core properties
    protected final Type idCoreType;

    /**
     * If true, the misc columns are added to hierarchy, not to a separate misc table.
     */
    protected final boolean miscInHierarchy;

    protected final RepositoryDescriptor repositoryDescriptor;

    /** Per-doctype list of schemas. */
    private final Map<String, Set<String>> allDocTypeSchemas;

    /** Per-mixin list of schemas. */
    private final Map<String, Set<String>> allMixinSchemas;

    /** The proxy schemas. */
    private final Set<String> allProxySchemas;

    /** Map of mixin to doctypes. */
    private final Map<String, Set<String>> mixinsDocumentTypes;

    /** Map of doctype to mixins, for search. */
    protected final Map<String, Set<String>> documentTypesMixins;

    /** Shared high-level properties that don't come from the schema manager. */
    private final Map<String, Type> specialPropertyTypes;

    /** Map of fragment to key to property info. */
    private final Map<String, Map<String, ModelProperty>> fragmentPropertyInfos;

    /** Map of schema to property to property info. */
    private final Map<String, Map<String, ModelProperty>> schemaPropertyInfos;

    /** Map of docType/complexType to property to property info. */
    private final Map<String, Map<String, ModelProperty>> typePropertyInfos;

    /** Map of mixin to property to property info. */
    private final Map<String, Map<String, ModelProperty>> mixinPropertyInfos;

    /** The proxy property infos. */
    private final Map<String, ModelProperty> proxyPropertyInfos;

    /** Map of property to property info. */
    private final Map<String, ModelProperty> sharedPropertyInfos;

    /** Merged properties (all schemas together + shared). */
    private final Map<String, ModelProperty> mergedPropertyInfos;

    /** Per-schema map of path to property info. */
    private final Map<String, Map<String, ModelProperty>> schemaPathPropertyInfos;

    /** Map of prefix to schema. */
    private final Map<String, String> prefixToSchema;

    /** Per-schema set of path to simple fulltext properties. */
    private final Map<String, Set<String>> schemaSimpleTextPaths;

    /**
     * Map of path (from all doc types) to property info. Value is NONE for valid complex property path prefixes.
     */
    private final Map<String, ModelProperty> allPathPropertyInfos;

    /** Map of fragment to key to column type. */
    private final Map<String, Map<String, ColumnType>> fragmentKeyTypes;

    /** Map of fragment to keys for binary columns. */
    private final Map<String, List<String>> binaryFragmentKeys;

    /** Maps collection table names to their type. */
    private final Map<String, PropertyType> collectionTables;

    /** Column ordering for collections. */
    private final Map<String, String> collectionOrderBy;

    // -------------------------------------------------------

    /**
     * Map of schema to simple+collection fragments. Used while computing document type fragments, and for prefetch.
     */
    private final Map<String, Set<String>> schemaFragments;

    /** Map of doctype/complextype to simple+collection fragments. */
    protected final Map<String, Set<String>> typeFragments;

    /** Map of mixin to simple+collection fragments. */
    protected final Map<String, Set<String>> mixinFragments;

    /** The proxy fragments. */
    private final Set<String> proxyFragments;

    /** Map of doctype to prefetched fragments. */
    protected final Map<String, Set<String>> docTypePrefetchedFragments;

    /** Map of schema to child name to type. */
    protected final Map<String, Map<String, String>> schemaComplexChildren;

    /** Map of doctype/complextype to child name to type. */
    protected final Map<String, Map<String, String>> typeComplexChildren;

    /** Map of mixin to child name to type. */
    protected final Map<String, Map<String, String>> mixinComplexChildren;

    /** Map of doctype to its supertype, for search. */
    protected final Map<String, String> documentSuperTypes;

    /** Map of doctype to its subtypes (including itself), for search. */
    protected final Map<String, Set<String>> documentSubTypes;

    /** Map of field name to fragment holding it. Used for prefetch. */
    protected final Map<String, String> fieldFragment;

    protected final FulltextConfiguration fulltextConfiguration;

    protected final Set<String> noPerDocumentQueryFacets;

    /**
     * Map of fragment -> info about whether there's a fulltext text field (PropertyType.STRING), binary field
     * (PropertyType.BINARY), or both (PropertyType.BOOLEAN).
     */
    protected final Map<String, PropertyType> fulltextInfoByFragment;

    private final boolean materializeFulltextSyntheticColumn;

    private final boolean supportsArrayColumns;

    public Model(ModelSetup modelSetup) {
        repositoryDescriptor = modelSetup.repositoryDescriptor;
        materializeFulltextSyntheticColumn = modelSetup.materializeFulltextSyntheticColumn;
        supportsArrayColumns = modelSetup.supportsArrayColumns;
        idType = modelSetup.idType;
        switch (idType) {
        case STRING:
            idPropertyType = PropertyType.STRING;
            idCoreType = StringType.INSTANCE;
            break;
        case LONG:
            idPropertyType = PropertyType.LONG;
            idCoreType = LongType.INSTANCE;
            break;
        default:
            throw new AssertionError(idType.toString());
        }
        softDeleteEnabled = repositoryDescriptor.getSoftDeleteEnabled();
        proxiesEnabled = repositoryDescriptor.getProxiesEnabled();
        changeTokenEnabled = repositoryDescriptor.isChangeTokenEnabled();

        allDocTypeSchemas = new HashMap<String, Set<String>>();
        mixinsDocumentTypes = new HashMap<String, Set<String>>();
        documentTypesMixins = new HashMap<String, Set<String>>();
        allMixinSchemas = new HashMap<String, Set<String>>();
        allProxySchemas = new HashSet<String>();

        fragmentPropertyInfos = new HashMap<String, Map<String, ModelProperty>>();
        schemaPropertyInfos = new HashMap<String, Map<String, ModelProperty>>();
        typePropertyInfos = new HashMap<String, Map<String, ModelProperty>>();
        mixinPropertyInfos = new HashMap<String, Map<String, ModelProperty>>();
        proxyPropertyInfos = new HashMap<String, ModelProperty>();
        sharedPropertyInfos = new HashMap<String, ModelProperty>();
        mergedPropertyInfos = new HashMap<String, ModelProperty>();
        schemaPathPropertyInfos = new HashMap<String, Map<String, ModelProperty>>();
        prefixToSchema = new HashMap<String, String>();
        schemaSimpleTextPaths = new HashMap<String, Set<String>>();
        allPathPropertyInfos = new HashMap<String, ModelProperty>();
        fulltextInfoByFragment = new HashMap<String, PropertyType>();
        fragmentKeyTypes = new HashMap<String, Map<String, ColumnType>>();
        binaryFragmentKeys = new HashMap<String, List<String>>();

        collectionTables = new HashMap<String, PropertyType>();
        collectionOrderBy = new HashMap<String, String>();

        schemaFragments = new HashMap<String, Set<String>>();
        typeFragments = new HashMap<String, Set<String>>();
        mixinFragments = new HashMap<String, Set<String>>();
        proxyFragments = new HashSet<String>();
        docTypePrefetchedFragments = new HashMap<String, Set<String>>();
        fieldFragment = new HashMap<String, String>();

        schemaComplexChildren = new HashMap<String, Map<String, String>>();
        typeComplexChildren = new HashMap<String, Map<String, String>>();
        mixinComplexChildren = new HashMap<String, Map<String, String>>();

        documentSuperTypes = new HashMap<String, String>();
        documentSubTypes = new HashMap<String, Set<String>>();

        specialPropertyTypes = new HashMap<String, Type>();
        noPerDocumentQueryFacets = new HashSet<String>();

        miscInHierarchy = false;

        if (repositoryDescriptor.getFulltextDescriptor().getFulltextDisabled()) {
            fulltextConfiguration = null;
        } else {
            fulltextConfiguration = new FulltextConfiguration(repositoryDescriptor.getFulltextDescriptor());
        }

        initMainModel();
        initVersionsModel();
        if (proxiesEnabled) {
            initProxiesModel();
        }
        initLocksModel();
        initAclModel();
        initMiscModel();
        // models for all document types and mixins
        initModels();
        if (fulltextConfiguration != null) {
            inferFulltextInfoByFragment(); // needs mixin schemas
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
     * Fixup an id that has been turned into a string for high-level Nuxeo APIs.
     *
     * @param id the id to fixup
     * @return the fixed up id
     */
    public Serializable idFromString(String id) {
        switch (idType) {
        case STRING:
            return id;
        case LONG:
            // use isNumeric instead of try/catch for efficiency
            if (StringUtils.isNumeric(id)) {
                return Long.valueOf(id);
            } else {
                return NO_SUCH_LONG_ID;
            }
        default:
            throw new AssertionError(idType.toString());
        }
    }

    /**
     * Turns an id that may be a String or a Long into a String for high-level Nuxeo APIs.
     *
     * @param the serializable id
     * @return the string
     */
    public String idToString(Serializable id) {
        return id.toString();
    }

    /**
     * Records info about a system property (applying to all document types).
     */
    private void addPropertyInfo(String propertyName, PropertyType propertyType, String fragmentName,
            String fragmentKey, boolean readonly, Type coreType, ColumnType type) {
        addPropertyInfo(null, false, propertyName, propertyType, fragmentName, fragmentKey, readonly, coreType, type);
    }

    /**
     * Records info about one property (for a given type). Used for proxy properties.
     */
    private void addPropertyInfo(String typeName, String propertyName, PropertyType propertyType, String fragmentName,
            String fragmentKey, boolean readonly, Type coreType, ColumnType type) {
        addPropertyInfo(typeName, false, propertyName, propertyType, fragmentName, fragmentKey, readonly, coreType,
                type);
    }

    /**
     * Records info about one property from a complex type or schema.
     */
    private void addPropertyInfo(ComplexType complexType, String propertyName, PropertyType propertyType,
            String fragmentName, String fragmentKey, boolean readonly, Type coreType, ColumnType type) {
        String typeName = complexType.getName();
        boolean isSchema = complexType instanceof Schema;
        addPropertyInfo(typeName, isSchema, propertyName, propertyType, fragmentName, fragmentKey, readonly, coreType,
                type);
    }

    /**
     * Records info about one property, in a schema-based structure and in a fragment-based structure.
     */
    private void addPropertyInfo(String typeName, boolean isSchema, String propertyName, PropertyType propertyType,
            String fragmentName, String fragmentKey, boolean readonly, Type coreType, ColumnType type) {

        ModelProperty propertyInfo = new ModelProperty(propertyType, fragmentName, fragmentKey, readonly);

        // per type/schema property info
        Map<String, ModelProperty> propertyInfos;
        if (typeName == null) {
            propertyInfos = sharedPropertyInfos;
        } else {
            Map<String, Map<String, ModelProperty>> map = isSchema ? schemaPropertyInfos : typePropertyInfos;
            propertyInfos = map.get(typeName);
            if (propertyInfos == null) {
                map.put(typeName, propertyInfos = new HashMap<String, ModelProperty>());
            }
        }
        propertyInfos.put(propertyName, propertyInfo);

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
        if (typeName != null && !propertyName.contains(":")) {
            // allow schema name as prefix
            propertyName = typeName + ':' + propertyName;
            previous = mergedPropertyInfos.get(propertyName);
            if (previous == null) {
                mergedPropertyInfos.put(propertyName, propertyInfo);
            }
        }

        // system properties core type (no core schema available)
        if (coreType != null) {
            specialPropertyTypes.put(propertyName, coreType);
        }

        // per-fragment property info
        Map<String, ModelProperty> fragmentKeyInfos = fragmentPropertyInfos.get(fragmentName);
        if (fragmentKeyInfos == null) {
            fragmentPropertyInfos.put(fragmentName, fragmentKeyInfos = new HashMap<String, ModelProperty>());
        }
        if (fragmentKey != null) {
            fragmentKeyInfos.put(fragmentKey, propertyInfo);
        }

        // per-fragment keys type
        if (fragmentKey != null && type != null) {
            Map<String, ColumnType> fragmentKeys = fragmentKeyTypes.get(fragmentName);
            if (fragmentKeys == null) {
                fragmentKeyTypes.put(fragmentName, fragmentKeys = new LinkedHashMap<String, ColumnType>());
            }
            fragmentKeys.put(fragmentKey, type);

            // record binary columns for the GC
            if (type.spec == ColumnSpec.BLOBID) {
                List<String> keys = binaryFragmentKeys.get(fragmentName);
                if (keys == null) {
                    binaryFragmentKeys.put(fragmentName, keys = new ArrayList<String>(1));
                }
                keys.add(fragmentKey);
            }
        } // else collection, uses addCollectionFragmentInfos directly
    }

    private void addCollectionFragmentInfos(String fragmentName, PropertyType propertyType, String orderBy,
            Map<String, ColumnType> keysType) {
        collectionTables.put(fragmentName, propertyType);
        collectionOrderBy.put(fragmentName, orderBy);
        fragmentKeyTypes.put(fragmentName, keysType);
    }

    // fill the map for key with property infos for the given schemas
    // different maps are used for doctypes or mixins
    private void inferPropertyInfos(Map<String, Map<String, ModelProperty>> map, String key, Set<String> schemaNames) {
        Map<String, ModelProperty> propertyInfos = map.get(key);
        if (propertyInfos == null) {
            map.put(key, propertyInfos = new HashMap<String, ModelProperty>());
        }
        for (String schemaName : schemaNames) {
            Map<String, ModelProperty> infos = schemaPropertyInfos.get(schemaName);
            if (infos != null) {
                propertyInfos.putAll(infos);
            }
            // else schema with no properties (complex list)
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
        Map<String, ModelProperty> alsoWithPrefixes = new HashMap<String, ModelProperty>(propertyInfoByPath);
        String prefix = schema.getNamespace().prefix;
        if (prefix.isEmpty()) {
            for (Entry<String, ModelProperty> e : propertyInfoByPath.entrySet()) {
                alsoWithPrefixes.put(schemaName + ':' + e.getKey(), e.getValue());
            }
        } else {
            prefixToSchema.put(prefix, schemaName);
        }
        allPathPropertyInfos.putAll(alsoWithPrefixes);
        // those for simpletext properties
        Set<String> simplePaths = new HashSet<String>();
        for (Entry<String, ModelProperty> entry : propertyInfoByPath.entrySet()) {
            ModelProperty pi = entry.getValue();
            if (pi.isIntermediateSegment()) {
                continue;
            }
            if (pi.propertyType != PropertyType.STRING && pi.propertyType != PropertyType.ARRAY_STRING) {
                continue;
            }
            simplePaths.add(entry.getKey());
        }
        schemaSimpleTextPaths.put(schemaName, simplePaths);
    }

    // recurses in a schema or complex type
    private void inferTypePropertyPaths(ComplexType complexType, String prefix,
            Map<String, ModelProperty> propertyInfoByPath, Set<String> done) {
        if (done == null) {
            done = new LinkedHashSet<String>();
        }
        String typeName = complexType.getName();
        if (done.contains(typeName)) {
            log.warn("Complex type " + typeName + " refers to itself recursively: " + done);
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
                propertyInfoByPath.put(path, new ModelProperty(propertyName));
                inferTypePropertyPaths((ComplexType) fieldType, path + '/', propertyInfoByPath, done);
                continue;
            } else if (fieldType.isListType()) {
                Type listFieldType = ((ListType) fieldType).getFieldType();
                if (!listFieldType.isSimpleType()) {
                    // complex list
                    propertyInfoByPath.put(path + "/*", new ModelProperty(propertyName));
                    inferTypePropertyPaths((ComplexType) listFieldType, path + "/*/", propertyInfoByPath, done);
                    continue;
                }
                // else array
            }
            // else primitive type
            // in both cases, record it
            Map<String, Map<String, ModelProperty>> map = (complexType instanceof Schema) ? schemaPropertyInfos
                    : typePropertyInfos;
            ModelProperty pi = map.get(typeName).get(propertyName);
            propertyInfoByPath.put(path, pi);
            // also add the propname/* path for array elements
            if (pi.propertyType.isArray()) {
                propertyInfoByPath.put(path + "/*", pi);
                if (!supportsArrayColumns) {
                    // pseudo-syntax with ending "#" to get to the pos column
                    String posPropertyName = propertyName + "#";
                    ModelProperty posPi = map.get(typeName).get(posPropertyName);
                    propertyInfoByPath.put(path + "#", posPi);
                }
            }
        }
        done.remove(typeName);
    }

    private void inferFulltextInfoByFragment() {
        // simple fragments
        for (Entry<String, Map<String, ModelProperty>> es : fragmentPropertyInfos.entrySet()) {
            String fragmentName = es.getKey();
            Map<String, ModelProperty> infos = es.getValue();
            if (infos == null) {
                continue;
            }
            PropertyType type = null;
            for (ModelProperty info : infos.values()) {
                if (info != null && info.fulltext) {
                    PropertyType t = info.propertyType;
                    if (t == PropertyType.STRING || t == PropertyType.BINARY) {
                        if (type == null) {
                            type = t;
                            continue;
                        }
                        if (type != t) {
                            type = PropertyType.BOOLEAN; // both
                            break;
                        }
                    }
                }
            }
            fulltextInfoByFragment.put(fragmentName, type);
        }
        // collection fragments
        for (Entry<String, PropertyType> es : collectionTables.entrySet()) {
            String fragmentName = es.getKey();
            PropertyType type = es.getValue();
            if (type == PropertyType.ARRAY_STRING || type == PropertyType.ARRAY_BINARY) {
                fulltextInfoByFragment.put(fragmentName, type.getArrayBaseType());
            }
        }
    }

    /** Get doctype/complextype property info. */
    public ModelProperty getPropertyInfo(String typeName, String propertyName) {
        Map<String, ModelProperty> propertyInfos = typePropertyInfos.get(typeName);
        if (propertyInfos == null) {
            // no such doctype/complextype
            return null;
        }
        ModelProperty propertyInfo = propertyInfos.get(propertyName);
        return propertyInfo != null ? propertyInfo : sharedPropertyInfos.get(propertyName);
    }

    public Map<String, ModelProperty> getMixinPropertyInfos(String mixin) {
        return mixinPropertyInfos.get(mixin);
    }

    // for all types for now
    public ModelProperty getProxySchemasPropertyInfo(String propertyName) {
        ModelProperty propertyInfo = proxyPropertyInfos.get(propertyName);
        return propertyInfo != null ? propertyInfo : sharedPropertyInfos.get(propertyName);
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

    /**
     * Gets the model of the property for the given path. Returns something with
     * {@link ModelProperty#isIntermediateSegment()} = true for an intermediate segment of a complex property.
     */
    public ModelProperty getPathPropertyInfo(String xpath) {
        return allPathPropertyInfos.get(xpath);
    }

    public Set<String> getPropertyInfoNames() {
        return mergedPropertyInfos.keySet();
    }

    public ModelProperty getPathPropertyInfo(String primaryType, String[] mixinTypes, String path) {
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

    public Map<String, String> getTypeComplexChildren(String typeName) {
        return typeComplexChildren.get(typeName);
    }

    public Map<String, String> getMixinComplexChildren(String mixin) {
        return mixinComplexChildren.get(mixin);
    }

    public Set<String> getSimpleTextPropertyPaths(String primaryType, String[] mixinTypes) {
        Set<String> paths = new HashSet<String>();
        for (String schema : getAllSchemas(primaryType, mixinTypes)) {
            Set<String> p = schemaSimpleTextPaths.get(schema);
            if (p != null) {
                paths.addAll(p);
            }
        }
        return paths;
    }

    /**
     * Checks if the given xpath, when resolved on a proxy, points to a proxy-specific schema instead of the target
     * document.
     */
    public boolean isProxySchemaPath(String xpath) {
        int p = xpath.indexOf(':');
        if (p == -1) {
            return false; // no schema/prefix -> not on proxy
        }
        String prefix = xpath.substring(0, p);
        String schema = prefixToSchema.get(prefix);
        if (schema == null) {
            schema = prefix;
        }
        return allProxySchemas.contains(schema);
    }

    private Set<String> getAllSchemas(String primaryType, String[] mixinTypes) {
        Set<String> schemas = new LinkedHashSet<String>();
        Set<String> s = allDocTypeSchemas.get(primaryType);
        if (s != null) {
            schemas.addAll(s);
        }
        for (String mixin : mixinTypes) {
            s = allMixinSchemas.get(mixin);
            if (s != null) {
                schemas.addAll(s);
            }
        }
        return schemas;
    }

    public FulltextConfiguration getFulltextConfiguration() {
        return fulltextConfiguration;
    }

    /**
     * Finds out if a field is to be indexed as fulltext.
     *
     * @param fragmentName
     * @param fragmentKey the key or {@code null} for a collection
     * @return {@link PropertyType#STRING} or {@link PropertyType#BINARY} if this field is to be indexed as fulltext
     */
    public PropertyType getFulltextFieldType(String fragmentName, String fragmentKey) {
        if (fragmentKey == null) {
            PropertyType type = collectionTables.get(fragmentName);
            if (type == PropertyType.ARRAY_STRING || type == PropertyType.ARRAY_BINARY) {
                return type.getArrayBaseType();
            }
            return null;
        } else {
            Map<String, ModelProperty> infos = fragmentPropertyInfos.get(fragmentName);
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

    /**
     * Checks if a fragment has any field indexable as fulltext.
     *
     * @param fragmentName
     * @return PropertyType.STRING, PropertyType.BINARY, or PropertyType.BOOLEAN for both.
     */
    public PropertyType getFulltextInfoForFragment(String fragmentName) {
        return fulltextInfoByFragment.get(fragmentName);
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
        return fragmentKeyTypes.keySet();
    }

    public Map<String, ColumnType> getFragmentKeysType(String fragmentName) {
        return fragmentKeyTypes.get(fragmentName);
    }

    public Map<String, List<String>> getBinaryPropertyInfos() {
        return binaryFragmentKeys;
    }

    private void addTypeFragments(String typeName, Set<String> fragmentNames) {
        typeFragments.put(typeName, fragmentNames);
    }

    private void addFieldFragment(Field field, String fragmentName) {
        String fieldName = field.getName().toString();
        fieldFragment.put(fieldName, fragmentName);
    }

    private void addDocTypePrefetchedFragments(String docTypeName, Set<String> fragmentNames) {
        Set<String> fragments = docTypePrefetchedFragments.get(docTypeName);
        if (fragments == null) {
            docTypePrefetchedFragments.put(docTypeName, fragments = new HashSet<String>());
        }
        fragments.addAll(fragmentNames);
    }

    /**
     * Gets the fragments for a type (doctype or complex type).
     */
    private Set<String> getTypeFragments(String docTypeName) {
        return typeFragments.get(docTypeName);
    }

    /**
     * Gets the fragments for a mixin.
     */
    private Set<String> getMixinFragments(String mixin) {
        return mixinFragments.get(mixin);
    }

    public Set<String> getTypePrefetchedFragments(String typeName) {
        return docTypePrefetchedFragments.get(typeName);
    }

    /**
     * Checks if we have a type (doctype or complex type).
     */
    public boolean isType(String typeName) {
        return typeFragments.containsKey(typeName);
    }

    public String getDocumentSuperType(String typeName) {
        return documentSuperTypes.get(typeName);
    }

    public Set<String> getDocumentSubTypes(String typeName) {
        return documentSubTypes.get(typeName);
    }

    public Set<String> getDocumentTypes() {
        return documentTypesMixins.keySet();
    }

    public Set<String> getDocumentTypeFacets(String typeName) {
        Set<String> facets = documentTypesMixins.get(typeName);
        return facets == null ? Collections.<String> emptySet() : facets;
    }

    public Set<String> getMixinDocumentTypes(String mixin) {
        Set<String> types = mixinsDocumentTypes.get(mixin);
        return types == null ? Collections.<String> emptySet() : types;
    }

    /**
     * Given a map of id to types, returns a map of fragment names to ids.
     */
    public Map<String, Set<Serializable>> getPerFragmentIds(Map<Serializable, IdWithTypes> idToTypes) {
        Map<String, Set<Serializable>> allFragmentIds = new HashMap<String, Set<Serializable>>();
        for (Entry<Serializable, IdWithTypes> e : idToTypes.entrySet()) {
            Serializable id = e.getKey();
            IdWithTypes typeInfo = e.getValue();
            for (String fragmentName : getTypeFragments(typeInfo)) {
                Set<Serializable> fragmentIds = allFragmentIds.get(fragmentName);
                if (fragmentIds == null) {
                    allFragmentIds.put(fragmentName, fragmentIds = new HashSet<Serializable>());
                }
                fragmentIds.add(id);
            }
        }
        return allFragmentIds;
    }

    /**
     * Gets the type fragments for a primary type and mixin types. Hierarchy is included.
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
        if (PROXY_TYPE.equals(typeInfo.primaryType)) {
            fragmentNames.addAll(proxyFragments);
        }
        return fragmentNames;
    }

    public Set<String> getNoPerDocumentQueryFacets() {
        return noPerDocumentQueryFacets;
    }

    /**
     * Creates all the models.
     */
    private void initModels() {
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        log.debug("Schemas fields from descriptor: " + repositoryDescriptor.schemaFields);
        // document types
        for (DocumentType docType : schemaManager.getDocumentTypes()) {
            initDocTypeOrMixinModel(docType.getName(), docType.getSchemas(), allDocTypeSchemas, typeFragments,
                    typePropertyInfos, typeComplexChildren, true);
            initDocTypePrefetch(docType);
            initDocTypeMixins(docType);
            inferSuperType(docType);
        }
        // mixins
        for (CompositeType type : schemaManager.getFacets()) {
            initDocTypeOrMixinModel(type.getName(), type.getSchemas(), allMixinSchemas, mixinFragments,
                    mixinPropertyInfos, mixinComplexChildren, false);
            log.debug("Fragments for facet " + type.getName() + ": " + getMixinFragments(type.getName()));
        }
        // proxy schemas
        initProxySchemas(schemaManager.getProxySchemas(null));
        // second pass to get subtypes (needs all supertypes)
        for (DocumentType documentType : schemaManager.getDocumentTypes()) {
            inferSubTypes(documentType);
        }
        // init no per instance query facets
        initNoPerDocumentQueryFacets(schemaManager);
    }

    private void initProxySchemas(List<Schema> proxySchemas) {
        Map<String, Set<String>> allSchemas = new HashMap<String, Set<String>>();
        Map<String, Set<String>> allFragments = new HashMap<String, Set<String>>();
        Map<String, Map<String, String>> allChildren = new HashMap<String, Map<String, String>>();
        Map<String, Map<String, ModelProperty>> allPropertyInfos = new HashMap<String, Map<String, ModelProperty>>();
        String key = "__proxies__"; // not stored
        initDocTypeOrMixinModel(key, proxySchemas, allSchemas, allFragments, allPropertyInfos, allChildren, false);
        allProxySchemas.addAll(allSchemas.get(key));
        proxyFragments.addAll(allFragments.get(key));
        proxyPropertyInfos.putAll(allPropertyInfos.get(key));
        typeComplexChildren.put(PROXY_TYPE, allChildren.get(key));
    }

    private Set<String> getCommonFragments(String typeName) {
        Set<String> fragments = new HashSet<String>(5);
        fragments.add(VERSION_TABLE_NAME);
        fragments.add(ACL_TABLE_NAME);
        if (!miscInHierarchy) {
            fragments.add(MISC_TABLE_NAME);
        }
        if (fulltextConfiguration != null && fulltextConfiguration.isFulltextIndexable(typeName)) {
            fragments.add(FULLTEXT_TABLE_NAME);
        }
        return fragments;
    }

    private Set<String> getCommonFragmentsPrefetched() {
        Set<String> fragments = new HashSet<String>(5);
        fragments.add(VERSION_TABLE_NAME);
        fragments.add(ACL_TABLE_NAME);
        if (!miscInHierarchy) {
            fragments.add(MISC_TABLE_NAME);
        }
        return fragments;
    }

    /**
     * For a doctype or mixin type, init the schemas-related structures.
     */
    private void initDocTypeOrMixinModel(String typeName, Collection<Schema> schemas,
            Map<String, Set<String>> schemasMap, Map<String, Set<String>> fragmentsMap,
            Map<String, Map<String, ModelProperty>> propertyInfoMap,
            Map<String, Map<String, String>> complexChildrenMap, boolean addCommonFragments) {
        Set<String> schemaNames = new HashSet<String>();
        Set<String> fragmentNames = new HashSet<String>();
        Map<String, String> complexChildren = new HashMap<String, String>();
        if (addCommonFragments) {
            fragmentNames.addAll(getCommonFragments(typeName));
        }
        for (Schema schema : schemas) {
            if (schema == null) {
                // happens when a type refers to a nonexistent schema
                // TODO log and avoid nulls earlier
                continue;
            }
            schemaNames.add(schema.getName());
            try {
                fragmentNames.addAll(initSchemaModel(schema));
            } catch (NuxeoException e) {
                e.addInfo(String.format("Error initializing schema '%s' for composite type '%s'", schema.getName(),
                        typeName));
                throw e;
            }
            inferSchemaPropertyPaths(schema);
            complexChildren.putAll(schemaComplexChildren.get(schema.getName()));
        }
        schemasMap.put(typeName, schemaNames);
        fragmentsMap.put(typeName, fragmentNames);
        complexChildrenMap.put(typeName, complexChildren);
        inferPropertyInfos(propertyInfoMap, typeName, schemaNames);
    }

    private void initDocTypePrefetch(DocumentType docType) {
        String docTypeName = docType.getName();
        PrefetchInfo prefetch = docType.getPrefetchInfo();
        if (prefetch != null) {
            Set<String> documentTypeFragments = getTypeFragments(docTypeName);
            for (String fieldName : prefetch.getFields()) {
                // prefetch all the relevant fragments
                // TODO deal with full xpath
                String fragment = fieldFragment.get(fieldName);
                if (fragment != null) {
                    // checks that the field actually belongs
                    // to the type
                    if (documentTypeFragments.contains(fragment)) {
                        addDocTypePrefetchedFragments(docTypeName, Collections.singleton(fragment));
                    }
                }
            }
            for (String schemaName : prefetch.getSchemas()) {
                Set<String> fragments = schemaFragments.get(schemaName);
                if (fragments != null) {
                    addDocTypePrefetchedFragments(docTypeName, fragments);
                }
            }
        }
        // always prefetch ACLs, versions, misc (for lifecycle)
        addDocTypePrefetchedFragments(docTypeName, getCommonFragmentsPrefetched());

        log.debug("Fragments for type " + docTypeName + ": " + getTypeFragments(docTypeName) + ", prefetch: "
                + getTypePrefetchedFragments(docTypeName));
    }

    private void initDocTypeMixins(DocumentType docType) {
        String docTypeName = docType.getName();
        Set<String> mixins = docType.getFacets();
        documentTypesMixins.put(docTypeName, new HashSet<String>(mixins));
        for (String mixin : mixins) {
            Set<String> mixinTypes = mixinsDocumentTypes.get(mixin);
            if (mixinTypes == null) {
                mixinsDocumentTypes.put(mixin, mixinTypes = new HashSet<String>());
            }
            mixinTypes.add(docTypeName);
        }
    }

    private void inferSuperType(DocumentType docType) {
        Type superType = docType.getSuperType();
        if (superType != null) {
            documentSuperTypes.put(docType.getName(), superType.getName());
        }
    }

    private void inferSubTypes(DocumentType docType) {
        String type = docType.getName();
        String superType = type;
        do {
            Set<String> subTypes = documentSubTypes.get(superType);
            if (subTypes == null) {
                documentSubTypes.put(superType, subTypes = new HashSet<String>());
            }
            subTypes.add(type);
            superType = documentSuperTypes.get(superType);
        } while (superType != null);
    }

    private void initNoPerDocumentQueryFacets(SchemaManager schemaManager) {
        noPerDocumentQueryFacets.addAll(schemaManager.getNoPerDocumentQueryFacets());
    }

    /**
     * Special model for the main table (the one containing the primary type information).
     * <p>
     * If the main table is not separate from the hierarchy table, then it's will not really be instantiated by itself
     * but merged into the hierarchy table.
     */
    private void initMainModel() {
        addPropertyInfo(MAIN_PRIMARY_TYPE_PROP, PropertyType.STRING, HIER_TABLE_NAME, MAIN_PRIMARY_TYPE_KEY, true, null,
                ColumnType.SYSNAME);
        addPropertyInfo(MAIN_MIXIN_TYPES_PROP, PropertyType.STRING, HIER_TABLE_NAME, MAIN_MIXIN_TYPES_KEY, false, null,
                ColumnType.SYSNAMEARRAY);
        addPropertyInfo(MAIN_CHECKED_IN_PROP, PropertyType.BOOLEAN, HIER_TABLE_NAME, MAIN_CHECKED_IN_KEY, false,
                BooleanType.INSTANCE, ColumnType.BOOLEAN);
        addPropertyInfo(MAIN_BASE_VERSION_PROP, idPropertyType, HIER_TABLE_NAME, MAIN_BASE_VERSION_KEY, false,
                idCoreType, ColumnType.NODEVAL);
        addPropertyInfo(MAIN_MAJOR_VERSION_PROP, PropertyType.LONG, HIER_TABLE_NAME, MAIN_MAJOR_VERSION_KEY, false,
                LongType.INSTANCE, ColumnType.INTEGER);
        addPropertyInfo(MAIN_MINOR_VERSION_PROP, PropertyType.LONG, HIER_TABLE_NAME, MAIN_MINOR_VERSION_KEY, false,
                LongType.INSTANCE, ColumnType.INTEGER);
        addPropertyInfo(MAIN_IS_VERSION_PROP, PropertyType.BOOLEAN, HIER_TABLE_NAME, MAIN_IS_VERSION_KEY, false,
                BooleanType.INSTANCE, ColumnType.BOOLEAN);
        if (changeTokenEnabled) {
            addPropertyInfo(MAIN_SYS_VERSION_PROP, PropertyType.LONG, HIER_TABLE_NAME, MAIN_SYS_VERSION_KEY, false,
                    LongType.INSTANCE, ColumnType.LONG);
            addPropertyInfo(MAIN_CHANGE_TOKEN_PROP, PropertyType.LONG, HIER_TABLE_NAME, MAIN_CHANGE_TOKEN_KEY, false,
                    LongType.INSTANCE, ColumnType.LONG);
        }
        if (softDeleteEnabled) {
            addPropertyInfo(MAIN_IS_DELETED_PROP, PropertyType.BOOLEAN, HIER_TABLE_NAME, MAIN_IS_DELETED_KEY, true,
                    BooleanType.INSTANCE, ColumnType.BOOLEAN);
            addPropertyInfo(MAIN_DELETED_TIME_PROP, PropertyType.DATETIME, HIER_TABLE_NAME, MAIN_DELETED_TIME_KEY, true,
                    DateType.INSTANCE, ColumnType.TIMESTAMP);
        }
    }

    /**
     * Special model for the "misc" table (lifecycle, dirty.).
     */
    private void initMiscModel() {
        String fragmentName = miscInHierarchy ? HIER_TABLE_NAME : MISC_TABLE_NAME;
        addPropertyInfo(MISC_LIFECYCLE_POLICY_PROP, PropertyType.STRING, fragmentName, MISC_LIFECYCLE_POLICY_KEY, false,
                StringType.INSTANCE, ColumnType.SYSNAME);
        addPropertyInfo(MISC_LIFECYCLE_STATE_PROP, PropertyType.STRING, fragmentName, MISC_LIFECYCLE_STATE_KEY, false,
                StringType.INSTANCE, ColumnType.SYSNAME);
    }

    /**
     * Special model for the versions table.
     */
    private void initVersionsModel() {
        addPropertyInfo(VERSION_VERSIONABLE_PROP, idPropertyType, VERSION_TABLE_NAME, VERSION_VERSIONABLE_KEY, false,
                idCoreType, ColumnType.NODEVAL);
        addPropertyInfo(VERSION_CREATED_PROP, PropertyType.DATETIME, VERSION_TABLE_NAME, VERSION_CREATED_KEY, false,
                DateType.INSTANCE, ColumnType.TIMESTAMP);
        addPropertyInfo(VERSION_LABEL_PROP, PropertyType.STRING, VERSION_TABLE_NAME, VERSION_LABEL_KEY, false,
                StringType.INSTANCE, ColumnType.SYSNAME);
        addPropertyInfo(VERSION_DESCRIPTION_PROP, PropertyType.STRING, VERSION_TABLE_NAME, VERSION_DESCRIPTION_KEY,
                false, StringType.INSTANCE, ColumnType.STRING);
        addPropertyInfo(VERSION_IS_LATEST_PROP, PropertyType.BOOLEAN, VERSION_TABLE_NAME, VERSION_IS_LATEST_KEY, false,
                BooleanType.INSTANCE, ColumnType.BOOLEAN);
        addPropertyInfo(VERSION_IS_LATEST_MAJOR_PROP, PropertyType.BOOLEAN, VERSION_TABLE_NAME,
                VERSION_IS_LATEST_MAJOR_KEY, false, BooleanType.INSTANCE, ColumnType.BOOLEAN);
    }

    /**
     * Special model for the proxies table.
     */
    private void initProxiesModel() {
        String type = PROXY_TYPE;
        addPropertyInfo(type, PROXY_TARGET_PROP, idPropertyType, PROXY_TABLE_NAME, PROXY_TARGET_KEY, false, idCoreType,
                ColumnType.NODEIDFKNP);
        addPropertyInfo(type, PROXY_VERSIONABLE_PROP, idPropertyType, PROXY_TABLE_NAME, PROXY_VERSIONABLE_KEY, false,
                idCoreType, ColumnType.NODEVAL);
        addTypeFragments(type, Collections.singleton(PROXY_TABLE_NAME));
    }

    /**
     * Special model for the locks table (also, primary key has no foreign key, see {@link SQLInfo#initFragmentSQL}.
     */
    private void initLocksModel() {
        addPropertyInfo(LOCK_OWNER_PROP, PropertyType.STRING, LOCK_TABLE_NAME, LOCK_OWNER_KEY, false,
                StringType.INSTANCE, ColumnType.SYSNAME);
        addPropertyInfo(LOCK_CREATED_PROP, PropertyType.DATETIME, LOCK_TABLE_NAME, LOCK_CREATED_KEY, false,
                DateType.INSTANCE, ColumnType.TIMESTAMP);
    }

    /**
     * Special model for the fulltext table.
     */
    private void initFullTextModel() {
        addPropertyInfo(FULLTEXT_JOBID_PROP, PropertyType.STRING, FULLTEXT_TABLE_NAME, FULLTEXT_JOBID_KEY, false,
                StringType.INSTANCE, ColumnType.SYSNAME);
        for (String indexName : fulltextConfiguration.indexNames) {
            String suffix = getFulltextIndexSuffix(indexName);
            if (materializeFulltextSyntheticColumn) {
                addPropertyInfo(FULLTEXT_FULLTEXT_PROP + suffix, PropertyType.STRING, FULLTEXT_TABLE_NAME,
                        FULLTEXT_FULLTEXT_KEY + suffix, false, StringType.INSTANCE, ColumnType.FTINDEXED);
            }
            addPropertyInfo(FULLTEXT_SIMPLETEXT_PROP + suffix, PropertyType.STRING, FULLTEXT_TABLE_NAME,
                    FULLTEXT_SIMPLETEXT_KEY + suffix, false, StringType.INSTANCE, ColumnType.FTSTORED);
            addPropertyInfo(FULLTEXT_BINARYTEXT_PROP + suffix, PropertyType.STRING, FULLTEXT_TABLE_NAME,
                    FULLTEXT_BINARYTEXT_KEY + suffix, false, StringType.INSTANCE, ColumnType.FTSTORED);
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
        keysType.put(ACL_CREATOR_KEY, ColumnType.SYSNAME);
        keysType.put(ACL_BEGIN_KEY, ColumnType.TIMESTAMP);
        keysType.put(ACL_END_KEY, ColumnType.TIMESTAMP);
        keysType.put(ACL_STATUS_KEY, ColumnType.LONG);
        keysType.put(ACL_USER_KEY, ColumnType.SYSNAME);
        keysType.put(ACL_GROUP_KEY, ColumnType.SYSNAME);
        String fragmentName = ACL_TABLE_NAME;
        addCollectionFragmentInfos(fragmentName, PropertyType.COLL_ACL, ACL_POS_KEY, keysType);
        addPropertyInfo(ACL_PROP, PropertyType.COLL_ACL, fragmentName, null, false, null, null);
        // for query
        // composed of NXQL.ECM_ACL and NXQL.ECM_ACL_PRINCIPAL etc.
        allPathPropertyInfos.put("ecm:acl.principal/*",
                new ModelProperty(PropertyType.STRING, fragmentName, ACL_USER_KEY, true));
        allPathPropertyInfos.put("ecm:acl.permission/*",
                new ModelProperty(PropertyType.STRING, fragmentName, ACL_PERMISSION_KEY, true));
        allPathPropertyInfos.put("ecm:acl.grant/*",
                new ModelProperty(PropertyType.BOOLEAN, fragmentName, ACL_GRANT_KEY, true));
        allPathPropertyInfos.put("ecm:acl.name/*",
                new ModelProperty(PropertyType.STRING, fragmentName, ACL_NAME_KEY, true));
        allPathPropertyInfos.put("ecm:acl.pos/*",
                new ModelProperty(PropertyType.LONG, fragmentName, ACL_POS_KEY, true));
        allPathPropertyInfos.put("ecm:acl.creator/*",
                new ModelProperty(PropertyType.STRING, fragmentName, ACL_CREATOR_KEY, true));
        allPathPropertyInfos.put("ecm:acl.begin/*",
                new ModelProperty(PropertyType.DATETIME, fragmentName, ACL_BEGIN_KEY, true));
        allPathPropertyInfos.put("ecm:acl.end/*",
                new ModelProperty(PropertyType.DATETIME, fragmentName, ACL_END_KEY, true));
        allPathPropertyInfos.put("ecm:acl.status/*",
                new ModelProperty(PropertyType.LONG, fragmentName, ACL_STATUS_KEY, true));
    }

    /**
     * Creates the model for a schema.
     */
    private Set<String> initSchemaModel(Schema schema) {
        initComplexTypeModel(schema);
        return schemaFragments.get(schema.getName());
    }

    /**
     * Creates the model for a complex type or a schema. Recurses in complex types.
     * <p>
     * Adds the simple+collection fragments to {@link #typeFragments} or {@link #schemaFragments}.
     */
    private void initComplexTypeModel(ComplexType complexType) {
        String typeName = complexType.getName();
        boolean isSchema = complexType instanceof Schema;
        if (isSchema && schemaFragments.containsKey(typeName)) {
            return;
        } else if (!isSchema && typeFragments.containsKey(typeName)) {
            return;
        }
        /** The fragment names to use for this type, usually just one. */
        Set<String> fragmentNames = new HashSet<String>(1);
        /** The children complex properties for this type. */
        Map<String, String> complexChildren = new HashMap<String, String>(1);

        log.debug("Making model for type " + typeName);

        /** Initialized if this type has a table associated. */
        for (Field field : complexType.getFields()) {
            Type fieldType = field.getType();
            if (fieldType.isComplexType()) {
                /*
                 * Complex type.
                 */
                String propertyName = field.getName().getPrefixedName();
                complexChildren.put(propertyName, fieldType.getName());
                initComplexTypeModel((ComplexType) fieldType);
            } else {
                String propertyName = field.getName().getPrefixedName();
                FieldDescriptor fieldDescriptor = null;
                for (FieldDescriptor fd : repositoryDescriptor.schemaFields) {
                    if (propertyName.equals(fd.field)) {
                        fieldDescriptor = fd;
                        break;
                    }
                }
                if (fieldType.isListType()) {
                    Type listFieldType = ((ListType) fieldType).getFieldType();
                    if (listFieldType.isSimpleType()) {
                        /*
                         * Simple list.
                         */
                        PropertyType propertyType = PropertyType.fromFieldType(listFieldType, true);
                        boolean useArray = false;
                        ColumnType columnType = null;
                        if (repositoryDescriptor.getArrayColumns() && fieldDescriptor == null) {
                            fieldDescriptor = new FieldDescriptor();
                            fieldDescriptor.type = FIELD_TYPE_ARRAY;
                        }
                        if (fieldDescriptor != null) {
                            if (FIELD_TYPE_ARRAY.equals(fieldDescriptor.type)) {
                                if (!supportsArrayColumns) {
                                    log.warn("  Field '" + propertyName + "' array specification is ignored since"
                                            + " this database does not support arrays");
                                }
                                useArray = supportsArrayColumns;
                                columnType = ColumnType.fromFieldType(listFieldType, useArray);
                            } else if (FIELD_TYPE_ARRAY_LARGETEXT.equals(fieldDescriptor.type)) {
                                boolean isStringColSpec = ColumnType.fromFieldType(
                                        listFieldType).spec == ColumnSpec.STRING;
                                if (supportsArrayColumns && !isStringColSpec) {
                                    log.warn("  Field '" + propertyName + "' is not a String yet it is specified"
                                            + " as array_largetext, using ARRAY_CLOB for it");
                                } else if (!supportsArrayColumns && isStringColSpec) {
                                    log.warn("  Field '" + propertyName + "' array specification is ignored since"
                                            + " this database does not support arrays," + " using CLOB for it");
                                } else if (!supportsArrayColumns && !isStringColSpec) {
                                    log.warn("  Field '" + propertyName + "' array specification is ignored since"
                                            + " this database does not support arrays, also"
                                            + " Field is not a String yet it is specified"
                                            + " as array_largetext, using CLOB for it");
                                }
                                useArray = supportsArrayColumns;
                                columnType = (supportsArrayColumns) ? ColumnType.ARRAY_CLOB : ColumnType.CLOB;
                            } else if (FIELD_TYPE_LARGETEXT.equals(fieldDescriptor.type)) {
                                if (ColumnType.fromFieldType(listFieldType).spec != ColumnSpec.STRING) {
                                    log.warn("  Field '" + propertyName + "' is not a String yet it is specified "
                                            + " as largetext, using CLOB for it");
                                }
                                columnType = ColumnType.CLOB;
                            } else {
                                log.warn("  Field '" + propertyName + "' specified but not successfully mapped");
                            }
                        }

                        if (columnType == null) {
                            columnType = ColumnType.fromFieldType(listFieldType);
                        }
                        log.debug("  List field '" + propertyName + "' using column type " + columnType);

                        if (useArray) {
                            /*
                             * Array: use an array.
                             */
                            String fragmentName = typeFragmentName(complexType);
                            String fragmentKey = field.getName().getLocalName();
                            addPropertyInfo(complexType, propertyName, propertyType, fragmentName, fragmentKey, false,
                                    null, columnType);
                            addFieldFragment(field, fragmentName);
                        } else {
                            /*
                             * Array: use a collection table.
                             */
                            String fragmentName = collectionFragmentName(propertyName);
                            addPropertyInfo(complexType, propertyName, propertyType, fragmentName, COLL_TABLE_VALUE_KEY,
                                    false, null, columnType);
                            // pseudo-syntax with ending "#" to get to the pos column
                            String posPropertyName = propertyName + "#";
                            PropertyType posPropertyType = PropertyType.LONG;
                            addPropertyInfo(complexType, posPropertyName, posPropertyType, fragmentName,
                                    COLL_TABLE_POS_KEY, false, null, ColumnType.INTEGER);

                            Map<String, ColumnType> keysType = new LinkedHashMap<String, ColumnType>();
                            keysType.put(COLL_TABLE_POS_KEY, ColumnType.INTEGER);
                            keysType.put(COLL_TABLE_VALUE_KEY, columnType);
                            addCollectionFragmentInfos(fragmentName, propertyType, COLL_TABLE_POS_KEY, keysType);

                            fragmentNames.add(fragmentName);
                            addFieldFragment(field, fragmentName);
                        }
                    } else {
                        /*
                         * Complex list.
                         */
                        initComplexTypeModel((ComplexType) listFieldType);
                    }
                } else {
                    /*
                     * Primitive type.
                     */
                    String fragmentName = typeFragmentName(complexType);
                    String fragmentKey = field.getName().getLocalName();
                    PropertyType propertyType = PropertyType.fromFieldType(fieldType, false);
                    ColumnType type = ColumnType.fromField(field);
                    if (type.spec == ColumnSpec.STRING) {
                        // backward compat with largetext, since 5.4.2
                        if (fieldDescriptor != null && FIELD_TYPE_LARGETEXT.equals(fieldDescriptor.type)) {
                            if (!type.isUnconstrained() && !type.isClob()) {
                                log.warn("  String field '" + propertyName + "' has a schema constraint to " + type
                                        + " but is specified as largetext," + " using CLOB for it");
                            }
                            type = ColumnType.CLOB;
                        }
                    }
                    if (fieldDescriptor != null) {
                        if (fieldDescriptor.table != null) {
                            fragmentName = fieldDescriptor.table;
                        }
                        if (fieldDescriptor.column != null) {
                            fragmentKey = fieldDescriptor.column;
                        }
                    }
                    if (MAIN_KEY.equalsIgnoreCase(fragmentKey)) {
                        String msg = "A property cannot be named '" + fragmentKey
                                + "' because this is a reserved name, in type: " + typeName;
                        throw new NuxeoException(msg);
                    }
                    if (fragmentName.equals(UID_SCHEMA_NAME) && (fragmentKey.equals(UID_MAJOR_VERSION_KEY)
                            || fragmentKey.equals(UID_MINOR_VERSION_KEY))) {
                        // workaround: special-case the "uid" schema, put
                        // major/minor in the hierarchy table
                        fragmentName = HIER_TABLE_NAME;
                        fragmentKey = fragmentKey.equals(UID_MAJOR_VERSION_KEY) ? MAIN_MAJOR_VERSION_KEY
                                : MAIN_MINOR_VERSION_KEY;
                    }
                    addPropertyInfo(complexType, propertyName, propertyType, fragmentName, fragmentKey, false, null,
                            type);
                    if (!fragmentName.equals(HIER_TABLE_NAME)) {
                        fragmentNames.add(fragmentName);
                        addFieldFragment(field, fragmentName);
                    }
                }
            }
        }

        if (isSchema) {
            schemaFragments.put(typeName, fragmentNames);
            schemaComplexChildren.put(typeName, complexChildren);
        } else {
            addTypeFragments(typeName, fragmentNames);
            typeComplexChildren.put(typeName, complexChildren);
        }
    }

    private static String typeFragmentName(ComplexType type) {
        return type.getName();
    }

    private static String collectionFragmentName(String propertyName) {
        return propertyName;
    }
}
