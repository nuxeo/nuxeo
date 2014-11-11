/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     George Lefter
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.sql;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.model.PropertyException;
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
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.EntrySource;
import org.nuxeo.ecm.directory.IdGenerator;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.ecm.directory.SizeLimitExceededException;

/**
 * This class represents a session against an SQLDirectory.
 */
public class SQLSession extends BaseSession implements EntrySource {

    private static final String READ_ONLY_VOCABULARY_WARN = "This SQLDirectory is ReadOnly, you are not allowed to modify it.";

    private static final Log log = LogFactory.getLog(SQLSession.class);

    protected final Map<String, Field> schemaFieldMap;

    protected final List<String> storedFieldNames;

    protected final Set<String> emptySet = Collections.emptySet();

    final String schemaName;

    final Table table;

    private final SQLDirectoryDescriptor.SubstringMatchType substringMatchType;

    String dataSourceName;

    final String idField;

    final String passwordField;

    final String passwordHashAlgorithm;

    IdGenerator idGenerator;

    final SQLDirectory directory;

    protected SQLStaticFilter[] staticFilters;

    String sid;

    Connection sqlConnection;

    private final boolean managedSQLSession;

    private final Dialect dialect;

    private JDBCLogger logger = new JDBCLogger("SQLDirectory");

    public SQLSession(SQLDirectory directory, SQLDirectoryDescriptor config,
            IdGenerator idGenerator, boolean managedSQLSession)
            throws DirectoryException {
        this.directory = directory;
        this.schemaName = config.getSchemaName();
        this.table = directory.getTable();
        this.idField = config.getIdField();
        this.passwordField = config.getPasswordField();
        this.passwordHashAlgorithm = config.passwordHashAlgorithm;
        this.schemaFieldMap = directory.getSchemaFieldMap();
        this.storedFieldNames = directory.getStoredFieldNames();
        this.dialect = directory.getDialect();
        acquireConnection();

        this.sid = String.valueOf(SIDGenerator.next());
        this.managedSQLSession = managedSQLSession;
        this.substringMatchType = config.getSubstringMatchType();
        this.idGenerator = idGenerator;
        this.staticFilters = config.getStaticFilters();
    }

    public Directory getDirectory() {
        return directory;
    }

    protected DocumentModel fieldMapToDocumentModel(Map<String, Object> fieldMap) {
        String id = String.valueOf(fieldMap.get(getIdField()));
        try {
            DocumentModel docModel = BaseSession.createEntryModel(sid,
                    schemaName, id, fieldMap, isReadOnly());
            return docModel;
        } catch (PropertyException e) {
            log.error(e, e);
            return null;
        }
    }

    private void acquireConnection() throws DirectoryException {
        try {
            if (sqlConnection == null || sqlConnection.isClosed()) {
                sqlConnection = directory.getDataSource().getConnection();
            }
        } catch (SQLException e) {
            throw new DirectoryException("updateConnection failed", e);
        }
    }

    @Override
    public DocumentModel createEntry(Map<String, Object> fieldMap)
            throws ClientException {

        if (isReadOnly()) {
            log.warn(READ_ONLY_VOCABULARY_WARN);
        }
        acquireConnection();
        if (idGenerator != null) {
            fieldMap.put(idField, Integer.valueOf(idGenerator.nextId()));
        } else {
            // check id that was given
            Object rawId = fieldMap.get(idField);
            if (rawId == null) {
                throw new DirectoryException("Missing id");
            }
            String id = String.valueOf(rawId);
            if (hasEntry(id)) {
                throw new DirectoryException(String.format(
                        "Entry with id %s already exists", id));
            }
        }

        // first step: insert stored fields
        // String columnList = StringUtils.join(storedFieldNames.iterator(), ",
        // ");
        // String valueList = StringUtils.join(Collections.nCopies(
        // storedFieldNames.size(), "?").iterator(), ", ");

        // String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
        // tableName, columnList, valueList);
        List<Column> columnList = new ArrayList<Column>(table.getColumns());
        for (Iterator<Column> i = columnList.iterator(); i.hasNext();) {
            Column column = i.next();
            if (fieldMap.get(column.getKey()) == null) {
                i.remove();
            }
        }
        Insert insert = new Insert(table);
        for (Column column : columnList) {
            insert.addColumn(column);
        }
        String sql = insert.getStatement();

        if (logger.isLogEnabled()) {
            List<Serializable> values = new ArrayList<Serializable>(
                    columnList.size());
            for (Column column : columnList) {
                Object value = fieldMap.get(column.getKey());
                values.add(fieldValueForWrite(value, column));
            }
            logger.logSQL(sql, values);
        }

        DocumentModel entry;
        PreparedStatement ps = null;
        try {
            ps = sqlConnection.prepareStatement(sql);
            int index = 1;
            for (Column column : columnList) {
                Object value = fieldMap.get(column.getKey());
                setFieldValue(ps, index, column, value);
                index++;
            }
            ps.execute();
            entry = fieldMapToDocumentModel(fieldMap);
        } catch (SQLException e) {
            throw new DirectoryException("createEntry failed", e);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException sqle) {
                throw new DirectoryException(sqle);
            }
        }

        // second step: add references fields
        String sourceId = entry.getId();
        for (Reference reference : getDirectory().getReferences()) {
            @SuppressWarnings("unchecked")
            List<String> targetIds = (List<String>) fieldMap.get(reference.getFieldName());
            if (reference instanceof TableReference) {
                // optim: reuse the current session
                // but still initialize the reference if not yet done
                TableReference tableReference = (TableReference) reference;
                tableReference.maybeInitialize(this);
                tableReference.addLinks(sourceId, targetIds, this);
            } else {
                reference.addLinks(sourceId, targetIds);
            }
        }
        directory.invalidateCaches();
        return entry;
    }

    @Override
    public DocumentModel getEntry(String id) throws DirectoryException {
        return getEntry(id, true);
    }

    @Override
    public DocumentModel getEntry(String id, boolean fetchReferences)
            throws DirectoryException {
        return directory.getCache().getEntry(id, this, fetchReferences);
    }

    protected String addFilterWhereClause(String whereClause)
            throws DirectoryException {
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
            whereClause += filter.getDirectoryColumn(table,
                    directory.useNativeCase()).getQuotedName();
            whereClause += " " + filter.getOperator() + " ";
            whereClause += "? ";

            if (i < staticFilters.length - 1) {
                whereClause = whereClause + " AND ";
            }
        }
        return whereClause;
    }

    protected void addFilterValues(PreparedStatement ps, int startIdx)
            throws DirectoryException {
        for (int i = 0; i < staticFilters.length; i++) {
            SQLStaticFilter filter = staticFilters[i];
            setFieldValue(
                    ps,
                    startIdx + i,
                    filter.getDirectoryColumn(table, directory.useNativeCase()),
                    filter.getValue());
        }
    }

    protected void addFilterValuesForLog(List<Serializable> values) {
        for (int i = 0; i < staticFilters.length; i++) {
            values.add(staticFilters[i].getValue());
        }
    }

    @Override
    public DocumentModel getEntryFromSource(String id, boolean fetchReferences)
            throws DirectoryException {
        acquireConnection();
        // String sql = String.format("SELECT * FROM %s WHERE %s = ?",
        // tableName, idField);
        Select select = new Select(table);
        select.setFrom(table.getQuotedName());
        select.setWhat("*");

        String whereClause = table.getPrimaryColumn().getQuotedName() + " = ?";
        whereClause = addFilterWhereClause(whereClause);

        select.setWhere(whereClause);
        String sql = select.getStatement();

        if (logger.isLogEnabled()) {
            List<Serializable> values = new ArrayList<Serializable>();
            values.add(id);
            addFilterValuesForLog(values);
            logger.logSQL(sql, values);
        }

        PreparedStatement ps = null;
        try {
            ps = sqlConnection.prepareStatement(sql);
            setFieldValue(ps, 1, table.getPrimaryColumn(), id);
            addFilterValues(ps, 2);

            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return null;
            }

            // fetch the stored fields
            Map<String, Object> fieldMap = new HashMap<String, Object>();
            for (String fieldName : storedFieldNames) {
                Object value = getFieldValue(rs, fieldName);
                fieldMap.put(fieldName, value);
            }

            DocumentModel entry = fieldMapToDocumentModel(fieldMap);
            // fetch the reference fields
            if (fetchReferences) {
                for (Reference reference : directory.getReferences()) {
                    List<String> targetIds = reference.getTargetIdsForSource(entry.getId());
                    try {
                        entry.setProperty(schemaName, reference.getFieldName(),
                                targetIds);
                    } catch (ClientException e) {
                        throw new DirectoryException(e);
                    }
                }
            }
            return entry;
        } catch (SQLException e) {
            throw new DirectoryException("getEntry failed", e);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException sqle) {
                throw new DirectoryException(sqle);
            }
        }
    }

    @Override
    public DocumentModelList getEntries() throws ClientException {
        Map<String, Serializable> emptyMap = Collections.emptyMap();
        return query(emptyMap);
    }

    @Override
    public void updateEntry(DocumentModel docModel) throws ClientException {

        if (isReadOnly()) {
            log.warn(READ_ONLY_VOCABULARY_WARN);
            return;
        }

        acquireConnection();
        List<Column> storedColumnList = new LinkedList<Column>();
        List<String> referenceFieldList = new LinkedList<String>();
        DataModel dataModel = docModel.getDataModel(schemaName);

        // collect fields to update
        for (String fieldName : schemaFieldMap.keySet()) {
            if (fieldName.equals(idField)) {
                continue;
            }
            if (!dataModel.isDirty(fieldName)) {
                continue;
            }
            if (directory.isReference(fieldName)) {
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
            String whereString = table.getPrimaryColumn().getQuotedName()
                    + " = ?";
            update.setWhere(whereString);
            String sql = update.getStatement();

            if (logger.isLogEnabled()) {
                List<Serializable> values = new ArrayList<Serializable>(
                        storedColumnList.size());
                for (Column column : storedColumnList) {
                    Object value = dataModel.getData(column.getKey());
                    values.add((Serializable) value);
                }
                values.add(docModel.getId());
                logger.logSQL(sql, values);
            }

            PreparedStatement ps = null;
            try {
                ps = sqlConnection.prepareStatement(sql);

                int index = 1;
                // TODO: how can I reset dirty fields?
                for (Column column : storedColumnList) {
                    Object value = dataModel.getData(column.getKey());
                    setFieldValue(ps, index, column, value);
                    index++;
                }
                setFieldValue(ps, index, table.getPrimaryColumn(),
                        docModel.getId());
                ps.execute();
            } catch (SQLException e) {
                throw new DirectoryException("updateEntry failed for "
                        + docModel.getId(), e);
            } finally {
                try {
                    if (ps != null) {
                        ps.close();
                    }
                } catch (SQLException sqle) {
                    throw new DirectoryException(sqle);
                }
            }
        }

        // update reference fields
        for (String referenceFieldName : referenceFieldList) {
            Reference reference = directory.getReference(referenceFieldName);
            @SuppressWarnings("unchecked")
            List<String> targetIds = (List<String>) docModel.getProperty(
                    schemaName, referenceFieldName);
            if (reference instanceof TableReference) {
                // optim: reuse current session
                TableReference tableReference = (TableReference) reference;
                tableReference.setTargetIdsForSource(docModel.getId(),
                        targetIds, this);
            } else {
                reference.setTargetIdsForSource(docModel.getId(), targetIds);
            }
        }
        directory.invalidateCaches();
    }

    @Override
    public void deleteEntry(DocumentModel docModel) throws ClientException {
        deleteEntry(docModel.getId());
    }

    @Override
    public void deleteEntry(String id) throws ClientException {
        acquireConnection();

        if (isReadOnly()) {
            log.warn(READ_ONLY_VOCABULARY_WARN);
            return;
        }

        // first step: remove references for this entry
        for (Reference reference : getDirectory().getReferences()) {
            if (reference instanceof TableReference) {
                // optim: reuse current session
                TableReference tableReference = (TableReference) reference;
                tableReference.removeLinksForSource(id, this);
            } else {
                reference.removeLinksForSource(id);
            }
        }

        // second step: clean stored fields
        PreparedStatement ps = null;
        try {
            Delete delete = new Delete(table);
            String whereString = table.getPrimaryColumn().getQuotedName()
                    + " = ?";
            delete.setWhere(whereString);
            String sql = delete.getStatement();
            if (logger.isLogEnabled()) {
                logger.logSQL(sql, Collections.<Serializable> singleton(id));
            }
            ps = sqlConnection.prepareStatement(sql);
            setFieldValue(ps, 1, table.getPrimaryColumn(), id);
            ps.execute();
        } catch (SQLException e) {
            throw new DirectoryException("deleteEntry failed", e);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException sqle) {
                throw new DirectoryException(sqle);
            }
        }
        directory.invalidateCaches();
    }

    @Override
    public void deleteEntry(String id, Map<String, String> map)
            throws DirectoryException {

        if (isReadOnly()) {
            log.warn(READ_ONLY_VOCABULARY_WARN);
            return;
        }

        acquireConnection();

        // Assume in this case that there are no References to this entry.
        PreparedStatement ps = null;
        try {
            Delete delete = new Delete(table);
            StringBuilder whereClause = new StringBuilder();
            List<Serializable> values = new ArrayList<Serializable>(
                    1 + map.size());

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

            ps = sqlConnection.prepareStatement(sql);
            for (int i = 0; i < values.size(); i++) {
                if (i == 0) {
                    setFieldValue(ps, 1, table.getPrimaryColumn(),
                            values.get(i));
                } else {
                    ps.setString(1 + i, (String) values.get(i));
                }
            }
            ps.execute();
        } catch (SQLException e) {
            throw new DirectoryException("deleteEntry failed", e);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException sqle) {
                throw new DirectoryException(sqle);
            }
        }
        directory.invalidateCaches();
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter,
            Set<String> fulltext, Map<String, String> orderBy)
            throws ClientException {
        // XXX not fetch references by default: breaks current behavior
        return query(filter, fulltext, orderBy, false);
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter,
            Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences) throws ClientException {
        acquireConnection();
        Map<String, Object> filterMap = new LinkedHashMap<String, Object>(
                filter);
        try {
            // build count query statement
            StringBuilder whereClause = new StringBuilder();
            String separator = "";
            List<Column> orderedColumns = new LinkedList<Column>();
            for (String columnName : filterMap.keySet()) {

                if (directory.isReference(columnName)) {
                    log.warn(columnName + " is a reference and will be ignored"
                            + " as a query criterion");
                    continue;
                }

                Object value = filterMap.get(columnName);
                Column column = table.getColumn(columnName);
                if (null == column) {
                    // this might happen if we have a case like a chain
                    // selection and a directory without parent column
                    throw new ClientException("cannot find column '"
                            + columnName + "' for table: " + table);
                }
                String leftSide = column.getQuotedName();
                String operator;
                if ("".equals(value) && dialect.hasNullEmptyString()) {
                    // see NXP-6172, empty values are Null in Oracle
                    value = null;
                }
                if (value != null) {
                    if (fulltext != null && fulltext.contains(columnName)) {
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
                    whereClause.append('?');
                    orderedColumns.add(column);
                }
                separator = " AND ";
            }

            int queryLimitSize = directory.getConfig().getQuerySizeLimit();
            if (queryLimitSize != 0) {
                PreparedStatement ps = null;
                try {
                    // create a preparedStatement for counting and bind the
                    // values
                    // String countQuery = new StringBuilder("SELECT count(*)
                    // FROM ")
                    // .append(table.getQuotedName(dialect)).append(whereClause).toString();
                    Select select = new Select(table);
                    select.setWhat("count(*)");
                    select.setFrom(table.getQuotedName());

                    String where = whereClause.toString();
                    where = addFilterWhereClause(where);
                    select.setWhere(where);

                    String countQuery = select.getStatement();
                    ps = sqlConnection.prepareStatement(countQuery);
                    int index = 1;
                    for (Column column : orderedColumns) {
                        Object value = filterMap.get(column.getKey());
                        setFieldValue(ps, index, column, value);
                        index++;
                    }
                    addFilterValues(ps, index);
                    ResultSet rs = ps.executeQuery();
                    rs.next();
                    int count = rs.getInt(1);
                    if (count > queryLimitSize) {
                        throw new SizeLimitExceededException(
                                "too many rows in result: " + count);
                    }
                } finally {
                    if (ps != null) {
                        ps.close();
                    }
                }
            }

            // create a preparedStatement and bind the values
            // String query = new StringBuilder("SELECT * FROM
            // ").append(tableName).append(
            // whereClause).toString();

            Select select = new Select(table);
            select.setWhat("*");
            select.setFrom(table.getQuotedName());

            String where = whereClause.toString();
            where = addFilterWhereClause(where);
            select.setWhere(where);

            StringBuilder orderby = new StringBuilder(128);
            if (orderBy != null) {
                for (Iterator<Map.Entry<String, String>> it = orderBy.entrySet().iterator(); it.hasNext();) {
                    Entry<String, String> entry = it.next();
                    orderby.append(dialect.openQuote()).append(entry.getKey()).append(
                            dialect.closeQuote()).append(' ').append(
                            entry.getValue());
                    if (it.hasNext()) {
                        orderby.append(',');
                    }
                }
            }
            select.setOrderBy(orderby.toString());
            String query = select.getStatement();

            if (logger.isLogEnabled()) {
                List<Serializable> values = new ArrayList<Serializable>(
                        orderedColumns.size());
                for (Column column : orderedColumns) {
                    Object value = filterMap.get(column.getKey());
                    values.add((Serializable) value);
                }
                addFilterValuesForLog(values);
                logger.logSQL(query, values);
            }

            PreparedStatement ps = null;
            try {
                ps = sqlConnection.prepareStatement(query);
                int index = 1;
                for (Column column : orderedColumns) {
                    Object value = filterMap.get(column.getKey());
                    setFieldValue(ps, index, column, value);
                    index++;
                }
                addFilterValues(ps, index);

                // execute the query and create a documentModel list
                ResultSet rs = ps.executeQuery();
                DocumentModelList list = new DocumentModelListImpl();
                while (rs.next()) {

                    // fetch values for stored fields
                    Map<String, Object> map = new HashMap<String, Object>();
                    for (String fieldName : storedFieldNames) {
                        Object o = getFieldValue(rs, fieldName);
                        map.put(fieldName, o);
                    }

                    DocumentModel docModel = fieldMapToDocumentModel(map);

                    // fetch the reference fields
                    if (fetchReferences) {
                        for (Reference reference : directory.getReferences()) {
                            List<String> targetIds = reference.getTargetIdsForSource(docModel.getId());
                            docModel.setProperty(schemaName,
                                    reference.getFieldName(), targetIds);
                        }
                    }
                    list.add(docModel);
                }
                return list;
            } finally {
                if (ps != null) {
                    ps.close();
                }
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
    public DocumentModelList query(Map<String, Serializable> filter)
            throws ClientException {
        return query(filter, emptySet);
    }

    private Object getFieldValue(ResultSet rs, String fieldName)
            throws DirectoryException {
        try {
            Column column = table.getColumn(fieldName);
            if (column == null) {
                throw new DirectoryException(String.format(
                        "Column '%s' does not exist in table '%s'", fieldName,
                        table.getKey()));
            }
            int index = rs.findColumn(column.getPhysicalName());
            return column.getFromResultSet(rs, index);
        } catch (SQLException e) {
            throw new DirectoryException("getFieldValue failed", e);
        }
    }

    private void setFieldValue(PreparedStatement ps, int index, Column column,
            Object value) throws DirectoryException {
        try {
            column.setToPreparedStatement(ps, index,
                    fieldValueForWrite(value, column));
        } catch (SQLException e) {
            throw new DirectoryException("setFieldValue failed", e);
        }
    }

    protected Serializable fieldValueForWrite(Object value, Column column) {
        if (value instanceof String) {
            if (column.getType().spec == ColumnSpec.LONG) {
                // allow storing string into integer/long key
                return Long.valueOf((String) value);
            }
            if (column.getKey().equals(passwordField)) {
                // hash password if not already hashed
                String password = (String) value;
                if (!PasswordHelper.isHashed(password)) {
                    password = PasswordHelper.hashPassword(password,
                            passwordHashAlgorithm);
                }
                return password;
            }
        } else if (value instanceof Number) {
            if (column.getType().spec == ColumnSpec.LONG) {
                // canonicalize to Long
                if (value instanceof Integer) {
                    return Long.valueOf(((Integer) value).longValue());
                }
            } else if (column.getType().spec == ColumnSpec.STRING) {
                // allow storing number in string field
                return value.toString();
            }
        }
        return (Serializable) value;
    }

    @Override
    public void commit() throws DirectoryException {
        // TODO: cannot commit during a managed transaction !!
        try {
            if (!managedSQLSession) {
                sqlConnection.commit();
            }
        } catch (SQLException e) {
            throw new DirectoryException("commit failed", e);
        }
    }

    @Override
    public void rollback() throws DirectoryException {
        try {
            sqlConnection.rollback();
        } catch (SQLException e) {
            throw new DirectoryException("rollback failed", e);
        }
    }

    @Override
    public void close() throws DirectoryException {
        try {
            sqlConnection.close();
            directory.removeSession(this);
        } catch (SQLException e) {
            throw new DirectoryException("close failed", e);
        }
    }

    @Override
    public List<String> getProjection(Map<String, Serializable> filter,
            Set<String> fulltext, String columnName) throws ClientException {
        DocumentModelList docList = query(filter, fulltext);
        List<String> result = new ArrayList<String>();
        for (DocumentModel docModel : docList) {
            Object obj = docModel.getProperty(schemaName, columnName);
            String propValue;
            if (obj instanceof String) {
                propValue = (String) obj;
            } else {
                propValue = String.valueOf(obj);
            }
            result.add(propValue);
        }
        return result;
    }

    @Override
    public List<String> getProjection(Map<String, Serializable> filter,
            String columnName) throws ClientException {
        return getProjection(filter, emptySet, columnName);
    }

    @Override
    public boolean authenticate(String username, String password)
            throws ClientException {
        DocumentModel entry = getEntry(username);
        if (entry == null) {
            return false;
        }
        String storedPassword = (String) entry.getProperty(schemaName,
                getPasswordField());
        return PasswordHelper.verifyPassword(password, storedPassword);
    }

    @Override
    public boolean isAuthenticating() throws ClientException {
        return schemaFieldMap.containsKey(getPasswordField());
    }

    @Override
    public String getIdField() {
        return directory.getConfig().getIdField();
    }

    @Override
    public String getPasswordField() {
        return directory.getConfig().getPasswordField();
    }

    @Override
    public boolean isReadOnly() {
        return Boolean.TRUE.equals(directory.getConfig().getReadOnly());
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter,
            Set<String> fulltext) throws ClientException {
        return query(filter, fulltext, new HashMap<String, String>());
    }

    @Override
    public DocumentModel createEntry(DocumentModel entry)
            throws ClientException {
        Map<String, Object> fieldMap = entry.getProperties(schemaName);
        return createEntry(fieldMap);
    }

    @Override
    public boolean hasEntry(String id) throws ClientException {
        acquireConnection();
        Select select = new Select(table);
        select.setFrom(table.getQuotedName());
        select.setWhat("*");
        select.setWhere(table.getPrimaryColumn().getQuotedName() + " = ?");
        String sql = select.getStatement();

        if (logger.isLogEnabled()) {
            logger.logSQL(sql, Collections.<Serializable> singleton(id));
        }

        PreparedStatement ps = null;
        try {
            ps = sqlConnection.prepareStatement(sql);
            setFieldValue(ps, 1, table.getPrimaryColumn(), id);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new DirectoryException("hasEntry failed", e);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException sqle) {
                throw new DirectoryException(sqle);
            }
        }
    }

    /**
     * Public getter to allow custom {@link Reference} implementation to access
     * the current connection even if it lives in a separate java package,
     * typically: com.company.custom.nuxeo.project.MyCustomReference
     *
     * @return the current {@link Connection} instance
     */
    public Connection getSqlConnection() {
        return sqlConnection;
    }

}
