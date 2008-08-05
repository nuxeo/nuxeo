/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeRef;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.FieldImpl;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.storage.sql.CollectionFragment.CollectionMaker;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.IdGenPolicy;
import org.nuxeo.ecm.core.storage.sql.db.Column;

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

    public static final String ROOT_TYPE = "Root";

    public static final String REPOINFO_TABLE_NAME = "repositoryinfo";

    public static final String REPOINFO_REPOID_KEY = "repoid";

    public static final String MAIN_KEY = "id";

    public final String hierFragmentName;

    public final String mainFragmentName;

    public static final String MAIN_PRIMARY_TYPE_PROP = "ecm:primaryType";

    private static final String MAIN_TABLE_NAME = "types";

    public static final String MAIN_PRIMARY_TYPE_KEY = "primarytype";

    private static final String HIER_TABLE_NAME = "hierarchy";

    public static final String HIER_PARENT_KEY = "parent";

    public static final String HIER_CHILD_NAME_KEY = "name";

    public static final String HIER_CHILD_POS_KEY = "pos";

    public static final String HIER_CHILD_ISPROPERTY_KEY = "isproperty";

    public static final String COLL_TABLE_POS_KEY = "pos";

    public static final String COLL_TABLE_VALUE_KEY = "item";

    public static final String SYSTEM_TABLE_NAME = "system";

    public static final String SYSTEM_LIFECYCLE_POLICY_PROP = "ecm:lifeCyclePolicy";

    public static final String SYSTEM_LIFECYCLE_POLICY_KEY = "lifecyclepolicy";

    public static final String SYSTEM_LIFECYCLE_STATE_PROP = "ecm:lifeCycleState";

    public static final String SYSTEM_LIFECYCLE_STATE_KEY = "lifecyclestate";

    public static final String SYSTEM_DIRTY_PROP = "ecm:dirty";

    public static final String SYSTEM_DIRTY_KEY = "dirty";

    public static final String ACL_TABLE_NAME = "acls";

    public static final String ACL_PROP = "__acl__";

    public static final String ACL_ACLPOS_KEY = "aclpos";

    public static final String ACL_ACLNAME_KEY = "aclname";

    public static final String ACL_POS_KEY = "pos";

    public static final String ACL_GRANT_KEY = "grant";

    public static final String ACL_PERMISSION_KEY = "permission";

    public static final String ACL_USER_KEY = "user";

    public static final String ACL_GROUP_KEY = "group";

    private static TypeRef<? extends Type> STRING_TYPE_REF = StringType.INSTANCE.getRef();

    public static Field SYSTEM_LIFECYCLE_POLICY_FIELD = new FieldImpl(
            QName.valueOf(SYSTEM_LIFECYCLE_POLICY_PROP), TypeRef.NULL,
            STRING_TYPE_REF);

    public static Field SYSTEM_LIFECYCLE_STATE_FIELD = new FieldImpl(
            QName.valueOf(SYSTEM_LIFECYCLE_STATE_PROP), TypeRef.NULL,
            STRING_TYPE_REF);

    public static Field SYSTEM_DIRTY_FIELD = new FieldImpl(
            QName.valueOf(SYSTEM_DIRTY_PROP), TypeRef.NULL, STRING_TYPE_REF);

    private final BinaryManager binaryManager;

    /** The id generation policy. */
    protected final IdGenPolicy idGenPolicy;

    /** Is the hierarchy table separate from the main table. */
    protected final boolean separateHierarchyTable;

    protected final AtomicLong temporaryIdCounter;

    /**
     * The fragment for each schema, or {@code null} if the schema doesn't have
     * a fragment.
     */
    private final Map<String, String> schemaFragment;

    /** Maps table name to a map of properties to their basic type. */
    protected final Map<String, Map<String, PropertyType>> fragmentsKeysType;

    /** Column ordering for collections. */
    protected final Map<String, List<String>> collectionOrderBy;

    /** Maps collection table names to their type. */
    protected final Map<String, PropertyType> collectionTables;

    /** The factories to build collection fragments. */
    protected final Map<String, CollectionMaker> collectionMakers;

    /** Maps document type name or schema name to allowed simple fragments. */
    protected final Map<String, Set<String>> typeSimpleFragments;

    /** Maps schema name to allowed collection fragments. */
    protected final Map<String, Set<String>> typeCollectionFragments;

    /** Maps schema name to allowed simple+collection fragments. */
    protected final Map<String, Set<String>> typeFragments;

    /** Maps property name to fragment name. */
    private final Map<String, String> propertyFragment;

    /** Maps property name to fragment key (single-valued). */
    private final Map<String, String> propertyFragmentKey;

    /** Maps of properties to their basic type. */
    public final Map<String, PropertyType> propertyType;

    public final Set<String[]> binaryColumns;

    public Model(RepositoryImpl repository, SchemaManager schemaManager) {
        binaryManager = repository.getBinaryManager();
        RepositoryDescriptor repositoryDescriptor = repository.getRepositoryDescriptor();
        idGenPolicy = repositoryDescriptor.idGenPolicy;
        separateHierarchyTable = repositoryDescriptor.separateHierarchyTable;
        temporaryIdCounter = new AtomicLong(0);
        hierFragmentName = HIER_TABLE_NAME;
        mainFragmentName = separateHierarchyTable ? MAIN_TABLE_NAME
                : HIER_TABLE_NAME;

        schemaFragment = new HashMap<String, String>();
        propertyType = new HashMap<String, PropertyType>();
        fragmentsKeysType = new HashMap<String, Map<String, PropertyType>>();
        collectionOrderBy = new HashMap<String, List<String>>();
        collectionTables = new HashMap<String, PropertyType>();
        collectionMakers = new HashMap<String, CollectionMaker>();
        typeFragments = new HashMap<String, Set<String>>();
        typeSimpleFragments = new HashMap<String, Set<String>>();
        typeCollectionFragments = new HashMap<String, Set<String>>();
        propertyFragment = new HashMap<String, String>();
        propertyFragmentKey = new HashMap<String, String>();
        binaryColumns = new HashSet<String[]>();

        initMainModel();
        initSystemModel();
        initAclModel();
        initModels(schemaManager);

        List<String> bincols = new ArrayList<String>(binaryColumns.size());
        for (String[] info : binaryColumns) {
            bincols.add(info[0] + '.' + info[1]);
        }
        log.info("Binary columns: " + StringUtils.join(bincols, ", "));
    }

    /**
     * Gets a binary given its digest.
     *
     * @param digest the digest
     * @return the binary for this digest, or {@code null} if unavailable
     *         (error)
     */
    public Binary getBinary(String digest) {
        return binaryManager.getBinary(digest);
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
        switch (idGenPolicy) {
        case APP_UUID:
            return UUID.randomUUID().toString();
        case DB_IDENTITY:
            return "T" + temporaryIdCounter.incrementAndGet();
        default:
            throw new AssertionError(idGenPolicy);
        }
    }

    /**
     * Fixup an id that has been turned into a string for high-level Nuxeo APIs.
     *
     * @param id the id to fixup
     * @return the fixed up id
     */
    public Serializable unHackStringId(String id) {
        switch (idGenPolicy) {
        case APP_UUID:
            return id;
        case DB_IDENTITY:
            if (id.startsWith("T")) {
                return id;
            }
            /*
             * Document ids coming from higher level have been turned into
             * strings (by SQLDocument.getUUID) but are really longs for the
             * backend.
             */
            return Long.valueOf(id);
        default:
            throw new AssertionError(idGenPolicy);
        }
    }

    // public Type getPropertyCoreType(String propertyName) {
    // return propertyCoreType.get(propertyName);
    // }

    public PropertyType getPropertyType(String propertyName) {
        return propertyType.get(propertyName);
    }

    public PropertyType getCollectionFragmentType(String tableName) {
        return collectionTables.get(tableName);
    }

    /**
     * Create a collection fragment according to the factories registered.
     */
    public Serializable[] newCollectionArray(Serializable id, ResultSet rs,
            List<Column> columns, Context context) throws SQLException {
        CollectionMaker maker = collectionMakers.get(context.getTableName());
        if (maker == null) {
            throw new IllegalArgumentException(context.getTableName());
        }
        return maker.makeArray(id, rs, columns, context, this);
    }

    public CollectionFragment newCollectionFragment(Serializable id,
            Serializable[] array, Context context) {
        CollectionMaker maker = collectionMakers.get(context.getTableName());
        if (maker == null) {
            throw new IllegalArgumentException(context.getTableName());
        }
        return maker.makeCollection(id, array, context);
    }

    public CollectionFragment newEmptyCollectionFragment(Serializable id,
            Context context) {
        CollectionMaker maker = collectionMakers.get(context.getTableName());
        if (maker == null) {
            throw new IllegalArgumentException(context.getTableName());
        }
        return maker.makeEmpty(id, context, this);
    }

    public String getFragmentName(String propertyName) {
        return propertyFragment.get(propertyName);
    }

    public String getFragmentKey(String propertyName) {
        return propertyFragmentKey.get(propertyName);
    }

    protected void addTypeSimpleFragment(String typeName, String fragmentName) {
        Set<String> fragments = typeSimpleFragments.get(typeName);
        if (fragments == null) {
            fragments = new HashSet<String>();
            typeSimpleFragments.put(typeName, fragments);
        }
        if (fragmentName != null) {
            fragments.add(fragmentName);
            addTypeFragment(typeName, fragmentName);
        }
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
            fragments = new HashSet<String>();
            typeFragments.put(typeName, fragments);
        }
        fragments.add(fragmentName);
    }

    public Set<String> getTypeSimpleFragments(String typeName) {
        return typeSimpleFragments.get(typeName);
    }

    public Set<String> getTypeCollectionFragments(String typeName) {
        return typeCollectionFragments.get(typeName);
    }

    public Set<String> getTypeFragments(String typeName) {
        return typeFragments.get(typeName);
    }

    public boolean isComplexType(String typeName) {
        return typeSimpleFragments.containsKey(typeName);
    }

    /**
     * Creates all the models.
     */
    private void initModels(SchemaManager schemaManager) {
        for (DocumentType documentType : schemaManager.getDocumentTypes()) {
            String typeName = documentType.getName();
            addTypeSimpleFragment(typeName, null); // create entry
            for (Schema schema : documentType.getSchemas()) {
                String fragmentName = initTypeModel(schema);
                addTypeSimpleFragment(typeName, fragmentName); // may be null
                // collection fragments too for this schema
                Set<String> cols = typeCollectionFragments.get(schema.getName());
                if (cols != null) {
                    for (String colFrag : cols) {
                        addTypeCollectionFragment(typeName, colFrag);
                    }
                }
            }
            // all documents have ACLs too
            addTypeCollectionFragment(typeName, ACL_TABLE_NAME);
            log.debug("Fragments for " + typeName + ": " +
                    getTypeFragments(typeName));
        }
    }

    /**
     * Special model for the main table (the one containing the primary type
     * information).
     */
    private void initMainModel() {
        if (!separateHierarchyTable) {
            return;
        }
        String tableName = MAIN_TABLE_NAME;
        String propertyName = MAIN_PRIMARY_TYPE_PROP;
        PropertyType type = PropertyType.STRING;
        // XXX propertyCoreType ?
        propertyType.put(propertyName, type);
        propertyFragment.put(propertyName, tableName);
        propertyFragmentKey.put(propertyName, MAIN_PRIMARY_TYPE_KEY);

        Map<String, PropertyType> fragmentKeysType = new HashMap<String, PropertyType>();
        fragmentsKeysType.put(tableName, fragmentKeysType);
        fragmentKeysType.put(MAIN_PRIMARY_TYPE_KEY, type);

    }

    /**
     * Special model for the system table (lifecycle, etc.).
     */
    private void initSystemModel() {
        String tableName = SYSTEM_TABLE_NAME;
        Map<String, PropertyType> fragmentKeysType = new LinkedHashMap<String, PropertyType>();
        fragmentsKeysType.put(tableName, fragmentKeysType);

        String propertyName = SYSTEM_LIFECYCLE_POLICY_PROP;
        String key = SYSTEM_LIFECYCLE_POLICY_KEY;
        PropertyType type = PropertyType.STRING;
        // XXX propertyCoreType needed ?
        propertyType.put(propertyName, type);
        propertyFragment.put(propertyName, tableName);
        propertyFragmentKey.put(propertyName, key);
        fragmentKeysType.put(key, type);

        propertyName = SYSTEM_LIFECYCLE_STATE_PROP;
        key = SYSTEM_LIFECYCLE_STATE_KEY;
        type = PropertyType.STRING;
        // XXX propertyCoreType needed ?
        propertyType.put(propertyName, type);
        propertyFragment.put(propertyName, tableName);
        propertyFragmentKey.put(propertyName, key);
        fragmentKeysType.put(key, type);

        propertyName = SYSTEM_DIRTY_PROP;
        key = SYSTEM_DIRTY_KEY;
        type = PropertyType.BOOLEAN;
        // XXX propertyCoreType needed ?
        propertyType.put(propertyName, type);
        propertyFragment.put(propertyName, tableName);
        propertyFragmentKey.put(propertyName, key);
        fragmentKeysType.put(key, type);
    }

    /**
     * Special collection-like model for the ACL table.
     */
    private void initAclModel() {
        Map<String, PropertyType> fragmentKeysType = new LinkedHashMap<String, PropertyType>();
        fragmentsKeysType.put(ACL_TABLE_NAME, fragmentKeysType);
        fragmentKeysType.put(ACL_ACLPOS_KEY, PropertyType.LONG);
        fragmentKeysType.put(ACL_ACLNAME_KEY, PropertyType.STRING);
        fragmentKeysType.put(ACL_POS_KEY, PropertyType.LONG);
        fragmentKeysType.put(ACL_GRANT_KEY, PropertyType.BOOLEAN);
        fragmentKeysType.put(ACL_PERMISSION_KEY, PropertyType.STRING);
        fragmentKeysType.put(ACL_USER_KEY, PropertyType.STRING);
        fragmentKeysType.put(ACL_GROUP_KEY, PropertyType.STRING);
        collectionTables.put(ACL_TABLE_NAME, PropertyType.COLL_ACL);
        collectionMakers.put(ACL_TABLE_NAME, ACLsFragment.MAKER);
        collectionOrderBy.put(ACL_TABLE_NAME, Arrays.asList(ACL_ACLPOS_KEY,
                ACL_POS_KEY));
        propertyFragment.put(ACL_PROP, ACL_TABLE_NAME);
        propertyType.put(ACL_PROP, PropertyType.COLL_ACL);
    }

    /**
     * Creates the model for one schema or complex type.
     *
     * @return the fragment table name for this type, or {@code null} if this
     *         type doesn't directly hold data
     */
    private String initTypeModel(ComplexType complexType) {
        String typeName = complexType.getName();
        if (schemaFragment.containsKey(typeName)) {
            return schemaFragment.get(typeName); // may be null
        }

        /** Initialized if this type has a table associated. */
        String fragmentName = null;

        log.debug("Making model for type " + typeName);

        for (Field field : complexType.getFields()) {
            Type fieldType = field.getType();
            if (fieldType.isComplexType()) {
                /*
                 * Complex type.
                 */
                ComplexType fieldComplexType = (ComplexType) fieldType;
                String subTypeName = fieldComplexType.getName();
                String subFragmentName = initTypeModel(fieldComplexType);
                if (subFragmentName != null) {
                    addTypeSimpleFragment(subTypeName, subFragmentName);
                }
            } else {
                String propertyName = field.getName().getPrefixedName();
                if (fieldType.isListType()) {
                    Type listFieldType = ((ListType) fieldType).getFieldType();
                    if (listFieldType.isSimpleType()) {
                        /*
                         * Array: use a collection table.
                         */
                        // propertyCoreType.put(propertyName, fieldType);
                        PropertyType type = PropertyType.fromFieldType(
                                listFieldType, true);
                        propertyType.put(propertyName, type);

                        String tableName = collectionFragmentName(propertyName);
                        propertyFragment.put(propertyName, tableName);
                        collectionTables.put(tableName, type);
                        collectionMakers.put(tableName, ArrayFragment.MAKER);
                        Map<String, PropertyType> fragmentKeysType = fragmentsKeysType.get(tableName);
                        if (fragmentKeysType == null) {
                            fragmentKeysType = new LinkedHashMap<String, PropertyType>();
                            fragmentsKeysType.put(tableName, fragmentKeysType);
                        }
                        fragmentKeysType.put(COLL_TABLE_POS_KEY,
                                PropertyType.LONG); // TODO INT
                        fragmentKeysType.put(COLL_TABLE_VALUE_KEY,
                                type.getArrayBaseType());
                        collectionOrderBy.put(tableName,
                                Collections.singletonList(COLL_TABLE_POS_KEY));
                        addTypeCollectionFragment(typeName, tableName);
                    } else {
                        /*
                         * Complex list.
                         */
                        // Set<String> listProperties =
                        // complexLists.get(typeName);
                        // if (listProperties == null) {
                        // listProperties = new HashSet<String>();
                        // complexLists.put(typeName, listProperties);
                        // }
                        // listProperties.add(propertyName);
                        initTypeModel((ComplexType) listFieldType);
                    }
                } else {
                    /*
                     * Primitive type.
                     */
                    // propertyCoreType.put(propertyName, fieldType);
                    PropertyType type = PropertyType.fromFieldType(fieldType,
                            false);
                    if (type == PropertyType.BINARY) {
                        // to log them, will be useful for GC of binaries
                        binaryColumns.add(new String[] { typeName, propertyName });
                    }
                    propertyType.put(propertyName, type);
                    fragmentName = typeFragmentName(complexType);
                    propertyFragment.put(propertyName, fragmentName);
                    String key = field.getName().getLocalName();
                    propertyFragmentKey.put(propertyName, key);

                    Map<String, PropertyType> fragmentKeysType = fragmentsKeysType.get(fragmentName);
                    if (fragmentKeysType == null) {
                        fragmentKeysType = new LinkedHashMap<String, PropertyType>();
                        fragmentsKeysType.put(fragmentName, fragmentKeysType);
                    }
                    fragmentKeysType.put(key, type);
                }
            }
        }

        schemaFragment.put(typeName, fragmentName); // may be null
        return fragmentName;
    }

    private String typeFragmentName(ComplexType type) {
        return type.getName();
    }

    private String collectionFragmentName(String propertyName) {
        return propertyName;
    }
}
