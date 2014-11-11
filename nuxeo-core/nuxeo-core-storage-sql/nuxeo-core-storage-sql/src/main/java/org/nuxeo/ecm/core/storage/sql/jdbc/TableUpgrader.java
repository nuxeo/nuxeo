package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;

/**
 * Helper to provide sql migration's call while adding a column.
 *
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 * @since 5.4.1
 */
public class TableUpgrader {

    protected class TableUpgraderHolder {
        private String tableKey;
        private String columnName;
        private String sqlProcedure;
        private String testProp;

        public TableUpgraderHolder(String tableKey, String columnName,
                String sqlProcedure) {
            this.tableKey = tableKey;
            this.columnName = columnName;
            this.sqlProcedure = sqlProcedure;
        }

        public TableUpgraderHolder(String tableKey, String columnName,
                String sqlProcedure, String testProp) {
            this.tableKey = tableKey;
            this.columnName = columnName;
            this.sqlProcedure = sqlProcedure;
            this.testProp = testProp;
        }
    }

    protected List<TableUpgraderHolder> tableUpgraders = new ArrayList<TableUpgraderHolder>();

    private JDBCMapper mapper;

    private static final Log log = LogFactory.getLog(TableUpgrader.class);

    public TableUpgrader(JDBCMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Add a couple table/column with a sql procedure to executes when column is added
     *
     * @param tableKey table name
     * @param columnName desired added column
     * @param sqlProcedure sql procedure name
     */
    public void add(String tableKey, String columnName,
                String sqlProcedure) {
        tableUpgraders.add(new TableUpgraderHolder(tableKey, columnName, sqlProcedure));
    }

    /**
     * Add a couple table/column associated with a sql procedure to be executed
     * when the column is added and a a test flag to force his execution.
     *
     * @param tableKey table name
     * @param columnName desired added column
     * @param sqlProcedure sql procedure name
     * @param testProp test flag name
     */
    public void add(String tableKey, String columnName,
                String sqlProcedure, String testProp) {
        tableUpgraders.add(new TableUpgraderHolder(tableKey, columnName, sqlProcedure, testProp));
    }

    /**
     * Check if there is an added column that match with a upgrade process.
     * If one exists, it executes the associated sql in the category.
     * If not, nothing happend.
     *
     * @param tableKey table name
     * @param addedColumns list of added column
     * @throws SQLException Exception thrown by JDBC
     */
    public void upgrade(String tableKey, List<Column> addedColumns)
            throws SQLException {
        for (TableUpgraderHolder holder : tableUpgraders) {
            if (holder.tableKey.equals(tableKey)) {
                boolean upgradeVersions;
                if (addedColumns == null) {
                    // table created
                    upgradeVersions = mapper.testProps.containsKey(
                            holder.testProp);
                } else {
                    // columns added
                    upgradeVersions = false;
                    for (Column col : addedColumns) {
                        if (col.getKey().equals(holder.columnName)) {
                            upgradeVersions = true;
                            break;
                        }
                    }
                }
                if (upgradeVersions) {
                    log.info("Upgrading table " + tableKey);
                    mapper.sqlInfo.executeSQLStatements(holder.sqlProcedure,
                            mapper);
                }
            }
        }
    }
}
