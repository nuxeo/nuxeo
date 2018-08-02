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
 *     Olivier Grisel
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.sql;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.nuxeo.ecm.core.schema.types.SchemaImpl;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Delete;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Insert;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Select;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table.IndexType;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.ecm.directory.AbstractReference;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryCSVLoader;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.ReferenceDescriptor;
import org.nuxeo.ecm.directory.Session;

public class TableReference extends AbstractReference {

    protected String tableName;

    protected String sourceColumn;

    protected String targetColumn;

    protected String dataFileName;

    private Table table;

    private Dialect dialect;

    private boolean initialized = false;

    /**
     * @since 9.2
     */
    public TableReference(String fieldName, String directory, String tableName, String sourceColumn,
            String targetColumn, String dataFileName) {
        super(fieldName, directory);
        this.tableName = tableName;
        this.sourceColumn = sourceColumn;
        this.targetColumn = targetColumn;
        this.dataFileName = dataFileName;
    }

    /**
     * @since 9.2
     */
    public TableReference(TableReferenceDescriptor descriptor) {
        this(descriptor.getFieldName(), descriptor.getTargetDirectoryName(), descriptor.getTableName(),
                descriptor.getSourceColumn(), descriptor.getTargetColumn(), descriptor.getDataFileName());
    }

    /**
     * @since 9.2
     */
    public TableReference(ReferenceDescriptor descriptor) {
        this(descriptor.getFieldName(), descriptor.getDirectory(), descriptor.getReferenceName(),
                descriptor.getSource(), descriptor.getTarget(), descriptor.getDataFileName());
    }

    private SQLDirectory getSQLSourceDirectory() {
        Directory dir = getSourceDirectory();
        return (SQLDirectory) dir;
    }

    private void initialize(SQLSession sqlSession) {
        Connection connection = sqlSession.sqlConnection;
        SQLDirectory directory = getSQLSourceDirectory();
        Table table = getTable();
        SQLHelper helper = new SQLHelper(connection, table, directory.getDescriptor().getCreateTablePolicy());
        boolean loadData = helper.setupTable();
        if (loadData && dataFileName != null) {
            // fake schema for DirectoryCSVLoader.loadData
            SchemaImpl schema = new SchemaImpl(tableName, null);
            schema.addField(sourceColumn, StringType.INSTANCE, null, 0, Collections.emptySet());
            schema.addField(targetColumn, StringType.INSTANCE, null, 0, Collections.emptySet());
            Insert insert = new Insert(table);
            for (Column column : table.getColumns()) {
                insert.addColumn(column);
            }
            try (PreparedStatement ps = connection.prepareStatement(insert.getStatement())) {
                Consumer<Map<String, Object>> loader = new Consumer<Map<String, Object>>() {
                    @Override
                    public void accept(Map<String, Object> map) {
                        try {
                            ps.setString(1, (String) map.get(sourceColumn));
                            ps.setString(2, (String) map.get(targetColumn));
                            ps.execute();
                        } catch (SQLException e) {
                            throw new DirectoryException(e);
                        }
                    }
                };
                DirectoryCSVLoader.loadData(dataFileName, BaseDirectoryDescriptor.DEFAULT_DATA_FILE_CHARACTER_SEPARATOR,
                        schema, loader);
            } catch (SQLException e) {
                throw new DirectoryException(String.format("Table '%s' initialization failed", tableName), e);
            }
        }
    }

    @Override
    public void addLinks(String sourceId, List<String> targetIds) {
        if (targetIds == null) {
            return;
        }
        try (SQLSession session = getSQLSession()) {
            addLinks(sourceId, targetIds, session);
        }
    }

    @Override
    public void addLinks(List<String> sourceIds, String targetId) {
        if (sourceIds == null) {
            return;
        }
        try (SQLSession session = getSQLSession()) {
            addLinks(sourceIds, targetId, session);
        }
    }

    @Override
    public void addLinks(String sourceId, List<String> targetIds, Session session) {
        if (targetIds == null) {
            return;
        }
        SQLSession sqlSession = (SQLSession) session;
        maybeInitialize(sqlSession);
        for (String targetId : targetIds) {
            addLink(sourceId, targetId, sqlSession, true);
        }
    }

    @Override
    public void addLinks(List<String> sourceIds, String targetId, Session session) {
        if (sourceIds == null) {
            return;
        }
        SQLSession sqlSession = (SQLSession) session;
        maybeInitialize(sqlSession);
        for (String sourceId : sourceIds) {
            addLink(sourceId, targetId, sqlSession, true);
        }
    }

    public boolean exists(String sourceId, String targetId, SQLSession session) {
        // "SELECT COUNT(*) FROM %s WHERE %s = ? AND %s = ?", tableName, sourceColumn, targetColumn

        Table table = getTable();
        Select select = new Select(table);
        select.setFrom(table.getQuotedName());
        select.setWhat("count(*)");
        String whereString = String.format("%s = ? and %s = ?", table.getColumn(sourceColumn).getQuotedName(),
                table.getColumn(targetColumn).getQuotedName());

        select.setWhere(whereString);

        String selectSql = select.getStatement();
        if (session.logger.isLogEnabled()) {
            session.logger.logSQL(selectSql, Arrays.<Serializable> asList(sourceId, targetId));
        }

        try (PreparedStatement ps = session.sqlConnection.prepareStatement(selectSql)) {
            ps.setString(1, sourceId);
            ps.setString(2, targetId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DirectoryException(String.format("error reading link from %s to %s", sourceId, targetId), e);
        }
    }

    public void addLink(String sourceId, String targetId, SQLSession session, boolean checkExisting) {
        // OG: the following query should have avoided the round trips but
        // does not work for some reason that might be related to a bug in the
        // JDBC driver:
        // "INSERT INTO %s (%s, %s) (SELECT ?, ? FROM %s WHERE %s = ? AND %s =
        // ? HAVING COUNT(*) = 0)", tableName, sourceColumn, targetColumn,
        // tableName, sourceColumn, targetColumn

        // first step: check that this link does not exist yet
        if (checkExisting && exists(sourceId, targetId, session)) {
            return;
        }

        // second step: add the link

        // "INSERT INTO %s (%s, %s) VALUES (?, ?)", tableName, sourceColumn, targetColumn
        Table table = getTable();
        Insert insert = new Insert(table);
        insert.addColumn(table.getColumn(sourceColumn));
        insert.addColumn(table.getColumn(targetColumn));
        String insertSql = insert.getStatement();
        if (session.logger.isLogEnabled()) {
            session.logger.logSQL(insertSql, Arrays.<Serializable> asList(sourceId, targetId));
        }

        try (PreparedStatement ps = session.sqlConnection.prepareStatement(insertSql)) {
            ps.setString(1, sourceId);
            ps.setString(2, targetId);
            ps.execute();
        } catch (SQLException e) {
            throw new DirectoryException(String.format("error adding link from %s to %s", sourceId, targetId), e);
        }
    }

    protected List<String> getIdsFor(String valueColumn, String filterColumn, String filterValue) {
        try (SQLSession session = getSQLSession()) {
            // "SELECT %s FROM %s WHERE %s = ?", table.getColumn(valueColumn), tableName, filterColumn
            Table table = getTable();
            Select select = new Select(table);
            select.setWhat(table.getColumn(valueColumn).getQuotedName());
            select.setFrom(table.getQuotedName());
            select.setWhere(table.getColumn(filterColumn).getQuotedName() + " = ?");

            String sql = select.getStatement();
            if (session.logger.isLogEnabled()) {
                session.logger.logSQL(sql, Collections.<Serializable> singleton(filterValue));
            }

            List<String> ids = new LinkedList<String>();
            try (PreparedStatement ps = session.sqlConnection.prepareStatement(sql)) {
                ps.setString(1, filterValue);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ids.add(rs.getString(valueColumn));
                    }
                    return ids;
                }
            } catch (SQLException e) {
                throw new DirectoryException("error fetching reference values: ", e);
            }
        }
    }

    @Override
    public List<String> getSourceIdsForTarget(String targetId) {
        return getIdsFor(sourceColumn, targetColumn, targetId);
    }

    @Override
    public List<String> getTargetIdsForSource(String sourceId) {
        return getIdsFor(targetColumn, sourceColumn, sourceId);
    }

    public void removeLinksFor(String column, String entryId, SQLSession session) {
        Table table = getTable();
        String sql = String.format("DELETE FROM %s WHERE %s = ?", table.getQuotedName(), table.getColumn(column)
                                                                                              .getQuotedName());
        if (session.logger.isLogEnabled()) {
            session.logger.logSQL(sql, Collections.<Serializable> singleton(entryId));
        }
        try (PreparedStatement ps = session.sqlConnection.prepareStatement(sql)) {
            ps.setString(1, entryId);
            ps.execute();
        } catch (SQLException e) {
            throw new DirectoryException("error remove links to " + entryId, e);
        }
    }

    @Override
    public void removeLinksForSource(String sourceId, Session session) {
        SQLSession sqlSession = (SQLSession) session;
        maybeInitialize(sqlSession);
        removeLinksFor(sourceColumn, sourceId, sqlSession);
    }

    @Override
    public void removeLinksForTarget(String targetId, Session session) {
        SQLSession sqlSession = (SQLSession) session;
        maybeInitialize(sqlSession);
        removeLinksFor(targetColumn, targetId, sqlSession);
    }

    @Override
    public void removeLinksForSource(String sourceId) {
        try (SQLSession session = getSQLSession()) {
            removeLinksForSource(sourceId, session);
        }
    }

    @Override
    public void removeLinksForTarget(String targetId) {
        try (SQLSession session = getSQLSession()) {
            removeLinksForTarget(targetId, session);
        }
    }

    public void setIdsFor(String idsColumn, List<String> ids, String filterColumn, String filterValue,
            SQLSession session) {

        List<String> idsToDelete = new LinkedList<String>();
        Set<String> idsToAdd = new HashSet<String>();
        if (ids != null) { // ids may be null
            idsToAdd.addAll(ids);
        }
        Table table = getTable();

        // iterate over existing links to find what to add and what to remove
        String selectSql = String.format("SELECT %s FROM %s WHERE %s = ?", table.getColumn(idsColumn).getQuotedName(),
                table.getQuotedName(), table.getColumn(filterColumn).getQuotedName());
        try (PreparedStatement ps = session.sqlConnection.prepareStatement(selectSql)) {
            ps.setString(1, filterValue);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String existingId = rs.getString(1);
                    if (idsToAdd.contains(existingId)) {
                        // to not add already existing ids
                        idsToAdd.remove(existingId);
                    } else {
                        // delete unwanted existing ids
                        idsToDelete.add(existingId);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DirectoryException("failed to fetch existing links for " + filterValue, e);
        }

        if (!idsToDelete.isEmpty()) {
            // remove unwanted links

            // "DELETE FROM %s WHERE %s = ? AND %s = ?", tableName, filterColumn, idsColumn);
            Delete delete = new Delete(table);
            String whereString = String.format("%s = ? AND %s = ?", table.getColumn(filterColumn).getQuotedName(),
                    table.getColumn(idsColumn).getQuotedName());
            delete.setWhere(whereString);
            String deleteSql = delete.getStatement();

            try (PreparedStatement ps = session.sqlConnection.prepareStatement(deleteSql)) {
                for (String unwantedId : idsToDelete) {
                    if (session.logger.isLogEnabled()) {
                        session.logger.logSQL(deleteSql, Arrays.<Serializable> asList(filterValue, unwantedId));
                    }
                    ps.setString(1, filterValue);
                    ps.setString(2, unwantedId);
                    ps.execute();
                }
            } catch (SQLException e) {
                throw new DirectoryException("failed to remove unwanted links for " + filterValue, e);
            }
        }

        if (!idsToAdd.isEmpty()) {
            // add missing links
            if (filterColumn.equals(sourceColumn)) {
                for (String missingId : idsToAdd) {
                    addLink(filterValue, missingId, session, false);
                }
            } else {
                for (String missingId : idsToAdd) {
                    addLink(missingId, filterValue, session, false);
                }
            }
        }
    }

    public void setSourceIdsForTarget(String targetId, List<String> sourceIds, SQLSession session) {
        setIdsFor(sourceColumn, sourceIds, targetColumn, targetId, session);
    }

    public void setTargetIdsForSource(String sourceId, List<String> targetIds, SQLSession session) {
        setIdsFor(targetColumn, targetIds, sourceColumn, sourceId, session);
    }

    @Override
    public void setSourceIdsForTarget(String targetId, List<String> sourceIds) {
        try (SQLSession session = getSQLSession()) {
            setSourceIdsForTarget(targetId, sourceIds, session);
        }
    }

    @Override
    public void setSourceIdsForTarget(String targetId, List<String> sourceIds, Session session) {
        SQLSession sqlSession = (SQLSession) session;
        maybeInitialize(sqlSession);
        setSourceIdsForTarget(targetId, sourceIds, sqlSession);
    }

    @Override
    public void setTargetIdsForSource(String sourceId, List<String> targetIds) {
        try (SQLSession session = getSQLSession()) {
            setTargetIdsForSource(sourceId, targetIds, session);
        }
    }

    @Override
    public void setTargetIdsForSource(String sourceId, List<String> targetIds, Session session) {
        SQLSession sqlSession = (SQLSession) session;
        maybeInitialize(sqlSession);
        setTargetIdsForSource(sourceId, targetIds, sqlSession);
    }

    // TODO add support for the ListDiff type

    protected SQLSession getSQLSession() {
        if (!initialized) {
            try (SQLSession sqlSession = (SQLSession) getSourceDirectory().getSession()) {
                initialize(sqlSession);
                initialized = true;
            }
        }
        return (SQLSession) getSourceDirectory().getSession();
    }

    /**
     * Initialize if needed, using an existing session.
     *
     * @param sqlSession
     */
    protected void maybeInitialize(SQLSession sqlSession) {
        if (!initialized) {
            initialize(sqlSession);
            initialized = true;
        }
    }

    public Table getTable() {
        if (table == null) {
            boolean nativeCase = getSQLSourceDirectory().useNativeCase();
            table = SQLHelper.addTable(tableName, getDialect(), nativeCase);
            SQLHelper.addColumn(table, sourceColumn, ColumnType.STRING, nativeCase);
            SQLHelper.addColumn(table, targetColumn, ColumnType.STRING, nativeCase);
            // index added for Azure
            table.addIndex(null, IndexType.MAIN_NON_PRIMARY, sourceColumn);
        }
        return table;
    }

    private Dialect getDialect() {
        if (dialect == null) {
            dialect = getSQLSourceDirectory().getDialect();
        }
        return dialect;
    }

    public String getSourceColumn() {
        return sourceColumn;
    }

    public String getTargetColumn() {
        return targetColumn;
    }

    public String getTargetDirectoryName() {
        return targetDirectoryName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getDataFileName() {
        return dataFileName;
    }

}
