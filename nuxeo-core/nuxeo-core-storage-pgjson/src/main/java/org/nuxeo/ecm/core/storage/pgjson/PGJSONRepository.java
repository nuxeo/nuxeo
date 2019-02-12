/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.pgjson;

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACP;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ANCESTOR_IDS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_BASE_VERSION_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_CHANGE_TOKEN;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_BINARY;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_JOBID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_SIMPLE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_IS_CHECKED_IN;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_IS_LATEST_MAJOR_VERSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_IS_LATEST_VERSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_IS_PROXY;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_IS_RETENTION_ACTIVE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_IS_TRASHED;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_IS_VERSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_LIFECYCLE_POLICY;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_LIFECYCLE_STATE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_LOCK_CREATED;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_LOCK_OWNER;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_MAJOR_VERSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_MINOR_VERSION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_MIXIN_TYPES;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_POS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PRIMARY_TYPE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_IDS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_TARGET_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_VERSION_SERIES_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_READ_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_SYS_CHANGE_TOKEN;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_VERSION_CREATED;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_VERSION_DESCRIPTION;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_VERSION_LABEL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_VERSION_SERIES_ID;
import static org.nuxeo.ecm.core.storage.pgjson.PGType.TYPE_BOOLEAN;
import static org.nuxeo.ecm.core.storage.pgjson.PGType.TYPE_JSON;
import static org.nuxeo.ecm.core.storage.pgjson.PGType.TYPE_LONG;
import static org.nuxeo.ecm.core.storage.pgjson.PGType.TYPE_LONG_ARRAY;
import static org.nuxeo.ecm.core.storage.pgjson.PGType.TYPE_STRING;
import static org.nuxeo.ecm.core.storage.pgjson.PGType.TYPE_STRING_ARRAY;
import static org.nuxeo.ecm.core.storage.pgjson.PGType.TYPE_TIMESTAMP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.resource.spi.ConnectionManager;

import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryBase;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * PostgreSQL+JSON implementation of a {@link Repository}.
 *
 * @since 11.1
 */
public class PGJSONRepository extends DBSRepositoryBase {

    protected static final int BATCH_SIZE = 100;

    public static final String TABLE_NAME = "documents";

    protected static final String COL_ID = "id";

    protected static final String COL_PARENT_ID = "parentid";

    protected static final String COL_ANCESTOR_IDS = "ancestorids";

    protected static final String COL_NAME = "name";

    protected static final String COL_POS = "pos";

    protected static final String COL_PRIMARY_TYPE = "primarytype";

    protected static final String COL_MIXIN_TYPES = "mixintypes";

    protected static final String COL_IS_PROXY = "isproxy";

    protected static final String COL_IS_CHECKED_IN = "ischeckedin";

    protected static final String COL_IS_VERSION = "isversion";

    protected static final String COL_IS_LATEST_VERSION = "islatestversion";

    protected static final String COL_IS_LATEST_MAJOR_VERSION = "islatestmajorversion";

    protected static final String COL_IS_TRASHED = "istrashed";

    protected static final String COL_IS_RETENTION_ACTIVE = "isretentionactive";

    protected static final String COL_BASE_VERSION_ID = "baseversionid";

    protected static final String COL_MAJOR_VERSION = "majorversion";

    protected static final String COL_MINOR_VERSION = "minorversion";

    protected static final String COL_VERSION_SERIES_ID = "versionseriesid";

    protected static final String COL_VERSION_CREATED = "versioncreated";

    protected static final String COL_VERSION_LABEL = "versionlabel";

    protected static final String COL_VERSION_DESCRIPTION = "versiondescription";

    protected static final String COL_ACP = "acp";

    protected static final String COL_READ_ACL = "racl";

    protected static final String COL_LOCK_OWNER = "lockowner";

    protected static final String COL_LOCK_CREATED = "lockcreated";

    protected static final String COL_PROXY_TARGET_ID = "proxytargetid";

    protected static final String COL_PROXY_VERSION_SERIES_ID = "proxyversionseriesid";

    protected static final String COL_PROXY_IDS = "proxyids";

    protected static final String COL_LIFECYCLE_POLICY = "lifecyclepolicy";

    protected static final String COL_LIFECYCLE_STATE = "lifecyclestate";

    protected static final String COL_SYS_CHANGE_TOKEN = "systemchangetoken";

    protected static final String COL_CHANGE_TOKEN = "changetoken";

    protected static final String COL_FULLTEXT_SIMPLE = "fulltextsimple";

    protected static final String COL_FULLTEXT_BINARY = "fulltextbinary";

    protected static final String COL_FULLTEXT_JOBID = "fulltextjobid";

    protected static final String COL_JSON = "doc";

    protected static final String PSEUDO_KEY_JSON = "__json__"; // internal

    /** Nuxeo types of non-system properties. */
    protected final TypesMap typesMap;

    protected final PGJSONConverter converter;

    protected final List<PGColumn> allColumns = new ArrayList<>();

    protected final Map<String, PGColumn> keyToColumn = new HashMap<>();

    protected PGColumn idColumn;

    protected PGColumn jsonDocColumn;

    protected String dataSourceName;

    protected final AtomicLong debugUUIDCounter = new AtomicLong(1);

    public PGJSONRepository(ConnectionManager cm, PGJSONRepositoryDescriptor descriptor) {
        super(cm, descriptor.name, descriptor);
        typesMap = new TypesMapFinder().find();
        converter = new PGJSONConverter(typesMap);
        registerColumns();
        dataSourceName = getDataSourceName(descriptor.name);

        // initialize the repository
        try (PGJSONConnection connection = getConnection()) {
            connection.initRepository();
        }
    }

    @Override
    public boolean supportsTransactions() {
        return true;
    }

    @Override
    public PGJSONConnection getConnection() {
        return new PGJSONConnection(this);
    }

    protected String getDataSourceName(String repositoryName) {
        // use same convention as VCS for the datasource of a given repository
        return "repository_" + repositoryName;
    }

    protected String getDataSourceName() {
        return dataSourceName;
    }

    @Override
    public List<IdType> getAllowedIdTypes() {
        return Collections.singletonList(IdType.varchar);
    }

    /**
     * Recursive type to describe of tree of named {@link Type}s.
     */
    public static class TypesMap extends HashMap<String, TypesMap> {

        private static final long serialVersionUID = 1L;

        public static final String ARRAY_ELEM = "0";

        /** Canonical storage name. */
        public final String name;

        public final Type type;

        public TypesMap() {
            name = null;
            type = null;
        }

        public TypesMap(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        // convenience for tests
        public TypesMap(String thisName, Type thisType, String name, Type type) {
            this(thisName, thisType);
            put(name, type);
        }

        public TypesMap put(String name, Type type) {
            TypesMap typesMap = new TypesMap(name, type);
            put(name, typesMap);
            return typesMap;

        }
        /** Gets the type at this path, or {@code null} */
        public Type get(Collection<String> path) {
            return get(path.iterator());
        }

        protected Type get(Iterator<String> it) {
            if (!it.hasNext()) {
                return null;
            }
            String name = it.next();
            TypesMap map = get(name);
            if (map == null) {
                return null;
            } else if (!map.isEmpty()) {
                return map.get(it);
            } else if (!it.hasNext()) {
                return map.type;
            } else {
                return null;
            }
        }
    }

    /**
     * Finds all the Nuxeo types for all possible property paths. Array elements are under a pseudo key "0".
     * <p>
     * This is needed at read time because at the JSON level we can't distinguish between longs, floats and calendars
     * (milliseconds), which are all represented by JSON numbers.
     */
    protected static class TypesMapFinder {

        public TypesMap find() {
            TypesMap map = new TypesMap();
            SchemaManager schemaManager = Framework.getService(SchemaManager.class);
            for (Schema schema : schemaManager.getSchemas()) {
                visitComplexType(map, schema);
            }
            return map;
        }

        protected void visitComplexType(TypesMap map, ComplexType complexType) {
            for (Field field : complexType.getFields()) {
                String name = field.getName().getPrefixedName();
                visitField(map, name, field.getType());
                if (complexType instanceof Schema && name.indexOf(':') < 0) {
                    // add compatibility name with schema-as-prefix
                    String prefixedName = complexType.getName() + ':' + name;
                    map.put(prefixedName, map.get(name));
                }
            }
        }

        protected void visitField(TypesMap map, String name, Type type) {
            TypesMap subMap = map.put(name, type);
            if (type.isComplexType()) {
                visitComplexType(subMap, (ComplexType) type);
            } else if (type.isListType()) {
                visitField(subMap, TypesMap.ARRAY_ELEM, ((ListType) type).getFieldType());
            }
        }
    }

    protected void registerColumns() {
        PGType typeId;
        PGType typeIdArray;
        switch (idType) {
        case varchar:
            typeId = TYPE_STRING;
            typeIdArray = TYPE_STRING_ARRAY;
            break;
        case sequence:
            typeId = TYPE_LONG;
            typeIdArray = TYPE_LONG_ARRAY;
            break;
        default:
            throw new NuxeoException("Unsupported id type: " + idType);
        }

        idColumn = registerColumn(KEY_ID, COL_ID, typeId);
        registerColumn(KEY_PARENT_ID, COL_PARENT_ID, typeId);
        registerColumn(KEY_ANCESTOR_IDS, COL_ANCESTOR_IDS, TYPE_STRING_ARRAY);
        registerColumn(KEY_PRIMARY_TYPE, COL_PRIMARY_TYPE, TYPE_STRING);
        registerColumn(KEY_MIXIN_TYPES, COL_MIXIN_TYPES, TYPE_STRING_ARRAY);
        registerColumn(KEY_NAME, COL_NAME, TYPE_STRING);
        registerColumn(KEY_POS, COL_POS, TYPE_LONG);
        registerColumn(KEY_SYS_CHANGE_TOKEN, COL_SYS_CHANGE_TOKEN, TYPE_LONG);
        registerColumn(KEY_CHANGE_TOKEN, COL_CHANGE_TOKEN, TYPE_LONG);
        registerColumn(KEY_IS_PROXY, COL_IS_PROXY, TYPE_BOOLEAN);
        registerColumn(KEY_IS_CHECKED_IN, COL_IS_CHECKED_IN, TYPE_BOOLEAN);
        registerColumn(KEY_IS_VERSION, COL_IS_VERSION, TYPE_BOOLEAN);
        registerColumn(KEY_IS_LATEST_VERSION, COL_IS_LATEST_VERSION, TYPE_BOOLEAN);
        registerColumn(KEY_IS_LATEST_MAJOR_VERSION, COL_IS_LATEST_MAJOR_VERSION, TYPE_BOOLEAN);
        registerColumn(KEY_IS_TRASHED, COL_IS_TRASHED, TYPE_BOOLEAN);
        registerColumn(KEY_IS_RETENTION_ACTIVE, COL_IS_RETENTION_ACTIVE, TYPE_BOOLEAN);
        registerColumn(KEY_BASE_VERSION_ID, COL_BASE_VERSION_ID, typeId);
        registerColumn(KEY_MAJOR_VERSION, COL_MAJOR_VERSION, TYPE_LONG);
        registerColumn(KEY_MINOR_VERSION, COL_MINOR_VERSION, TYPE_LONG);
        registerColumn(KEY_VERSION_SERIES_ID, COL_VERSION_SERIES_ID, typeId);
        registerColumn(KEY_VERSION_CREATED, COL_VERSION_CREATED, TYPE_TIMESTAMP);
        registerColumn(KEY_VERSION_LABEL, COL_VERSION_LABEL, TYPE_STRING);
        registerColumn(KEY_VERSION_DESCRIPTION, COL_VERSION_DESCRIPTION, TYPE_STRING);
        registerColumn(KEY_PROXY_TARGET_ID, COL_PROXY_TARGET_ID, typeId);
        registerColumn(KEY_PROXY_VERSION_SERIES_ID, COL_PROXY_VERSION_SERIES_ID, typeId);
        registerColumn(KEY_PROXY_IDS, COL_PROXY_IDS, typeIdArray);
        registerColumn(KEY_LOCK_OWNER, COL_LOCK_OWNER, TYPE_STRING);
        registerColumn(KEY_LOCK_CREATED, COL_LOCK_CREATED, TYPE_TIMESTAMP);
        registerColumn(KEY_LIFECYCLE_POLICY, COL_LIFECYCLE_POLICY, TYPE_STRING);
        registerColumn(KEY_LIFECYCLE_STATE, COL_LIFECYCLE_STATE, TYPE_STRING);
        registerColumn(KEY_FULLTEXT_SIMPLE, COL_FULLTEXT_SIMPLE, TYPE_STRING);
        registerColumn(KEY_FULLTEXT_BINARY, COL_FULLTEXT_BINARY, TYPE_STRING);
        registerColumn(KEY_FULLTEXT_JOBID, COL_FULLTEXT_JOBID, TYPE_STRING);
        registerColumn(KEY_READ_ACL, COL_READ_ACL, TYPE_STRING_ARRAY);
        registerColumn(KEY_ACP, COL_ACP, TYPE_JSON);
        jsonDocColumn = registerColumn(PSEUDO_KEY_JSON, COL_JSON, TYPE_JSON);
    }

    protected PGColumn registerColumn(String key, String name, PGType type) {
        PGColumn col = new PGColumn(key, name, type);
        allColumns.add(col);
        keyToColumn.put(key, col);
        return col;
    }

    protected PGJSONConverter getConverter() {
        return converter;
    }

    protected TypesMap getTypesMap() {
        return typesMap;
    }

    protected List<PGColumn> getAllColumns() {
        return allColumns;
    }

    protected Map<String, PGColumn> getKeyToColumn() {
        return keyToColumn;
    }

    @Override
    protected void initBlobsPaths() {
        // TODO Auto-generated method stub
    }

    @Override
    public void markReferencedBinaries() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Lock getLock(String id) {
        return TransactionHelper.runWithoutTransaction(() -> super.getLock(id));
    }

    @Override
    public Lock setLock(String id, Lock lock) {
        return TransactionHelper.runWithoutTransaction(() -> super.setLock(id, lock));
    }

    @Override
    public Lock removeLock(String id, String owner) {
        return TransactionHelper.runWithoutTransaction(() -> super.removeLock(id, owner));
    }

}
