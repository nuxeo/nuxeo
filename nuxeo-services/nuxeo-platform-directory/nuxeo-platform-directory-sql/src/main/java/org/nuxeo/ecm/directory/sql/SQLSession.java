/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     George Lefter
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.sql;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.storage.sql.ColumnSpec;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCLogger;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Delete;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Insert;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Select;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Update;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.ecm.core.utils.SIDGenerator;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.OperationNotAllowedException;
import org.nuxeo.ecm.directory.PasswordHelper;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.ecm.directory.sql.filter.SQLComplexFilter;

/**
 * This class represents a session against an SQLDirectory.
 */
public class SQLSession extends BaseSession {

    private static final Log log = LogFactory.getLog(SQLSession.class);

    // set to false for debugging
    private static final boolean HIDE_PASSWORD_IN_LOGS = true;

    final Table table;

    protected SQLStaticFilter[] staticFilters;

    String sid;

    Connection sqlConnection;

    private final Dialect dialect;

    protected JDBCLogger logger = new JDBCLogger("SQLDirectory");

    public SQLSession(SQLDirectory directory, SQLDirectoryDescriptor config) throws DirectoryException {
        super(directory, TableReference.class);
        table = directory.getTable();
        dialect = directory.getDialect();
        sid = String.valueOf(SIDGenerator.next());
        staticFilters = config.getStaticFilters();
        acquireConnection();
    }

    @Override
    public SQLDirectory getDirectory() {
        return (SQLDirectory) directory;
    }

    @Override
    public DocumentModel getEntryFromSource(String id, boolean fetchReferences) throws DirectoryException {
        acquireConnection();
        // String sql = String.format("SELECT * FROM %s WHERE %s = ?",
        // tableName, idField);
        Select select = new Select(table);
        select.setFrom(table.getQuotedName());
        select.setWhat(getReadColumnsSQL());

        String whereClause = table.getPrimaryColumn().getQuotedName() + " = ?";
        whereClause = addFilterWhereClause(whereClause);

        select.setWhere(whereClause);
        String sql = select.getStatement();

        if (logger.isLogEnabled()) {
            List<Serializable> values = new ArrayList<>();
            values.add(id);
            addFilterValuesForLog(values);
            logger.logSQL(sql, values);
        }

        try (PreparedStatement ps = sqlConnection.prepareStatement(sql)) {
            setFieldValue(ps, 1, table.getPrimaryColumn(), id);
            addFilterValues(ps, 2);

            Map<String, Object> fieldMap = new HashMap<>();
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                for (Column column : getReadColumns()) {
                    Object value = getFieldValue(rs, column);
                    fieldMap.put(column.getKey(), value);
                }
                if (logger.isLogEnabled()) {
                    logger.logResultSet(rs, getReadColumns());
                }
            }

            if (isMultiTenant()) {
                // check that the entry is from the current tenant, or no tenant
                // at all
                String tenantId = getCurrentTenantId();
                if (!StringUtils.isBlank(tenantId)) {
                    String entryTenantId = (String) fieldMap.get(TENANT_ID_FIELD);
                    if (!StringUtils.isBlank(entryTenantId)) {
                        if (!entryTenantId.equals(tenantId)) {
                            return null;
                        }
                    }
                }
            }

            DocumentModel entry = fieldMapToDocumentModel(fieldMap);

            // fetch the reference fields
            if (fetchReferences) {
                Map<String, List<String>> targetIdsMap = new HashMap<>();
                for (Reference reference : directory.getReferences()) {
                    List<String> targetIds = reference.getTargetIdsForSource(entry.getId());
                    targetIds = new ArrayList<>(targetIds);
                    Collections.sort(targetIds);
                    String fieldName = reference.getFieldName();
                    if (targetIdsMap.containsKey(fieldName)) {
                        targetIdsMap.get(fieldName).addAll(targetIds);
                    } else {
                        targetIdsMap.put(fieldName, targetIds);
                    }
                }
                for (Entry<String, List<String>> en : targetIdsMap.entrySet()) {
                    String fieldName = en.getKey();
                    List<String> targetIds = en.getValue();
                    try {
                        entry.setProperty(schemaName, fieldName, targetIds);
                    } catch (PropertyException e) {
                        throw new DirectoryException(e);
                    }
                }
            }
            return entry;
        } catch (SQLException e) {
            throw new DirectoryException("getEntry failed", e);
        }
    }

    protected List<Column> getReadColumns() {
        return readAllColumns ? getDirectory().readColumnsAll : getDirectory().readColumns;
    }

    protected String getReadColumnsSQL() {
        return readAllColumns ? getDirectory().readColumnsAllSQL : getDirectory().readColumnsSQL;
    }

    protected DocumentModel fieldMapToDocumentModel(Map<String, Object> fieldMap) {
        String idFieldName = directory.getSchemaFieldMap().get(getIdField()).getName().getPrefixedName();
        // If the prefixed id is not here, try to get without prefix
        // It may happen when we gentry from sql
        if (!fieldMap.containsKey(idFieldName)) {
            idFieldName = getIdField();
        }

        String id = String.valueOf(fieldMap.get(idFieldName));
        try {
            DocumentModel docModel = BaseSession.createEntryModel(sid, schemaName, id, fieldMap, isReadOnly());
            return docModel;
        } catch (PropertyException e) {
            log.error(e, e);
            return null;
        }
    }

    private void acquireConnection() throws DirectoryException {
        try {
            if (sqlConnection == null || sqlConnection.isClosed()) {
                sqlConnection = getDirectory().getConnection();
            }
        } catch (SQLException e) {
            throw new DirectoryException(
                    "Cannot connect to SQL directory '" + directory.getName() + "': " + e.getMessage(), e);
        }
    }

    /**
     * Checks the SQL error we got and determine if a concurrent update happened. Throws if that's the case.
     *
     * @param e the exception
     * @since 7.10-HF04, 8.2
     */
    protected void checkConcurrentUpdate(Throwable e) throws ConcurrentUpdateException {
        if (dialect.isConcurrentUpdateException(e)) {
            throw new ConcurrentUpdateException(e);
        }
    }

    protected String addFilterWhereClause(String whereClause) throws DirectoryException {
        if (staticFilters.length == 0) {
            return whereClause;
        }
        if (whereClause != null && whereClause.trim().length() > 0) {
            whereClause = whereClause + " AND ";
        } else {
            whereClause = "";
        }
        for (int i = 0; i < staticFilters.length; i++) {
            SQLStaticFilter filter = staticFilters[i];
            whereClause += filter.getDirectoryColumn(table, getDirectory().useNativeCase()).getQuotedName();
            whereClause += " " + filter.getOperator() + " ";
            whereClause += "? ";

            if (i < staticFilters.length - 1) {
                whereClause = whereClause + " AND ";
            }
        }
        return whereClause;
    }

    protected void addFilterValues(PreparedStatement ps, int startIdx) throws DirectoryException {
        for (int i = 0; i < staticFilters.length; i++) {
            SQLStaticFilter filter = staticFilters[i];
            setFieldValue(ps, startIdx + i, filter.getDirectoryColumn(table, getDirectory().useNativeCase()),
                    filter.getValue());
        }
    }

    protected void addFilterValuesForLog(List<Serializable> values) {
        for (int i = 0; i < staticFilters.length; i++) {
            values.add(staticFilters[i].getValue());
        }
    }

    /**
     * Internal method to read the hashed password for authentication.
     *
     * @since 9.1
     */
    protected String getPassword(String id) {
        acquireConnection();

        Select select = new Select(table);
        select.setFrom(table.getQuotedName());
        List<Column> whatColumns = new ArrayList<>(2);
        whatColumns.add(table.getColumn(getPasswordField()));
        if (isMultiTenant()) {
            whatColumns.add(table.getColumn(TENANT_ID_FIELD));
        }
        String what = whatColumns.stream().map(Column::getQuotedName).collect(Collectors.joining(", "));
        select.setWhat(what);
        String whereClause = table.getPrimaryColumn().getQuotedName() + " = ?";
        whereClause = addFilterWhereClause(whereClause);
        select.setWhere(whereClause);
        String sql = select.getStatement();

        if (logger.isLogEnabled()) {
            List<Serializable> values = new ArrayList<>();
            values.add(id);
            addFilterValuesForLog(values);
            logger.logSQL(sql, values);
        }

        try (PreparedStatement ps = sqlConnection.prepareStatement(sql)) {
            setFieldValue(ps, 1, table.getPrimaryColumn(), id);
            addFilterValues(ps, 2);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                if (isMultiTenant()) {
                    // check that the entry is from the current tenant, or no tenant at all
                    String tenantId = getCurrentTenantId();
                    if (!StringUtils.isBlank(tenantId)) {
                        String entryTenantId = (String) getFieldValue(rs, table.getColumn(TENANT_ID_FIELD));
                        if (!StringUtils.isBlank(entryTenantId)) {
                            if (!entryTenantId.equals(tenantId)) {
                                return null;
                            }
                        }
                    }
                }
                String password = (String) getFieldValue(rs, table.getColumn(getPasswordField()));
                if (logger.isLogEnabled()) {
                    String value = HIDE_PASSWORD_IN_LOGS ? "********" : password;
                    logger.logMap(Collections.singletonMap(getPasswordField(), value));
                }
                return password;
            }
        } catch (SQLException e) {
            throw new DirectoryException("getPassword failed", e);
        }
    }

    @Override
    public void deleteEntry(String id) {
        acquireConnection();
        if (!canDeleteMultiTenantEntry(id)) {
            throw new OperationNotAllowedException("Operation not allowed in the current tenant context",
                    "label.directory.error.multi.tenant.operationNotAllowed", null);
        }
        super.deleteEntry(id);
    }

    @Override
    public void deleteEntry(String id, Map<String, String> map) throws DirectoryException {
        checkPermission(SecurityConstants.WRITE);
        acquireConnection();

        if (!canDeleteMultiTenantEntry(id)) {
            throw new DirectoryException("Operation not allowed in the current tenant context");
        }

        // Assume in this case that there are no References to this entry.
        Delete delete = new Delete(table);
        StringBuilder whereClause = new StringBuilder();
        List<Serializable> values = new ArrayList<>(1 + map.size());

        whereClause.append(table.getPrimaryColumn().getQuotedName());
        whereClause.append(" = ?");
        values.add(id);
        for (Entry<String, String> e : map.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            whereClause.append(" AND ");
            Column col = table.getColumn(key);
            if (col == null) {
                throw new IllegalArgumentException("Unknown column " + key);
            }
            whereClause.append(col.getQuotedName());
            if (value == null) {
                whereClause.append(" IS NULL");
            } else {
                whereClause.append(" = ?");
                values.add(value);
            }
        }
        delete.setWhere(whereClause.toString());
        String sql = delete.getStatement();

        if (logger.isLogEnabled()) {
            logger.logSQL(sql, values);
        }

        try (PreparedStatement ps = sqlConnection.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) {
                if (i == 0) {
                    setFieldValue(ps, 1, table.getPrimaryColumn(), values.get(i));
                } else {
                    ps.setString(1 + i, (String) values.get(i));
                }
            }
            ps.execute();
        } catch (SQLException e) {
            checkConcurrentUpdate(e);
            throw new DirectoryException("deleteEntry failed", e);
        }
        getDirectory().invalidateCaches();
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter, Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences, int limit, int offset) throws DirectoryException {
        if (!hasPermission(SecurityConstants.READ)) {
            return new DocumentModelListImpl();
        }
        acquireConnection();
        Map<String, Object> filterMap = new LinkedHashMap<>(filter);
        filterMap.remove(getPasswordField()); // cannot filter on password

        if (isMultiTenant()) {
            // filter entries on the tenantId field also
            String tenantId = getCurrentTenantId();
            if (!StringUtils.isBlank(tenantId)) {
                filterMap.put(TENANT_ID_FIELD, tenantId);
            }
        }

        try {
            // build count query statement
            StringBuilder whereClause = new StringBuilder();
            String separator = "";
            List<Column> orderedColumns = new LinkedList<>();
            for (String columnName : filterMap.keySet()) {

                if (getDirectory().isReference(columnName)) {
                    log.warn(columnName + " is a reference and will be ignored" + " as a query criterion");
                    continue;
                }

                Object value = filterMap.get(columnName);
                Column column = table.getColumn(columnName);
                if (null == column) {
                    // this might happen if we have a case like a chain
                    // selection and a directory without parent column
                    throw new DirectoryException("cannot find column '" + columnName + "' for table: " + table);
                }
                String leftSide = column.getQuotedName();
                String rightSide = "?";
                String operator;
                boolean substring = fulltext != null && fulltext.contains(columnName);
                if ("".equals(value) && dialect.hasNullEmptyString() && !substring) {
                    // see NXP-6172, empty values are Null in Oracle
                    value = null;
                }
                if (value != null) {
                    if (value instanceof SQLComplexFilter) {
                        SQLComplexFilter complexFilter = (SQLComplexFilter) value;
                        operator = complexFilter.getOperator();
                        rightSide = complexFilter.getRightSide();
                    } else if (substring) {
                        // NB : remove double % in like query NXGED-833
                        String searchedValue = null;
                        switch (substringMatchType) {
                        case subany:
                            searchedValue = '%' + String.valueOf(value).toLowerCase() + '%';
                            break;
                        case subinitial:
                            searchedValue = String.valueOf(value).toLowerCase() + '%';
                            break;
                        case subfinal:
                            searchedValue = '%' + String.valueOf(value).toLowerCase();
                            break;
                        }
                        filterMap.put(columnName, searchedValue);
                        if (dialect.supportsIlike()) {
                            operator = " ILIKE "; // postgresql rules
                        } else {
                            leftSide = "LOWER(" + leftSide + ')';
                            operator = " LIKE ";
                        }
                    } else {
                        operator = " = ";
                    }
                } else {
                    operator = " IS NULL";
                }
                whereClause.append(separator).append(leftSide).append(operator);
                if (value != null) {
                    whereClause.append(rightSide);
                    orderedColumns.add(column);
                }
                separator = " AND ";
            }

            int queryLimitSize = getDirectory().getDescriptor().getQuerySizeLimit();
            boolean trucatedResults = false;
            if (queryLimitSize != 0 && (limit <= 0 || limit > queryLimitSize)) {
                // create a preparedStatement for counting and bind the values
                Select select = new Select(table);
                select.setWhat("count(*)");
                select.setFrom(table.getQuotedName());

                String where = whereClause.toString();
                where = addFilterWhereClause(where);
                select.setWhere(where);

                String countQuery = select.getStatement();
                if (logger.isLogEnabled()) {
                    List<Serializable> values = new ArrayList<>(orderedColumns.size());
                    for (Column column : orderedColumns) {
                        Object value = filterMap.get(column.getKey());
                        values.add((Serializable) value);
                    }
                    addFilterValuesForLog(values);
                    logger.logSQL(countQuery, values);
                }
                int count;
                try (PreparedStatement ps = sqlConnection.prepareStatement(countQuery)) {
                    fillPreparedStatementFields(filterMap, orderedColumns, ps);

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        count = rs.getInt(1);
                    }
                }
                if (logger.isLogEnabled()) {
                    logger.logCount(count);
                }
                if (count > queryLimitSize) {
                    trucatedResults = true;
                    limit = queryLimitSize;
                    log.error("Displayed results will be truncated because too many rows in result: " + count);
                    // throw new SizeLimitExceededException("too many rows in result: " + count);
                }
            }

            // create a preparedStatement and bind the values
            // String query = new StringBuilder("SELECT * FROM
            // ").append(tableName).append(
            // whereClause).toString();

            Select select = new Select(table);
            select.setWhat(getReadColumnsSQL());
            select.setFrom(table.getQuotedName());

            String where = whereClause.toString();
            where = addFilterWhereClause(where);
            select.setWhere(where);

            StringBuilder orderby = new StringBuilder(128);
            if (orderBy != null) {
                for (Iterator<Map.Entry<String, String>> it = orderBy.entrySet().iterator(); it.hasNext();) {
                    Entry<String, String> entry = it.next();
                    orderby.append(dialect.openQuote())
                           .append(entry.getKey())
                           .append(dialect.closeQuote())
                           .append(' ')
                           .append(entry.getValue());
                    if (it.hasNext()) {
                        orderby.append(',');
                    }
                }
            }
            select.setOrderBy(orderby.toString());
            String query = select.getStatement();
            boolean manualLimitOffset;
            if (limit <= 0) {
                manualLimitOffset = false;
            } else {
                if (offset < 0) {
                    offset = 0;
                }
                if (dialect.supportsPaging()) {
                    query = dialect.addPagingClause(query, limit, offset);
                    manualLimitOffset = false;
                } else {
                    manualLimitOffset = true;
                }
            }

            if (logger.isLogEnabled()) {
                List<Serializable> values = new ArrayList<>(orderedColumns.size());
                for (Column column : orderedColumns) {
                    Object value = filterMap.get(column.getKey());
                    values.add((Serializable) value);
                }
                addFilterValuesForLog(values);
                logger.logSQL(query, values);
            }

            try (PreparedStatement ps = sqlConnection.prepareStatement(query)) {
                fillPreparedStatementFields(filterMap, orderedColumns, ps);

                // execute the query and create a documentModel list
                DocumentModelList list = new DocumentModelListImpl();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {

                        // fetch values for stored fields
                        Map<String, Object> map = new HashMap<>();
                        for (Column column : getReadColumns()) {
                            Object o = getFieldValue(rs, column);
                            map.put(column.getKey(), o);
                        }

                        DocumentModel docModel = fieldMapToDocumentModel(map);

                        // fetch the reference fields
                        if (fetchReferences) {
                            Map<String, List<String>> targetIdsMap = new HashMap<>();
                            for (Reference reference : directory.getReferences()) {
                                List<String> targetIds = reference.getTargetIdsForSource(docModel.getId());
                                String fieldName = reference.getFieldName();
                                if (targetIdsMap.containsKey(fieldName)) {
                                    targetIdsMap.get(fieldName).addAll(targetIds);
                                } else {
                                    targetIdsMap.put(fieldName, targetIds);
                                }
                            }
                            for (Entry<String, List<String>> en : targetIdsMap.entrySet()) {
                                String fieldName = en.getKey();
                                List<String> targetIds = en.getValue();
                                docModel.setProperty(schemaName, fieldName, targetIds);
                            }
                        }
                        list.add(docModel);
                    }
                }
                if (manualLimitOffset) {
                    int totalSize = list.size();
                    if (offset > 0) {
                        if (offset >= totalSize) {
                            list = new DocumentModelListImpl();
                        } else {
                            list = new DocumentModelListImpl(list.subList(offset, totalSize));
                        }
                    }
                    if (list.size() > limit) { // list.size() not totalSize, we may have an offset already
                        list = new DocumentModelListImpl(list.subList(0, limit));
                    }
                    ((DocumentModelListImpl) list).setTotalSize(totalSize);
                }
                if (trucatedResults) {
                    ((DocumentModelListImpl) list).setTotalSize(-2);
                }
                return list;
            }

        } catch (SQLException e) {
            try {
                sqlConnection.close();
            } catch (SQLException e1) {
            }
            throw new DirectoryException("query failed", e);
        }
    }

    @Override
    protected DocumentModel createEntryWithoutReferences(Map<String, Object> fieldMap) {
        // Make a copy of fieldMap to avoid modifying it
        fieldMap = new HashMap<>(fieldMap);

        Map<String, Field> schemaFieldMap = directory.getSchemaFieldMap();
        Field schemaIdField = schemaFieldMap.get(getIdField());

        String idFieldName = schemaIdField.getName().getPrefixedName();

        acquireConnection();
        if (autoincrementId) {
            fieldMap.remove(idFieldName);
        } else {
            // check id that was given
            Object rawId = fieldMap.get(idFieldName);
            if (rawId == null) {
                throw new DirectoryException("Missing id");
            }

            String id = String.valueOf(rawId);
            if (isMultiTenant()) {
                String tenantId = getCurrentTenantId();
                if (!StringUtils.isBlank(tenantId)) {
                    fieldMap.put(TENANT_ID_FIELD, tenantId);
                    if (computeMultiTenantId) {
                        id = computeMultiTenantDirectoryId(tenantId, id);
                        fieldMap.put(idFieldName, id);
                    }
                }
            }

            if (hasEntry(id)) {
                throw new DirectoryException(String.format("Entry with id %s already exists", id));
            }
        }

        List<Column> columnList = new ArrayList<>(table.getColumns());
        Column idColumn = null;
        for (Iterator<Column> i = columnList.iterator(); i.hasNext();) {
            Column column = i.next();
            if (column.isIdentity()) {
                idColumn = column;
            }
            String prefixedName = schemaFieldMap.get(column.getKey()).getName().getPrefixedName();

            if (!fieldMap.containsKey(prefixedName)) {
                Field prefixedField = schemaFieldMap.get(prefixedName);
                if (prefixedField != null && prefixedField.getDefaultValue() != null) {
                    fieldMap.put(prefixedName, prefixedField.getDefaultValue());
                } else {
                    i.remove();
                }
            }
        }
        Insert insert = new Insert(table);
        for (Column column : columnList) {
            insert.addColumn(column);
        }
        // needed for Oracle for empty map insert
        insert.addIdentityColumn(idColumn);
        String sql = insert.getStatement();

        if (logger.isLogEnabled()) {
            List<Serializable> values = new ArrayList<>(columnList.size());
            for (Column column : columnList) {
                String prefixField = schemaFieldMap.get(column.getKey()).getName().getPrefixedName();
                Object value = fieldMap.get(prefixField);
                Serializable v;
                if (HIDE_PASSWORD_IN_LOGS && column.getKey().equals(getPasswordField())) {
                    v = "********"; // hide password in logs
                } else {
                    v = fieldValueForWrite(value, column);
                }
                values.add(v);
            }
            logger.logSQL(sql, values);
        }

        DocumentModel entry;
        try (PreparedStatement ps = prepareStatementWithAutoKeys(sql)) {

            int index = 1;
            for (Column column : columnList) {
                String prefixField = schemaFieldMap.get(column.getKey()).getName().getPrefixedName();
                Object value = fieldMap.get(prefixField);
                setFieldValue(ps, index, column, value);
                index++;
            }
            ps.execute();
            if (autoincrementId) {
                Column column = table.getColumn(getIdField());
                if (dialect.hasIdentityGeneratedKey()) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        setIdFieldInMap(rs, column, idFieldName, fieldMap);
                    }
                } else {
                    // needs specific statements
                    sql = dialect.getIdentityGeneratedKeySql(column);
                    try (Statement st = sqlConnection.createStatement()) {
                        try (ResultSet rs = st.executeQuery(sql)) {
                            setIdFieldInMap(rs, column, idFieldName, fieldMap);
                        }
                    }
                }
            }
            entry = fieldMapToDocumentModel(fieldMap);
        } catch (SQLException e) {
            checkConcurrentUpdate(e);
            throw new DirectoryException("createEntry failed", e);
        }

        return entry;
    }

    protected void setIdFieldInMap(ResultSet rs, Column column, String idFieldName, Map<String, Object> fieldMap)
            throws SQLException {
        if (!rs.next()) {
            throw new DirectoryException("Cannot get generated key");
        }
        if (logger.isLogEnabled()) {
            logger.logResultSet(rs, Collections.singletonList(column));
        }
        Serializable rawId = column.getFromResultSet(rs, 1);
        fieldMap.put(idFieldName, rawId);
    }

    /**
     * Create a {@link PreparedStatement} returning the id key if it is auto-incremented and dialect has identity
     * generated key ({@see Dialect#hasIdentityGeneratedKey}.
     *
     * @since 10.1
     */
    protected PreparedStatement prepareStatementWithAutoKeys(String sql) throws SQLException {
        if (autoincrementId && dialect.hasIdentityGeneratedKey()) {
            return sqlConnection.prepareStatement(sql, new String[] { getIdField() });
        } else {
            return sqlConnection.prepareStatement(sql);
        }
    }

    @Override
    protected List<String> updateEntryWithoutReferences(DocumentModel docModel) throws DirectoryException {
        acquireConnection();
        List<Column> storedColumnList = new LinkedList<>();
        List<String> referenceFieldList = new LinkedList<>();

        if (isMultiTenant()) {
            // can only update entry from the current tenant
            String tenantId = getCurrentTenantId();
            if (!StringUtils.isBlank(tenantId)) {
                String entryTenantId = (String) docModel.getProperty(schemaName, TENANT_ID_FIELD);
                if (StringUtils.isBlank(entryTenantId) || !entryTenantId.equals(tenantId)) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Trying to update entry '%s' not part of current tenant '%s'",
                                docModel.getId(), tenantId));
                    }
                    throw new OperationNotAllowedException("Operation not allowed in the current tenant context",
                            "label.directory.error.multi.tenant.operationNotAllowed", null);
                }
            }
        }

        // collect fields to update
        for (String fieldName : directory.getSchemaFieldMap().keySet()) {
            if (fieldName.equals(getIdField())) {
                continue;
            }
            Property prop = docModel.getPropertyObject(schemaName, fieldName);
            if (!prop.isDirty()) {
                continue;
            }
            if (fieldName.equals(getPasswordField()) && StringUtils.isEmpty((String) prop.getValue())) {
                // null/empty password means unchanged
                continue;
            }
            if (getDirectory().isReference(fieldName)) {
                referenceFieldList.add(fieldName);
            } else {
                storedColumnList.add(table.getColumn(fieldName));
            }
        }

        if (!storedColumnList.isEmpty()) {
            // update stored fields
            // String whereString = StringUtils.join(
            // storedFieldPredicateList.iterator(), ", ");
            // String sql = String.format("UPDATE %s SET %s WHERE %s = ?",
            // tableName, whereString,
            // primaryColumn);

            Update update = new Update(table);
            update.setUpdatedColumns(storedColumnList);
            String whereString = table.getPrimaryColumn().getQuotedName() + " = ?";
            update.setWhere(whereString);
            String sql = update.getStatement();

            if (logger.isLogEnabled()) {
                List<Serializable> values = new ArrayList<>(storedColumnList.size());
                for (Column column : storedColumnList) {
                    Object value = docModel.getProperty(schemaName, column.getKey());
                    if (HIDE_PASSWORD_IN_LOGS && column.getKey().equals(getPasswordField())) {
                        value = "********"; // hide password in logs
                    }
                    values.add((Serializable) value);
                }
                values.add(docModel.getId());
                logger.logSQL(sql, values);
            }

            try (PreparedStatement ps = sqlConnection.prepareStatement(sql)) {

                int index = 1;
                // TODO: how can I reset dirty fields?
                for (Column column : storedColumnList) {
                    Object value = docModel.getProperty(schemaName, column.getKey());
                    setFieldValue(ps, index, column, value);
                    index++;
                }
                setFieldValue(ps, index, table.getPrimaryColumn(), docModel.getId());
                ps.execute();
            } catch (SQLException e) {
                checkConcurrentUpdate(e);
                throw new DirectoryException("updateEntry failed for " + docModel.getId(), e);
            }
        }

        return referenceFieldList;
    }

    @Override
    public void deleteEntryWithoutReferences(String id) throws DirectoryException {
        // second step: clean stored fields
        Delete delete = new Delete(table);
        String whereString = table.getPrimaryColumn().getQuotedName() + " = ?";
        delete.setWhere(whereString);
        String sql = delete.getStatement();
        if (logger.isLogEnabled()) {
            logger.logSQL(sql, Collections.singleton(id));
        }
        try (PreparedStatement ps = sqlConnection.prepareStatement(sql)) {
            setFieldValue(ps, 1, table.getPrimaryColumn(), id);
            ps.execute();
        } catch (SQLException e) {
            checkConcurrentUpdate(e);
            throw new DirectoryException("deleteEntry failed", e);
        }
    }

    protected void fillPreparedStatementFields(Map<String, Object> filterMap, List<Column> orderedColumns,
            PreparedStatement ps) throws DirectoryException {
        int index = 1;
        for (Column column : orderedColumns) {
            Object value = filterMap.get(column.getKey());

            if (value instanceof SQLComplexFilter) {
                index = ((SQLComplexFilter) value).setFieldValue(ps, index, column);
            } else {
                setFieldValue(ps, index, column, value);
                index++;
            }
        }
        addFilterValues(ps, index);
    }

    private Object getFieldValue(ResultSet rs, Column column) throws DirectoryException {
        try {
            int index = rs.findColumn(column.getPhysicalName());
            return column.getFromResultSet(rs, index);
        } catch (SQLException e) {
            throw new DirectoryException("getFieldValue failed", e);
        }
    }

    private void setFieldValue(PreparedStatement ps, int index, Column column, Object value) throws DirectoryException {
        try {
            column.setToPreparedStatement(ps, index, fieldValueForWrite(value, column));
        } catch (SQLException e) {
            throw new DirectoryException("setFieldValue failed", e);
        }
    }

    protected Serializable fieldValueForWrite(Object value, Column column) {
        ColumnSpec spec = column.getType().spec;
        if (value instanceof String) {
            if (spec == ColumnSpec.LONG || spec == ColumnSpec.AUTOINC) {
                // allow storing string into integer/long key
                return Long.valueOf((String) value);
            }
            if (column.getKey().equals(getPasswordField())) {
                // hash password if not already hashed
                String password = (String) value;
                if (!PasswordHelper.isHashed(password)) {
                    password = PasswordHelper.hashPassword(password, passwordHashAlgorithm);
                }
                return password;
            }
        } else if (value instanceof Number) {
            if (spec == ColumnSpec.LONG || spec == ColumnSpec.AUTOINC) {
                // canonicalize to Long
                if (value instanceof Integer) {
                    return Long.valueOf(((Integer) value).longValue());
                }
            } else if (spec == ColumnSpec.STRING) {
                // allow storing number in string field
                return value.toString();
            }
        }
        return (Serializable) value;
    }

    @Override
    public void close() throws DirectoryException {
        try {
            if (!sqlConnection.isClosed()) {
                sqlConnection.close();
            }
        } catch (SQLException e) {
            throw new DirectoryException("close failed", e);
        } finally {
            getDirectory().removeSession(this);
        }
    }

    /**
     * Enable connection status checking on SQL directory connections
     *
     * @since 5.7.2
     */
    public boolean isLive() throws DirectoryException {
        try {
            return !sqlConnection.isClosed();
        } catch (SQLException e) {
            throw new DirectoryException("Cannot check connection status of " + this, e);
        }
    }

    @Override
    public boolean authenticate(String username, String password) {
        String storedPassword = getPassword(username);
        return PasswordHelper.verifyPassword(password, storedPassword);
    }

    @Override
    public boolean isAuthenticating() {
        return directory.getSchemaFieldMap().containsKey(getPasswordField());
    }

    @Override
    public boolean hasEntry(String id) {
        acquireConnection();
        Select select = new Select(table);
        select.setFrom(table.getQuotedName());
        select.setWhat("1");
        select.setWhere(table.getPrimaryColumn().getQuotedName() + " = ?");
        String sql = select.getStatement();

        if (logger.isLogEnabled()) {
            logger.logSQL(sql, Collections.singleton(id));
        }

        try (PreparedStatement ps = sqlConnection.prepareStatement(sql)) {
            setFieldValue(ps, 1, table.getPrimaryColumn(), id);
            try (ResultSet rs = ps.executeQuery()) {
                boolean has = rs.next();
                if (logger.isLogEnabled()) {
                    logger.logCount(has ? 1 : 0);
                }
                return has;
            }
        } catch (SQLException e) {
            throw new DirectoryException("hasEntry failed", e);
        }
    }

    @Override
    public String toString() {
        return "SQLSession [directory=" + directory.getName() + ", sid=" + sid + "]";
    }

}
