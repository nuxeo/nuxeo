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

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ANCESTOR_IDS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;
import static org.nuxeo.ecm.core.storage.pgjson.PGJSONRepository.COL_ID;
import static org.nuxeo.ecm.core.storage.pgjson.PGJSONRepository.COL_JSON;
import static org.nuxeo.ecm.core.storage.pgjson.PGJSONRepository.COL_PARENT_ID;
import static org.nuxeo.ecm.core.storage.pgjson.PGType.TYPE_JSON;

import java.io.Serializable;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.api.model.Delta;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.ListDiff;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.dbs.DBSConnection;
import org.nuxeo.ecm.core.storage.dbs.DBSConnectionBase;
import org.nuxeo.ecm.core.storage.dbs.DBSExpressionEvaluator;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryBase;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryBase.IdType;
import org.nuxeo.ecm.core.storage.dbs.DBSTransactionState.ChangeTokenUpdater;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.datasource.ConnectionHelper;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * PostgreSQL+JSON implementation of a {@link DBSConnection}.
 *
 * @since 11.1
 */
public class PGJSONConnection extends DBSConnectionBase {

    private static final Logger log = LogManager.getLogger(PGJSONConnection.class);

    protected static final int BATCH_SIZE = 100;

    public static final String TABLE_NAME = "documents";

    protected final Map<String, Type> allTypes;

    protected final PGJSONConverter converter;

    protected final List<PGColumn> allColumns;

    protected final PGColumn idColumn;

    protected final PGColumn jsonDocColumn;

    protected final Map<String, PGColumn> keyToColumn;

    protected final Connection connection;

    public PGJSONConnection(PGJSONRepository repository) {
        super(repository);
        allTypes = repository.getAllTypes();
        converter = repository.getConverter();
        allColumns = repository.getAllColumns();
        idColumn = repository.getIdColumn(); // TODO get from keyToColumn
        jsonDocColumn = repository.getJsonDocColumn(); // TODO get from keyToColumn
        keyToColumn = repository.getKeyToColumn();

        // we want a non-transactional connection
        connection = TransactionHelper.runWithoutTransaction(() -> {
            try {
                Connection c = ConnectionHelper.getConnection(repository.getDataSourceName(), true);
                c.setClientInfo("ApplicationName", "Nuxeo");
                return c;
            } catch (SQLException e) {
                throw new NuxeoException(e);
            }
        });
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            log.error(e, e);
        }
    }

    @Override
    public void begin() {
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public void commit() {
        try {
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public void rollback() {
        try {
            connection.rollback();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new NuxeoException(e);
        }
    }

    /**
     * Finds all the fields for all possible property paths. The property paths are simplified, there's no intermediate
     * {@code / * / } for complex properties, and no {@code [ ]} for lists.
     * <p>
     * This is needed at read time because at the JSON level we can't distinguish between longs, floats and calendars
     * (milliseconds), which are all represented by JSON numbers.
     */
    protected static class TypesFinder {

        protected final Map<String, Type> types = new HashMap<>();

        protected final Deque<String> path = new ArrayDeque<>();

        public Map<String, Type> find() {
            SchemaManager schemaManager = Framework.getService(SchemaManager.class);
            for (Schema schema : schemaManager.getSchemas()) {
                visitComplexType(schema);
            }
            return types;
        }

        protected void visitComplexType(ComplexType complexType) {
            for (Field field : complexType.getFields()) {
                visitField(field);
            }
        }

        protected void visitField(Field field) {
            Type type = field.getType();
            if (type.isSimpleType()) {
                // scalar
                String xpath = String.join("/", path);
                types.put(xpath, type);
            } else if (type.isComplexType()) {
                // complex property
                String name = field.getName().getPrefixedName();
                path.addLast(name);
                visitComplexType((ComplexType) type);
                path.removeLast();
            } else {
                // array or list
                Type fieldType = ((ListType) type).getFieldType();
                if (fieldType.isSimpleType()) {
                    // array
                    String xpath = String.join("/", path);
                    types.put(xpath, fieldType);
                } else {
                    // complex list
                    visitComplexType((ComplexType) fieldType);
                }
            }
        }
    }

    protected void initRepository() {
        try {
            // check table
            DatabaseMetaData metadata = connection.getMetaData();
            boolean hasTable = false;
            try (ResultSet rs = metadata.getTables(null, null, TABLE_NAME, new String[] { "TABLE" })) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    if (TABLE_NAME.equals(tableName)) {
                        hasTable = true;
                        break;
                    }
                }
            }
            if (hasTable) {
                return;
            }
            createTable();
            String indexSQL = "";

        } catch (SQLException e) {
            throw new NuxeoException(e);
        } catch (Exception e) { // XXX for debug
            throw new NuxeoException(e);
        }
        initRoot();
    }

    protected void createTable() throws SQLException {
        StringBuilder buf = new StringBuilder();
        buf.append("CREATE TABLE ");
        buf.append(TABLE_NAME);
        buf.append(" (");
        for (int i = 0; i < allColumns.size(); i++) {
            if (i != 0) {
                buf.append(", ");
            }
            PGColumn col = allColumns.get(i);
            buf.append(col.name);
            buf.append(' ');
            buf.append(col.type.name);
            if (col.name.equals(COL_ID)) {
                buf.append(" PRIMARY KEY");
            } else if (col.name.equals(COL_PARENT_ID)) {
                buf.append(" REFERENCES ");
                buf.append(TABLE_NAME);
                buf.append('(');
                buf.append(COL_ID);
                buf.append(") ON DELETE CASCADE");
            }
        }
        buf.append(')');
        String sql = buf.toString();
        try (Statement st = connection.createStatement()) {
            log.trace("SQL: {}", sql);
            st.execute(sql);
        }
    }

    protected void setValue(PreparedStatement ps, int i, PGType type, Object value) throws SQLException {
        if (value == null) {
            ps.setNull(i, type.type);
        } else if (type == PGType.TYPE_STRING) {
            ps.setString(i, (String) value);
        } else if (type == PGType.TYPE_LONG) {
            ps.setLong(i, ((Long) value).longValue());
        } else if (type == PGType.TYPE_DOUBLE) {
            ps.setDouble(i, ((Double) value).doubleValue());
        } else if (type == PGType.TYPE_TIMESTAMP) {
            long millis = ((Calendar) value).getTimeInMillis();
            ps.setLong(i, millis);
        } else if (type == PGType.TYPE_BOOLEAN) {
            ps.setBoolean(i, ((Boolean) value).booleanValue());
        } else if (type.isArray()) {
            Array array = connection.createArrayOf(type.baseType.name, (Object[]) value);
            ps.setArray(i, array);
        } else if (type == PGType.TYPE_JSON) {
            String json = converter.valueToJson(value);
            ps.setString(i, json);
        } else {
            throw new UnsupportedOperationException("Unsupported type: " + type);
        }
    }

    protected Serializable getValue(ResultSet rs, int i, PGType type) throws SQLException {
        if (type == PGType.TYPE_STRING) {
            return rs.getString(i);
        } else if (type == PGType.TYPE_LONG) {
            long l = rs.getLong(i);
            if (rs.wasNull()) {
                return null;
            }
            return Long.valueOf(l);
        } else if (type == PGType.TYPE_DOUBLE) {
            double d = rs.getDouble(i);
            if (rs.wasNull()) {
                return null;
            }
            return Double.valueOf(d);
        } else if (type == PGType.TYPE_TIMESTAMP) {
            long millis = rs.getLong(i);
            if (rs.wasNull()) {
                return null;
            }
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(millis);
            return cal;
        } else if (type == PGType.TYPE_BOOLEAN) {
            boolean b = rs.getBoolean(i);
            if (rs.wasNull()) {
                return null;
            }
            return Boolean.valueOf(b);
        } else if (type == PGType.TYPE_STRING_ARRAY) {
            Array array = rs.getArray(i);
            if (rs.wasNull()) {
                return null;
            }
            Object[] objectArray = (Object[]) array.getArray();
            if (objectArray instanceof String[]) {
                return objectArray;
            } else {
                // convert to String[]
                String[] stringArray = new String[objectArray.length];
                System.arraycopy(objectArray, 0, stringArray, 0, objectArray.length);
                return stringArray;
            }
        } else if (type == PGType.TYPE_LONG_ARRAY) {
            Array array = rs.getArray(i);
            if (rs.wasNull()) {
                return null;
            }
            Object[] objectArray = (Object[]) array.getArray();
            if (objectArray instanceof Long[]) {
                return objectArray;
            } else {
                // convert to Long[]
                Long[] longArray = new Long[objectArray.length];
                System.arraycopy(objectArray, 0, longArray, 0, objectArray.length);
                return longArray;
            }
        } else if (type == PGType.TYPE_JSON) {
            String json = rs.getString(i);
            if (rs.wasNull()) {
                return null;
            }
            return converter.jsonToValue(json);
        } else {
            throw new UnsupportedOperationException("Unsupported type: " + type);
        }
    }

    @Override
    public String generateNewId() {
        if (DBSRepositoryBase.DEBUG_UUIDS) {
            return "UUID_" + ((PGJSONRepository) repository).debugUUIDCounter.incrementAndGet();
        }
        if (repository.getIdType() == IdType.sequence) {
            throw new UnsupportedOperationException();
        } else {
            return UUID.randomUUID().toString();
        }
    }

    @Override
    public State readPartialState(String id, Collection<String> keys) {
        List<PGColumn> queryColumns = Arrays.asList(idColumn);
        List<Serializable> queryValues = Arrays.asList(id); // TODO convert for idType
        List<PGColumn> returnedColumns = new ArrayList<>();
        for (String key : keys) {
            PGColumn col = keyToColumn.get(key);
            if (col == null) {
                throw new UnsupportedOperationException(key);
            }
            returnedColumns.add(col);
        }
        List<State> states = queryKeyValue(queryColumns, queryValues, null, returnedColumns);
        if (states.isEmpty()) {
            return null;
        } else if (states.size() == 1) {
            return states.get(0);
        } else {
            throw new NuxeoException("More that one child with ecm:id=" + id);
        }
    }

    @Override
    public State readState(String id) {
        List<PGColumn> queryColumns = Arrays.asList(idColumn);
        List<Serializable> queryValues = Arrays.asList(id); // TODO convert for idType
        List<State> states = queryKeyValue(queryColumns, queryValues, null);
        if (states.isEmpty()) {
            return null;
        } else if (states.size() == 1) {
            return states.get(0);
        } else {
            throw new NuxeoException("More that one child with ecm:id=" + id);
        }
    }

    @Override
    public List<State> readStates(List<String> ids) {
        // TODO optimize to fetch several rows in a single query
        List<State> list = new ArrayList<>(ids.size());
        for (String id : ids) {
            list.add(readState(id));
        }
        return list;
    }

    @Override
    public void createStates(List<State> states) {
        states.forEach(this::createState); // TODO optimize
    }

    @Override
    public void createState(State state) {
        List<PGColumn> columns = new ArrayList<>();
        List<Serializable> values = new ArrayList<>();
        State jsonDoc = new State(); // everything not stored in individual columns

        StringBuilder buf = new StringBuilder();
        buf.append("INSERT INTO ");
        buf.append(TABLE_NAME);
        buf.append(" (");
        for (Entry<String, Serializable> en : state.entrySet()) {
            String key = en.getKey();
            Serializable value = en.getValue();
            PGColumn col = keyToColumn.get(key);
            if (col == null) {
                // collect into JSON doc everything not explicitly in columns
                jsonDoc.put(key, value);
            } else {
                if (!columns.isEmpty()) {
                    buf.append(", ");
                }
                columns.add(col);
                values.add(value);
                buf.append(col.name);
            }
        }
        if (!jsonDoc.isEmpty()) {
            if (!columns.isEmpty()) {
                buf.append(", ");
            }
            columns.add(jsonDocColumn);
            buf.append(jsonDocColumn.name);
            values.add(jsonDoc);
        }
        buf.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            PGColumn col = columns.get(i);
            if (i != 0) {
                buf.append(", ");
            }
            if (col.type == TYPE_JSON) {
                buf.append("?::jsonb");
            } else {
                buf.append('?');
            }
        }
        buf.append(')');
        String sql = buf.toString();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < columns.size(); i++) {
                PGColumn col = columns.get(i);
                Object value = values.get(i);
                setValue(ps, i + 1, col.type, value);
            }
            ps.execute();
        } catch (SQLException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public void updateState(String id, StateDiff diff, ChangeTokenUpdater changeTokenUpdater) {
        // TODO changeTokenUpdater
        // TODO optimize later the number of writes
        List<PGColumn> columns = new ArrayList<>();
        List<Serializable> values = new ArrayList<>();

        StringBuilder buf = new StringBuilder();
        buf.append("UPDATE ");
        buf.append(TABLE_NAME);
        buf.append(" SET ");
        StateDiff jsonDocUpdate = new StateDiff();
        List<String> jsonDocRemove = new ArrayList<>();
        for (Entry<String, Serializable> en : diff.entrySet()) {
            String key = en.getKey();
            Serializable value = en.getValue();
            PGColumn col = keyToColumn.get(key);
            if (col != null) {
                if (!columns.isEmpty()) {
                    buf.append(", ");
                }
                if (value instanceof StateDiff) {
                    throw new UnsupportedOperationException("StateDiff");
                } else if (value instanceof ListDiff) {
                    throw new UnsupportedOperationException("ListDiff");
                } else if (value instanceof State) {
                    throw new UnsupportedOperationException();
                } else if (value instanceof Delta) {
                    buf.append(col.name);
                    columns.add(col);
                    buf.append(" = ");
                    buf.append(col.name);
                    buf.append(" + ?");
                    values.add(((Delta) value).getDeltaValue());
                } else {
                    buf.append(col.name);
                    columns.add(col);
                    buf.append(" = ?");
                    values.add(value);
                }
            } else {
                // collect into JSON doc everything not explicitly in columns
                if (value == null) {
                    jsonDocRemove.add(key);
                } else {
                    jsonDocUpdate.put(key, value);
                }
            }
        }
        if (!jsonDocUpdate.isEmpty() || !jsonDocRemove.isEmpty()) {
            if (!columns.isEmpty()) {
                buf.append(", ");
            }
            buf.append(COL_JSON);
            columns.add(jsonDocColumn);
            buf.append(" = ");
            buf.append(COL_JSON);
            if (!jsonDocUpdate.isEmpty()) {
                // check values supported
                for (Entry<String, Serializable> en : jsonDocUpdate.entrySet()) {
                    String key = en.getKey();
                    Serializable value = en.getValue();
                    // TODO for now we collect just direct (not diff) updates to first-level keys
                    if (value instanceof StateDiff) {
                        throw new UnsupportedOperationException("StateDiff");
                    } else if (value instanceof ListDiff) {
                        throw new UnsupportedOperationException("ListDiff");
                    } else if (value instanceof State) {
                        // State ok
                    } else if (value instanceof Delta) {
                        throw new UnsupportedOperationException("Delta");
                    } else {
                        // value ok
                    }
                }
                buf.append(" || ?::jsonb");
                values.add(jsonDocUpdate);
            }
            if (!jsonDocRemove.isEmpty()) {
                buf.append(" - {"); // PostgreSQL array syntax for list of keys to remove
                for (int i = 0; i < jsonDocRemove.size(); i++) {
                    if (i != 0) {
                        buf.append(',');
                    }
                    String key = jsonDocRemove.get(i);
                    buf.append(key);
                }
                buf.append("}::text[]");
            }
        }
        buf.append(" WHERE ");
        buf.append(COL_ID);
        columns.add(idColumn);
        buf.append(" = ?");
        values.add(id); // TODO convert for idType
        // TODO add changeToken from condition
        String sql = buf.toString();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < columns.size(); i++) {
                PGColumn col = columns.get(i);
                Object value = values.get(i);
                setValue(ps, i + 1, col.type, value);
            }
            int count = ps.executeUpdate();
            if (count != 1) {
                // change token condition didn't match or doc concurrently deleted
                throw new ConcurrentUpdateException(id);
            }
        } catch (SQLException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public void deleteStates(Set<String> ids) {
        if (ids.isEmpty()) {
            return;
        }
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE " + COL_ID + " ";
        int size = ids.size();
        if (size == 1) {
            sql += "= ?";
        } else {
            StringBuilder buf = new StringBuilder(3 + 3 * size);
            buf.append("IN (");
            for (int i = 0; i < size; i++) {
                if (i != 0) {
                    buf.append(", ");
                }
                buf.append('?');
            }
            buf.append(')');
            sql += buf;
        }
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 0;
            for (String id : ids) {
                ps.setString(++i, id);
            }
            int count = ps.executeUpdate();
            if (count != ids.size()) {
                log.debug("Removed {} docs for {} ids: {}", () -> count, ids::size, () -> ids);
            }
        } catch (SQLException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public State readChildState(String parentId, String name, Set<String> ignored) {
        PGColumn parentIdCol = keyToColumn.get(KEY_PARENT_ID);
        PGColumn nameCol = keyToColumn.get(KEY_NAME);
        List<PGColumn> queryColumns = Arrays.asList(parentIdCol, nameCol);
        List<Serializable> queryValues = Arrays.asList(parentId, name);
        List<State> states = queryKeyValue(queryColumns, queryValues, ignored);
        if (states.isEmpty()) {
            return null;
        } else if (states.size() == 1) {
            return states.get(0);
        } else {
            throw new NuxeoException("More that one child with ecm:parentId=" + parentId + " and ecm:name=" + name);
        }
    }

    @Override
    public boolean hasChild(String parentId, String name, Set<String> ignored) {
        PGColumn parentIdCol = keyToColumn.get(KEY_PARENT_ID);
        PGColumn nameCol = keyToColumn.get(KEY_NAME);
        List<PGColumn> queryColumns = Arrays.asList(parentIdCol, nameCol);
        List<Serializable> queryValues = Arrays.asList(parentId, name);
        return queryKeyValuePresence(queryColumns, queryValues, ignored);
    }

    @Override
    public List<State> queryKeyValue(String key, Object value, Set<String> ignored) {
        PGColumn col = keyToColumn.get(key);
        if (col == null) {
            throw new UnsupportedOperationException(key);
        }
        List<PGColumn> queryColumns = Arrays.asList(col);
        List<Serializable> queryValues = Arrays.asList((Serializable) value); // TODO convert for idType
        return queryKeyValue(queryColumns, queryValues, ignored);
    }

    @Override
    public List<State> queryKeyValue(String key1, Object value1, String key2, Object value2, Set<String> ignored) {
        PGColumn col1 = keyToColumn.get(key1);
        PGColumn col2 = keyToColumn.get(key2);
        if (col1 == null) {
            throw new UnsupportedOperationException(key1);
        }
        if (col2 == null) {
            throw new UnsupportedOperationException(key2);
        }
        List<PGColumn> queryColumns = Arrays.asList(col1, col2);
        List<Serializable> queryValues = Arrays.asList((Serializable) value1, (Serializable) value2);
        return queryKeyValue(queryColumns, queryValues, ignored);
    }

    @Override
    public Stream<State> getDescendants(String id, Set<String> keys) {
        return getDescendants(id, keys, 0);
    }

    @Override
    public Stream<State> getDescendants(String id, Set<String> keys, int limit) {
        // TODO limit
        PGColumn ancestorIdsCol = keyToColumn.get(KEY_ANCESTOR_IDS);
        List<PGColumn> queryColumns = Arrays.asList(ancestorIdsCol);
        List<Serializable> queryValues = Arrays.asList(id); // TODO convert for idType
        List<PGColumn> returnedColumns = new ArrayList<>();
        for (String key : keys) {
            PGColumn col = keyToColumn.get(key);
            if (col == null) {
                throw new UnsupportedOperationException(key);
            }
            returnedColumns.add(col);
        }
        if (!returnedColumns.contains(idColumn)) {
            returnedColumns.add(idColumn);
        }
        List<State> states = queryKeyValue(queryColumns, queryValues, null, returnedColumns);
        return StreamSupport.stream(states.spliterator(), false);
    }

    @Override
    public boolean queryKeyValuePresence(String key, String value, Set<String> ignored) {
        PGColumn col = keyToColumn.get(key);
        if (col == null) {
            throw new UnsupportedOperationException(key);
        }
        List<PGColumn> queryColumns = Arrays.asList(col);
        List<Serializable> queryValues = Arrays.asList(value);
        return queryKeyValuePresence(queryColumns, queryValues, ignored);
    }

    @Override
    public PartialList<Map<String, Serializable>> queryAndFetch(DBSExpressionEvaluator evaluator,
            OrderByClause orderByClause, boolean distinctDocuments, int limit, int offset, int countUpTo) {
        // TODO Auto-generated method stub
        return new PartialList<>(Collections.emptyList(), 0);
    }

    protected List<State> queryKeyValue(List<PGColumn> queryColumns, List<Serializable> queryValues, Set<String> ignored) {
        return queryKeyValue(queryColumns, queryValues, ignored, allColumns);
    }

    protected List<State> queryKeyValue(List<PGColumn> queryColumns, List<Serializable> queryValues,
            Set<String> ignored, List<PGColumn> returnedColumns) {
        List<State> states = new ArrayList<>();
        List<PGType> queryColumnTypes = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        buf.append("SELECT ");
        boolean firstCol = true;
        for (PGColumn col : returnedColumns) {
            if (queryColumns.contains(col) && !col.type.isArray()) {
                // query columns that aren't arrays are matched with = so we know the value
                continue;
            }
            if (firstCol) {
                firstCol = false;
            } else {
                buf.append(", ");
            }
            buf.append(col.name);
        }
        buf.append(" FROM ");
        buf.append(TABLE_NAME);
        buf.append(" WHERE ");
        for (int i = 0; i < queryColumns.size(); i++) {
            if (i != 0) {
                buf.append(" AND ");
            }
            PGColumn col = queryColumns.get(i);
            buf.append(col.name);
            PGType type = col.type;
            if (!type.isArray()) {
                buf.append(" = ?");
                queryColumnTypes.add(type);
                // COL_JSON + "->'" + KEY_ANCESTOR_IDS + "' <@ ?::jsonb";
            } else {
                buf.append(" @> ARRAY[?]"); // contains
                buf.append(getCastForIdArray());
                // to set the value we must use the base type, not the array type
                queryColumnTypes.add(type.baseType);
            }
        }
        String sql = buf.toString();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < queryColumnTypes.size(); i++) {
                PGType type = queryColumnTypes.get(i);
                Serializable value = queryValues.get(i);
                setValue(ps, i + 1, type, value);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    State state = new State();
                    // fixed values from the query
                    for (int i = 0; i < queryColumns.size(); i++) {
                        PGColumn col = queryColumns.get(i);
                        if (col.type.isArray()) {
                            // for arrays we don't have the full value because
                            // we're doing a "contains" operator
                            continue;
                        }
                        Serializable value = queryValues.get(i);
                        state.put(col.key, value); // TODO not converted for idType
                    }
                    // values from the result set
                    int i = 0;
                    for (PGColumn col : returnedColumns) {
                        if (queryColumns.contains(col) && !col.type.isArray()) {
                            continue;
                        }
                        i++;
                        Serializable value = getValue(rs, i, col.type);
                        if (value instanceof State) {
                            State jsonDoc = (State) value;
                            for (Entry<String, Serializable> en : jsonDoc.entrySet()) {
                                state.put(en.getKey(), en.getValue());
                            }
                        } else if (value != null) {
                            state.put(col.key, value);
                        }
                    }
                    // TODO do ignore in query instead of post-filter
                    if (ignored != null && !ignored.isEmpty()) {
                        if (ignored.contains(state.get(KEY_ID))) {
                            continue;
                        }
                    }
                    states.add(state);
                }
                return states;
            }
        } catch (SQLException e) {
            throw new NuxeoException(e);
        }
    }

    protected String getCastForIdArray() {
        IdType idType = repository.getIdType();
        switch (idType) {
        case varchar:
            return "";
        case uuid:
            return "::uuid[]";
        case sequence:
            return "::bigint[]";
        default:
            throw new AssertionError("Unknown id type: " + idType);
        }
    }

    protected boolean queryKeyValuePresence(List<PGColumn> queryColumns, List<Serializable> queryValues,
            Set<String> ignored) {
        // "ignored" unused (callers all use an empty set)
        StringBuilder buf = new StringBuilder();
        buf.append("SELECT 1 FROM ");
        buf.append(TABLE_NAME);
        buf.append(" WHERE ");
        for (int i = 0; i < queryColumns.size(); i++) {
            if (i != 0) {
                buf.append(" AND ");
            }
            PGColumn col = queryColumns.get(i);
            buf.append(col.name);
            buf.append(" = ?");
            // COL_JSON + "->'" + KEY_ANCESTOR_IDS + "' <@ ?::jsonb";
        }
        String sql = buf.toString();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < queryColumns.size(); i++) {
                PGColumn col = queryColumns.get(i);
                Serializable value = queryValues.get(i);
                setValue(ps, i + 1, col.type, value);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public ScrollResult<String> scroll(DBSExpressionEvaluator evaluator, int batchSize, int keepAliveSeconds) {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public ScrollResult<String> scroll(String scrollId) {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Lock getLock(String id) {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Lock setLock(String id, Lock lock) {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    public Lock removeLock(String id, String owner) {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

}
