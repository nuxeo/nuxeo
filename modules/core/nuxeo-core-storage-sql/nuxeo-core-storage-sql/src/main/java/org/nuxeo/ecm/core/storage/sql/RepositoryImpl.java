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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.lock.LockManager;
import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;
import org.nuxeo.ecm.core.storage.FulltextDescriptor;
import org.nuxeo.ecm.core.storage.lock.LockManagerService;
import org.nuxeo.ecm.core.storage.sql.Model.IdType;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLSession;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCConnection;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCLogger;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCMapper;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo;
import org.nuxeo.ecm.core.storage.sql.jdbc.TableUpgrader;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Database;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.DialectOracle;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.SQLStatement.ListCollector;
import org.nuxeo.runtime.RuntimeMessage;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.RuntimeMessage.Source;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.cluster.ClusterService;
import org.nuxeo.runtime.datasource.ConnectionHelper;
import org.nuxeo.runtime.datasource.DataSourceHelper;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.transaction.TransactionHelper;

import io.dropwizard.metrics5.Counter;
import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;

/**
 * {@link Repository} implementation, to be extended by backend-specific initialization code.
 */
public class RepositoryImpl implements Repository, org.nuxeo.ecm.core.model.Repository {

    private static final Log log = LogFactory.getLog(RepositoryImpl.class);

    public static final String TEST_UPGRADE = "testUpgrade";

    // property in sql.txt file
    public static final String TEST_UPGRADE_VERSIONS = "testUpgradeVersions";

    public static final String TEST_UPGRADE_LAST_CONTRIBUTOR = "testUpgradeLastContributor";

    public static final String TEST_UPGRADE_LOCKS = "testUpgradeLocks";

    public static final String TEST_UPGRADE_SYS_CHANGE_TOKEN = "testUpgradeSysChangeToken";

    public static Map<String, Serializable> testProps = new HashMap<>();

    protected final RepositoryDescriptor repositoryDescriptor;

    private final Collection<SessionImpl> sessions;

    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected final Counter sessionCount;

    private LockManager lockManager;

    /**
     * @since 7.4 : used to know if the LockManager was provided by this repository or externally
     */
    protected boolean selfRegisteredLockManager = false;

    /** Propagator of invalidations to all mappers' caches. */
    // public for tests
    public final VCSInvalidationsPropagator invalidationsPropagator;

    protected VCSClusterInvalidator clusterInvalidator;

    public boolean requiresClusterSQL;

    private Model model;

    protected SQLInfo sqlInfo;

    public RepositoryImpl(RepositoryDescriptor repositoryDescriptor) {
        this.repositoryDescriptor = repositoryDescriptor;
        sessions = new CopyOnWriteArrayList<>();
        invalidationsPropagator = new VCSInvalidationsPropagator();

        sessionCount = registry.counter(MetricName.build("nuxeo", "repositories", "repository", "sessions")
                                                  .tagged("repository", repositoryDescriptor.name));
        createMetricsGauges();

        initRepository();
    }

    protected void createMetricsGauges() {
        MetricName gaugeName = MetricName.build("nuxeo", "repositories", "repository", "cache", "size")
                                         .tagged("repository", repositoryDescriptor.name);
        registry.remove(gaugeName);
        registry.register(gaugeName, new Gauge<Long>() {
            @Override
            public Long getValue() {
                return getCacheSize();
            }
        });
        gaugeName = MetricName.build("nuxeo", "repositories", "repository", "cache", "pristine")
                              .tagged("repository", repositoryDescriptor.name);
        registry.remove(gaugeName);
        registry.register(gaugeName, new Gauge<Long>() {
            @Override
            public Long getValue() {
                return getCachePristineSize();
            }
        });
        gaugeName = MetricName.build("nuxeo", "repositories", "repository", "cache", "selection")
                              .tagged("repository", repositoryDescriptor.name);
        registry.remove(gaugeName);
        registry.register(gaugeName, new Gauge<Long>() {
            @Override
            public Long getValue() {
                return getCacheSelectionSize();
            }
        });
        gaugeName = MetricName.build("nuxeo", "repositories", "repository", "cache", "mapper")
                .tagged("repository", repositoryDescriptor.name);
        registry.remove(gaugeName);
        registry.register(gaugeName, new Gauge<Long>() {
            @Override
            public Long getValue() {
                return getCacheMapperSize();
            }
        });
    }

    protected Mapper createCachingMapper(Model model, Mapper mapper) {
        try {
            Class<? extends CachingMapper> cachingMapperClass = getCachingMapperClass();
            if (cachingMapperClass == null) {
                return mapper;
            }
            CachingMapper cachingMapper = cachingMapperClass.getDeclaredConstructor().newInstance();
            cachingMapper.initialize(getName(), model, mapper, invalidationsPropagator,
                    repositoryDescriptor.cachingMapperProperties);
            return cachingMapper;
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
    }

    protected Class<? extends CachingMapper> getCachingMapperClass() {
        if (!repositoryDescriptor.getCachingMapperEnabled()) {
            return null;
        }
        Class<? extends CachingMapper> cachingMapperClass = repositoryDescriptor.cachingMapperClass;
        if (cachingMapperClass == null) {
            // default cache
            cachingMapperClass = SoftRefCachingMapper.class;
        }
        return cachingMapperClass;
    }

    public RepositoryDescriptor getRepositoryDescriptor() {
        return repositoryDescriptor;
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    public Model getModel() {
        return model;
    }

    /** @since 11.1 */
    public SQLInfo getSQLInfo() {
        return sqlInfo;
    }

    public VCSInvalidationsPropagator getInvalidationsPropagator() {
        return invalidationsPropagator;
    }

    public boolean isChangeTokenEnabled() {
        return repositoryDescriptor.isChangeTokenEnabled();
    }

    @Override
    public SQLSession getSession() {
        return new SQLSession(getConnection(), this); // NOSONAR
    }

    /**
     * Gets a new connection.
     *
     * @return the session
     */
    @Override
    public synchronized SessionImpl getConnection() {
        if (Framework.getRuntime().isShuttingDown()) {
            throw new IllegalStateException("Cannot open connection, runtime is shutting down");
        }
        SessionPathResolver pathResolver = new SessionPathResolver();
        Mapper mapper = new JDBCMapper(model, pathResolver, sqlInfo, clusterInvalidator, this);
        mapper = createCachingMapper(model, mapper);
        SessionImpl session = new SessionImpl(this, model, mapper);
        pathResolver.setSession(session);

        sessions.add(session);
        sessionCount.inc();
        return session;
    }

    // callback by session at close time
    protected void closeSession(SessionImpl session) {
        sessions.remove(session);
        sessionCount.dec();
    }

    protected void initRepository() {
        log.debug("Initializing");
        prepareClusterInvalidator(); // sets requiresClusterSQL used by backend init

        // check datasource
        String dataSourceName = JDBCConnection.getDataSourceName(repositoryDescriptor.name);
        try {
            DataSourceHelper.getDataSource(dataSourceName);
        } catch (NamingException cause) {
            throw new NuxeoException("Cannot acquire datasource: " + dataSourceName, cause);
        }

        // check connection and get dialect
        Dialect dialect;
        try (Connection connection = ConnectionHelper.getConnection(dataSourceName)) {
            dialect = Dialect.createDialect(connection, repositoryDescriptor);
        } catch (SQLException cause) {
            throw new NuxeoException("Cannot get connection from datasource: " + dataSourceName, cause);
        }

        // model setup
        ModelSetup modelSetup = new ModelSetup();
        modelSetup.materializeFulltextSyntheticColumn = dialect.getMaterializeFulltextSyntheticColumn();
        modelSetup.supportsArrayColumns = dialect.supportsArrayColumns();
        switch (dialect.getIdType()) {
        case VARCHAR:
        case UUID:
            modelSetup.idType = IdType.STRING;
            break;
        case SEQUENCE:
            modelSetup.idType = IdType.LONG;
            break;
        default:
            throw new AssertionError(dialect.getIdType().toString());
        }
        modelSetup.repositoryDescriptor = repositoryDescriptor;

        // Model and SQLInfo
        model = new Model(modelSetup);
        sqlInfo = new SQLInfo(model, dialect, requiresClusterSQL);

        // DDL mode
        String ddlMode = repositoryDescriptor.getDDLMode();
        if (ddlMode == null) {
            // compat
            ddlMode = repositoryDescriptor.getNoDDL() ? RepositoryDescriptor.DDL_MODE_IGNORE
                    : RepositoryDescriptor.DDL_MODE_EXECUTE;
        }

        // create database
        if (ddlMode.equals(RepositoryDescriptor.DDL_MODE_IGNORE)) {
            log.info("Skipping database creation");
        } else {
            createDatabase(ddlMode);
        }
        if (log.isDebugEnabled()) {
            FulltextDescriptor fulltextDescriptor = repositoryDescriptor.getFulltextDescriptor();
            log.debug(String.format("Database ready, fulltext: disabled=%b storedInBlob=%b searchDisabled=%b.",
                    fulltextDescriptor.getFulltextDisabled(), fulltextDescriptor.getFulltextStoredInBlob(),
                    fulltextDescriptor.getFulltextSearchDisabled()));
        }

        initLockManager();
        initClusterInvalidator();

        // log once which mapper cache is being used
        Class<? extends CachingMapper> cachingMapperClass = getCachingMapperClass();
        if (cachingMapperClass == null) {
            log.warn("VCS Mapper cache is disabled.");
        } else {
            log.info("VCS Mapper cache using: " + cachingMapperClass.getName());
        }

        initRootNode();
    }

    protected void initRootNode() {
        // access a session once so that SessionImpl.computeRootNode can create the root node
        try (SessionImpl session = getConnection()) {
            // nothing
        }
    }

    protected String getLockManagerName() {
        // TODO configure in repo descriptor
        return getName();
    }

    protected void initLockManager() {
        String lockManagerName = getLockManagerName();
        LockManagerService lockManagerService = Framework.getService(LockManagerService.class);
        lockManager = lockManagerService.getLockManager(lockManagerName);
        if (lockManager == null) {
            // no descriptor
            // default to a VCSLockManager
            lockManager = new VCSLockManager(this);
            lockManagerService.registerLockManager(lockManagerName, lockManager);
            selfRegisteredLockManager = true;
        } else {
            selfRegisteredLockManager = false;
        }
        log.info("Repository " + getName() + " using lock manager " + lockManager);
    }

    protected void prepareClusterInvalidator() {
        if (Framework.getService(ClusterService.class).isEnabled()) {
            clusterInvalidator = createClusterInvalidator();
            requiresClusterSQL = clusterInvalidator.requiresClusterSQL();
        }
    }

    protected VCSClusterInvalidator createClusterInvalidator() {
        Class<? extends VCSClusterInvalidator> klass = repositoryDescriptor.clusterInvalidatorClass;
        if (klass == null) {
            klass = VCSPubSubInvalidator.class;
        }
        try {
            return klass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
    }

    protected void initClusterInvalidator() {
        if (clusterInvalidator != null) {
            String nodeId = Framework.getService(ClusterService.class).getNodeId();
            clusterInvalidator.initialize(nodeId, this);
        }
    }

    public static class SessionPathResolver implements PathResolver {

        private Session session;

        protected void setSession(Session session) {
            this.session = session;
        }

        @Override
        public Serializable getIdForPath(String path) {
            Node node = session.getNodeByPath(path, null);
            return node == null ? null : node.getId();
        }
    }

    /*
     * ----- Repository -----
     */

    @Override
    public void shutdown() {
        close();
    }

    @Override
    public synchronized void close() {
        closeAllSessions();
        model = null;
        if (clusterInvalidator != null) {
            clusterInvalidator.close();
        }

        if (selfRegisteredLockManager) {
            LockManagerService lms = Framework.getService(LockManagerService.class);
            if (lms != null) {
                lms.unregisterLockManager(getLockManagerName());
            }
        }
    }

    protected synchronized void closeAllSessions() {
        for (SessionImpl session : sessions) {
            session.closeSession();
        }
        sessions.clear();
        sessionCount.dec(sessionCount.getCount());
        if (lockManager != null) {
            lockManager.closeLockManager();
        }
    }

    /*
     * ----- RepositoryManagement -----
     */

    @Override
    public String getName() {
        return repositoryDescriptor.name;
    }

    @Override
    public int clearCaches() {
        int n = 0;
        for (SessionImpl session : sessions) {
            n += session.clearCaches();
        }
        if (lockManager != null) {
            lockManager.clearLockManagerCaches();
        }
        return n;
    }

    @Override
    public long getCacheSize() {
        long size = 0;
        for (SessionImpl session : sessions) {
            size += session.getCacheSize();
        }
        return size;
    }

    public long getCacheMapperSize() {
        long size = 0;
        for (SessionImpl session : sessions) {
            size += session.getCacheMapperSize();
        }
        return size;
    }

    @Override
    public long getCachePristineSize() {
        long size = 0;
        for (SessionImpl session : sessions) {
            size += session.getCachePristineSize();
        }
        return size;
    }

    @Override
    public long getCacheSelectionSize() {
        long size = 0;
        for (SessionImpl session : sessions) {
            size += session.getCacheSelectionSize();
        }
        return size;
    }

    @Override
    public void processClusterInvalidationsNext() {
        // TODO pass through or something
    }

    @Override
    public void markReferencedBinaries() {
        try (SessionImpl session = getConnection()) {
            session.markReferencedBinaries();
        }
    }

    @Override
    public int cleanupDeletedDocuments(int max, Calendar beforeTime) {
        if (!repositoryDescriptor.getSoftDeleteEnabled()) {
            return 0;
        }
        try (SessionImpl session = getConnection()) {
            return session.cleanupDeletedDocuments(max, beforeTime);
        }
    }

    @Override
    public FulltextConfiguration getFulltextConfiguration() {
        return model.getFulltextConfiguration();
    }

    /**
     * Creates the necessary structures in the database.
     *
     * @param ddlMode the DDL execution mode
     */
    protected void createDatabase(String ddlMode) {
        // some databases (SQL Server) can't create tables/indexes/etc in a transaction, so suspend it
        runWithoutTransaction(() -> createDatabaseNoTx(ddlMode));
    }

    protected void createDatabaseNoTx(String ddlMode) {
        String dataSourceName = "repository_" + getName();
        try {
            // open connection in noSharing mode
            try (Connection connection = ConnectionHelper.getConnection(dataSourceName, true)) {
                sqlInfo.dialect.performPostOpenStatements(connection);
                if (!connection.getAutoCommit()) {
                    throw new NuxeoException("connection should not run in transactional mode for DDL operations");
                }
                createTables(connection, ddlMode);
            }
        } catch (SQLException e) {
            throw new NuxeoException(e);
        }
    }

    protected String getTableName(String origName) {
        if (sqlInfo.dialect instanceof DialectOracle) {
            if (origName.length() > 30) {
                StringBuilder sb = new StringBuilder(origName.length());
                try {
                    MessageDigest digest = MessageDigest.getInstance("MD5");
                    sb.append(origName.substring(0, 15));
                    sb.append('_');
                    digest.update(origName.getBytes());
                    sb.append(Dialect.toHexString(digest.digest()).substring(0, 12));
                    return sb.toString();
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException("Error", e);
                }
            }
        }
        return origName;
    }

    protected void createTables(Connection connection, String ddlMode) throws SQLException {
        JDBCLogger logger = new JDBCLogger(getName());
        Dialect dialect = sqlInfo.dialect;
        ListCollector ddlCollector = new ListCollector();

        sqlInfo.executeSQLStatements(null, ddlMode, connection, logger, ddlCollector); // for missing category
        sqlInfo.executeSQLStatements("first", ddlMode, connection, logger, ddlCollector);
        sqlInfo.executeSQLStatements("beforeTableCreation", ddlMode, connection, logger, ddlCollector);
        if (testProps.containsKey(TEST_UPGRADE)) {
            // create "old" tables
            sqlInfo.executeSQLStatements("testUpgrade", ddlMode, connection, logger, null); // do not collect
        }

        String schemaName = dialect.getConnectionSchema(connection);
        DatabaseMetaData metadata = connection.getMetaData();
        Set<String> tableNames = findTableNames(metadata, schemaName);
        Database database = sqlInfo.getDatabase();
        Map<String, List<Column>> added = new HashMap<>();

        for (Table table : database.getTables()) {
            String tableName = getTableName(table.getPhysicalName());
            if (!tableNames.contains(tableName.toUpperCase())) {
                /*
                 * Create missing table.
                 */
                ddlCollector.add(table.getCreateSql());
                ddlCollector.addAll(table.getPostCreateSqls(model));
                added.put(table.getKey(), null); // null = table created
                sqlInfo.sqlStatementsProperties.put("create_table_" + tableName.toLowerCase(), Boolean.TRUE);
            } else {
                /*
                 * Get existing columns.
                 */
                Map<String, Integer> columnTypes = new HashMap<>();
                Map<String, String> columnTypeNames = new HashMap<>();
                Map<String, Integer> columnTypeSizes = new HashMap<>();
                try (ResultSet rs = metadata.getColumns(null, schemaName, tableName, "%")) {
                    while (rs.next()) {
                        String schema = rs.getString("TABLE_SCHEM");
                        if (schema != null) { // null for MySQL, doh!
                            if ("INFORMATION_SCHEMA".equals(schema.toUpperCase())) {
                                // H2 returns some system tables (locks)
                                continue;
                            }
                        }
                        String columnName = rs.getString("COLUMN_NAME").toUpperCase();
                        columnTypes.put(columnName, Integer.valueOf(rs.getInt("DATA_TYPE")));
                        columnTypeNames.put(columnName, rs.getString("TYPE_NAME"));
                        columnTypeSizes.put(columnName, Integer.valueOf(rs.getInt("COLUMN_SIZE")));
                    }
                }
                /*
                 * Update types and create missing columns.
                 */
                List<Column> addedColumns = new LinkedList<>();
                for (Column column : table.getColumns()) {
                    String upperName = column.getPhysicalName().toUpperCase();
                    Integer type = columnTypes.remove(upperName);
                    if (type == null) {
                        log.warn("Adding missing column in database: " + column.getFullQuotedName());
                        ddlCollector.add(table.getAddColumnSql(column));
                        ddlCollector.addAll(table.getPostAddSqls(column, model));
                        addedColumns.add(column);
                    } else {
                        String actualName = columnTypeNames.get(upperName);
                        Integer actualSize = columnTypeSizes.get(upperName);
                        String message = column.checkJdbcType(type, actualName, actualSize);
                        if (message != null) {
                            log.error(message);
                            Framework.getRuntime()
                                     .getMessageHandler()
                                     .addMessage(new RuntimeMessage(Level.ERROR, message, Source.CODE,
                                             this.getClass().getName()));
                        }
                    }
                }
                for (String col : dialect.getIgnoredColumns(table)) {
                    columnTypes.remove(col.toUpperCase());
                }
                if (!columnTypes.isEmpty()) {
                    log.warn("Database contains additional unused columns for table " + table.getQuotedName() + ": "
                            + String.join(", ", columnTypes.keySet()));
                }
                if (!addedColumns.isEmpty()) {
                    if (added.containsKey(table.getKey())) {
                        throw new AssertionError();
                    }
                    added.put(table.getKey(), addedColumns);
                }
            }
        }

        if (testProps.containsKey(TEST_UPGRADE)) {
            // create "old" content in tables
            sqlInfo.executeSQLStatements("testUpgradeOldTables", ddlMode, connection, logger, ddlCollector);
        }

        // run upgrade for each table if added columns or test
        if (!added.isEmpty()) {
            TableUpgrader tableUpgrader = createTableUpgrader(connection, logger);
            for (Entry<String, List<Column>> en : added.entrySet()) {
                List<Column> addedColumns = en.getValue();
                String tableKey = en.getKey();
                tableUpgrader.upgrade(tableKey, addedColumns, ddlMode, ddlCollector);
            }
        }

        sqlInfo.executeSQLStatements("afterTableCreation", ddlMode, connection, logger, ddlCollector);
        sqlInfo.executeSQLStatements("last", ddlMode, connection, logger, ddlCollector);

        // aclr_permission check for PostgreSQL
        dialect.performAdditionalStatements(connection);

        /*
         * Execute all the collected DDL, or dump it if requested, depending on ddlMode
         */

        // ddlMode may be:
        // ignore (not treated here, nothing done)
        // dump (implies execute)
        // dump,execute
        // dump,ignore (no execute)
        // execute
        // abort (implies dump)
        // compat can be used instead of execute to always recreate stored procedures

        List<String> ddl = ddlCollector.getStrings();
        boolean ignore = ddlMode.contains(RepositoryDescriptor.DDL_MODE_IGNORE);
        boolean dump = ddlMode.contains(RepositoryDescriptor.DDL_MODE_DUMP);
        boolean abort = ddlMode.contains(RepositoryDescriptor.DDL_MODE_ABORT);
        if (dump || abort) {
            /*
             * Dump DDL if not empty.
             */
            if (!ddl.isEmpty()) {
                File dumpFile = new File(Environment.getDefault().getLog(), "ddl-vcs-" + getName() + ".sql");
                try (OutputStream out = new FileOutputStream(dumpFile); PrintStream ps = new PrintStream(out)) {
                    for (String sql : dialect.getDumpStart()) {
                        ps.println(sql);
                    }
                    for (String sql : ddl) {
                        sql = sql.trim();
                        if (sql.endsWith(";")) {
                            sql = sql.substring(0, sql.length() - 1);
                        }
                        ps.println(dialect.getSQLForDump(sql));
                    }
                    for (String sql : dialect.getDumpStop()) {
                        ps.println(sql);
                    }
                } catch (IOException e) {
                    throw new NuxeoException(e);
                }
                /*
                 * Abort if requested.
                 */
                if (abort) {
                    log.error("Dumped DDL to: " + dumpFile);
                    throw new NuxeoException(
                            "Database initialization failed for: " + getName() + ", DDL must be executed: " + dumpFile);
                }
            }
        }
        if (!ignore) {
            /*
             * Execute DDL.
             */
            try (Statement st = connection.createStatement()) {
                for (String sql : ddl) {
                    logger.log(sql.replace("\n", "\n    ")); // indented
                    try {
                        st.execute(sql);
                    } catch (SQLException e) {
                        throw new SQLException("Error executing: " + sql + " : " + e.getMessage(), e);
                    }
                }
            }
            /*
             * Execute post-DDL stuff.
             */
            try (Statement st = connection.createStatement()) {
                for (String sql : dialect.getStartupSqls(model, sqlInfo.database)) {
                    logger.log(sql.replace("\n", "\n    ")); // indented
                    try {
                        st.execute(sql);
                    } catch (SQLException e) {
                        throw new SQLException("Error executing: " + sql + " : " + e.getMessage(), e);
                    }
                }
            }
        }
    }

    protected TableUpgrader createTableUpgrader(Connection connection, JDBCLogger logger) {
        TableUpgrader tableUpgrader = new TableUpgrader(sqlInfo, connection, logger);
        tableUpgrader.add(Model.VERSION_TABLE_NAME, Model.VERSION_IS_LATEST_KEY, "upgradeVersions",
                TEST_UPGRADE_VERSIONS);
        tableUpgrader.add("dublincore", "lastContributor", "upgradeLastContributor", TEST_UPGRADE_LAST_CONTRIBUTOR);
        tableUpgrader.add(Model.LOCK_TABLE_NAME, Model.LOCK_OWNER_KEY, "upgradeLocks", TEST_UPGRADE_LOCKS);
        tableUpgrader.add(Model.HIER_TABLE_NAME, Model.MAIN_SYS_CHANGE_TOKEN_KEY, "upgradeSysChangeToken",
                TEST_UPGRADE_SYS_CHANGE_TOKEN);
        return tableUpgrader;
    }

    /** Finds uppercase table names. */
    protected static Set<String> findTableNames(DatabaseMetaData metadata, String schemaName) throws SQLException {
        Set<String> tableNames = new HashSet<>();
        ResultSet rs = metadata.getTables(null, schemaName, "%", new String[] { "TABLE" });
        while (rs.next()) {
            String tableName = rs.getString("TABLE_NAME");
            tableNames.add(tableName.toUpperCase());
        }
        rs.close();
        return tableNames;
    }

    // completely stops the current transaction while running something
    protected static void runWithoutTransaction(Runnable runnable) {
        boolean rollback = TransactionHelper.isTransactionMarkedRollback();
        boolean hasTransaction = TransactionHelper.isTransactionActiveOrMarkedRollback();
        if (hasTransaction) {
            TransactionHelper.commitOrRollbackTransaction();
        }
        boolean completedAbruptly = true;
        try {
            runnable.run();
            completedAbruptly = false;
        } finally {
            if (hasTransaction) {
                try {
                    TransactionHelper.startTransaction();
                } finally {
                    if (completedAbruptly || rollback) {
                        TransactionHelper.setTransactionRollbackOnly();
                    }
                }
            }
        }
    }

}
