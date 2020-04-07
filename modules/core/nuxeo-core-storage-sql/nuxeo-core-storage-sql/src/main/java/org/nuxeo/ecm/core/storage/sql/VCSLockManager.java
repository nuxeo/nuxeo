/*
 * (C) Copyright 2006-2020 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.core.storage.sql.Model.LOCK_CREATED_KEY;
import static org.nuxeo.ecm.core.storage.sql.Model.LOCK_OWNER_KEY;
import static org.nuxeo.ecm.core.storage.sql.Model.LOCK_TABLE_NAME;
import static org.nuxeo.ecm.core.storage.sql.Model.MAIN_KEY;

import java.io.Serializable;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.LockException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.model.LockManager;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo.SQLInfoSelect;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.runtime.datasource.ConnectionHelper;

/**
 * Manager of locks stored in the repository SQL database.
 */
public class VCSLockManager implements LockManager {

    private static final Logger log = LogManager.getLogger(VCSLockManager.class);

    public static final int LOCK_RETRIES = 10;

    public static final long LOCK_SLEEP_DELAY = 1; // 1 ms

    public static final long LOCK_SLEEP_INCREMENT = 50; // add 50 ms each time

    protected final String dataSourceName;

    protected final Model model;

    protected final SQLInfo sqlInfo;

    /**
     * Creates a lock manager for the given repository.
     * <p>
     * {@link #closeLockManager()} must be called when done with the lock manager.
     *
     * @since 9.3
     */
    public VCSLockManager(RepositoryImpl repository) {
        dataSourceName = "repository_" + repository.getName();
        model = repository.getModel();
        sqlInfo = repository.getSQLInfo();
    }

    protected Connection getConnection() throws SQLException {
        // open connection in noSharing mode
        return ConnectionHelper.getConnection(dataSourceName, true);
    }

    protected Serializable idFromString(String id) {
        return model.idFromString(id);
    }

    @Override
    public Lock getLock(final String id) {
        return readLock(idFromString(id));
    }

    @Override
    public Lock setLock(String id, Lock lock) {
        // We don't call addSuppressed() on an existing exception
        // because constructing it beforehand when it most likely
        // won't be needed is expensive.
        List<Throwable> suppressed = new ArrayList<>(0);
        long sleepDelay = LOCK_SLEEP_DELAY;
        for (int i = 0; i < LOCK_RETRIES; i++) {
            if (i > 0) {
                log.debug("Retrying lock on {}: try {}", id, i + 1);
            }
            try {
                return writeLock(idFromString(id), lock);
            } catch (NuxeoException e) {
                suppressed.add(e);
                if (shouldRetry(e)) {
                    // cluster: two simultaneous inserts
                    // retry
                    try {
                        Thread.sleep(sleepDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    }
                    sleepDelay += LOCK_SLEEP_INCREMENT;
                    continue;
                }
                // not something to retry
                NuxeoException exception = new NuxeoException(e);
                for (Throwable t : suppressed) {
                    exception.addSuppressed(t);
                }
                throw exception;
            }
        }
        LockException exception = new LockException(
                "Failed to lock " + id + ", too much concurrency (tried " + LOCK_RETRIES + " times)");
        for (Throwable t : suppressed) {
            exception.addSuppressed(t);
        }
        throw exception;
    }

    protected void checkConcurrentUpdate(Throwable e) {
        if (sqlInfo.dialect.isConcurrentUpdateException(e)) {
            log.debug(e, e);
            // don't keep the original message, as it may reveal database-level info
            throw new ConcurrentUpdateException("Concurrent update", e);
        }
    }

    /**
     * Does the exception mean that we should retry the transaction?
     */
    protected boolean shouldRetry(Exception e) {
        if (e instanceof ConcurrentUpdateException) {
            return true;
        }
        Throwable t = e.getCause();
        if (t instanceof BatchUpdateException && t.getCause() != null) {
            t = t.getCause();
        }
        return t instanceof SQLException && shouldRetry((SQLException) t);
    }

    protected boolean shouldRetry(SQLException e) {
        String sqlState = e.getSQLState();
        if ("23000".equals(sqlState)) {
            // MySQL: Duplicate entry ... for key ...
            // Oracle: unique constraint ... violated
            // SQL Server: Violation of PRIMARY KEY constraint
            return true;
        }
        if ("23001".equals(sqlState)) {
            // H2: Unique index or primary key violation
            return true;
        }
        if ("23505".equals(sqlState)) {
            // PostgreSQL: duplicate key value violates unique constraint
            return true;
        }
        if ("S0003".equals(sqlState) || "S0005".equals(sqlState)) {
            // SQL Server: Snapshot isolation transaction aborted due to update
            // conflict
            return true;
        }
        return false;
    }

    @Override
    public Lock removeLock(String id, String owner) {
        return deleteLock(idFromString(id), owner);
    }

    /*
     * ----- JDBC -----
     */

    protected Lock readLock(Serializable id) {
        try (Connection connection = getConnection()) {
            return readLock0(connection, id);
        } catch (SQLException e) {
            checkConcurrentUpdate(e);
            throw new NuxeoException(e);
        }
    }

    protected Lock readLock0(Connection connection, Serializable id) throws SQLException {
        SQLInfoSelect select = sqlInfo.selectFragmentById.get(LOCK_TABLE_NAME);
        try (PreparedStatement ps = connection.prepareStatement(select.sql)) {
            for (Column column : select.whereColumns) {
                String key = column.getKey();
                if (MAIN_KEY.equals(key)) {
                    column.setToPreparedStatement(ps, 1, id);
                } else {
                    throw new NuxeoException(key);
                }
            }
            log.trace("SQL: {} id={}", select.sql, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    log.trace("SQL:   -> null");
                    return null;
                }
                String owner = null;
                Calendar created = null;
                int i = 1;
                for (Column column : select.whatColumns) {
                    String key = column.getKey();
                    Serializable value = column.getFromResultSet(rs, i++);
                    if (LOCK_OWNER_KEY.equals(key)) {
                        owner = (String) value;
                    } else if (LOCK_CREATED_KEY.equals(key)) {
                        created = (Calendar) value;
                    } else {
                        throw new NuxeoException(key);
                    }
                }
                log.trace("SQL:   -> {}", owner);
                return new Lock(owner, created);
            }
        }
    }

    protected Lock writeLock(Serializable id, Lock lock) {
        try (Connection connection = getConnection()) {
            return writeLock(connection, id, lock);
        } catch (SQLException e) {
            checkConcurrentUpdate(e);
            throw new NuxeoException(e);
        }
    }

    protected Lock writeLock(Connection connection, Serializable id, Lock lock) throws SQLException {
        try {
            writeLock0(connection, id, lock);
            return null;
        } catch (SQLException e) {
            if (!sqlInfo.dialect.isConcurrentUpdateException(e)) {
                throw e;
            }
            log.trace("SQL:   -> duplicate");
        }
        // lock already exists, try to read it
        Lock oldLock = readLock0(connection, id);
        if (oldLock != null) {
            // there indeed was another lock, return it
            return oldLock;
        }
        // we attempted a write that failed, but when reading there's nothing
        // because a concurrent transaction already removed it
        // however we have to return an old lock per our contract
        // retry at a higher level
        throw new ConcurrentUpdateException("Concurrent update");
    }

    protected void writeLock0(Connection connection, Serializable id, Lock lock) throws SQLException {
        String sql = sqlInfo.getInsertSql(LOCK_TABLE_NAME);
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            for (Column column : sqlInfo.getInsertColumns(LOCK_TABLE_NAME)) {
                String key = column.getKey();
                Serializable value;
                if (MAIN_KEY.equals(key)) {
                    value = id;
                } else if (LOCK_OWNER_KEY.equals(key)) {
                    value = lock.getOwner();
                } else if (LOCK_CREATED_KEY.equals(key)) {
                    value = lock.getCreated();
                } else {
                    throw new NuxeoException(key);
                }
                column.setToPreparedStatement(ps, i++, value);
            }
            log.trace("SQL: {} id={} owner={}", () -> sql, () -> id, lock::getOwner);
            ps.execute();
        }
    }

    protected Lock deleteLock(Serializable id, String owner) {
        try (Connection connection = getConnection()) {
            return deleteLock(connection, id, owner);
        } catch (SQLException e) {
            checkConcurrentUpdate(e);
            throw new NuxeoException(e);
        }
    }

    protected Lock deleteLock(Connection connection, Serializable id, String owner) throws SQLException {
        log.trace("SQL: tx begin");
        connection.setAutoCommit(false);
        try {
            Lock oldLock = readLock0(connection, id);
            if (owner != null) {
                if (oldLock == null) {
                    // not locked, nothing to do
                    return null;
                }
                if (!LockManager.canLockBeRemoved(oldLock.getOwner(), owner)) {
                    // existing mismatched lock, flag failure
                    return new Lock(oldLock, true);
                }
            }
            if (oldLock != null) {
                deleteLock0(connection, id);
            }
            return oldLock;
        } finally {
            try {
                log.trace("SQL: tx commit");
                connection.commit();
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    protected void deleteLock0(Connection connection, Serializable id) throws SQLException {
        String sql = sqlInfo.getDeleteSql(LOCK_TABLE_NAME, 1);
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            log.trace("SQL: {} id={}", sql, id);
            sqlInfo.dialect.setId(ps, 1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + dataSourceName + ')';
    }

}
