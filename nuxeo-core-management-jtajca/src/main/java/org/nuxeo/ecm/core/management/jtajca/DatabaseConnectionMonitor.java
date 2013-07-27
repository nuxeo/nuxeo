package org.nuxeo.ecm.core.management.jtajca;

import java.sql.Connection;
import java.sql.SQLException;
import javax.management.MXBean;

import org.apache.commons.pool.impl.GenericObjectPool;

@MXBean
public interface DatabaseConnectionMonitor extends Monitor {

    /**
     * Returns the default auto-commit property.
     *
     * @return true if default auto-commit is enabled
     */
    boolean getDefaultAutoCommit();

    /**
     * <p>Sets default auto-commit state of connections returned by this
     * datasource.</p>
     * <p>
     * Note: this method currently has no effect once the pool has been
     * initialized.  The pool is initialized the first time one of the
     * following methods is invoked: <code>getConnection, setLogwriter,
     * setLoginTimeout, getLoginTimeout, getLogWriter.</code></p>
     *
     * @param defaultAutoCommit default auto-commit value
     */
    void setDefaultAutoCommit(boolean defaultAutoCommit);

    /**
     * Returns the default readOnly property.
     *
     * @return true if connections are readOnly by default
     */
    boolean getDefaultReadOnly();

    /**
     * <p>Sets defaultReadonly property.</p>
     * <p>
     * Note: this method currently has no effect once the pool has been
     * initialized.  The pool is initialized the first time one of the
     * following methods is invoked: <code>getConnection, setLogwriter,
     * setLoginTimeout, getLoginTimeout, getLogWriter.</code></p>
     *
     * @param defaultReadOnly default read-only value
     */
    void setDefaultReadOnly(boolean defaultReadOnly);

    /**
     * Returns the default transaction isolation state of returned connections.
     *
     * @return the default value for transaction isolation state
     * @see Connection#getTransactionIsolation
     */
    int getDefaultTransactionIsolation();

    /**
     * <p>Sets the default transaction isolation state for returned
     * connections.</p>
     * <p>
     * Note: this method currently has no effect once the pool has been
     * initialized.  The pool is initialized the first time one of the
     * following methods is invoked: <code>getConnection, setLogwriter,
     * setLoginTimeout, getLoginTimeout, getLogWriter.</code></p>
     *
     * @param defaultTransactionIsolation the default transaction isolation
     * state
     * @see Connection#getTransactionIsolation
     */
    void setDefaultTransactionIsolation(
            int defaultTransactionIsolation);

    /**
     * Returns the default catalog.
     *
     * @return the default catalog
     */
    String getDefaultCatalog();

    /**
     * <p>Sets the default catalog.</p>
     * <p>
     * Note: this method currently has no effect once the pool has been
     * initialized.  The pool is initialized the first time one of the
     * following methods is invoked: <code>getConnection, setLogwriter,
     * setLoginTimeout, getLoginTimeout, getLogWriter.</code></p>
     *
     * @param defaultCatalog the default catalog
     */
    void setDefaultCatalog(String defaultCatalog);

    /**
     * Returns the jdbc driver class name.
     *
     * @return the jdbc driver class name
     */
    String getDriverClassName();

    /**
     * <p>Returns the maximum number of active connections that can be
     * allocated at the same time.
     * </p>
     * <p>A negative number means that there is no limit.</p>
     *
     * @return the maximum number of active connections
     */
    int getMaxActive();

    /**
     * Sets the maximum number of active connections that can be
     * allocated at the same time. Use a negative value for no limit.
     *
     * @param maxActive the new value for maxActive
     * @see #getMaxActive()
     */
    void setMaxActive(int maxActive);

    /**
     * <p>Returns the maximum number of connections that can remain idle in the
     * pool.
     * </p>
     * <p>A negative value indicates that there is no limit</p>
     *
     * @return the maximum number of idle connections
     */
    int getMaxIdle();

    /**
     * Sets the maximum number of connections that can remain idle in the
     * pool.
     *
     * @see #getMaxIdle()
     * @param maxIdle the new value for maxIdle
     */
    void setMaxIdle(int maxIdle);

    /**
     * Returns the minimum number of idle connections in the pool
     *
     * @return the minimum number of idle connections
     * @see GenericObjectPool#getMinIdle()
     */
    int getMinIdle();

    /**
     * Sets the minimum number of idle connections in the pool.
     *
     * @param minIdle the new value for minIdle
     * @see GenericObjectPool#setMinIdle(int)
     */
    void setMinIdle(int minIdle);

    /**
     * Returns the initial size of the connection pool.
     *
     * @return the number of connections created when the pool is initialized
     */
    int getInitialSize();

    /**
     * <p>Returns the maximum number of milliseconds that the pool will wait
     * for a connection to be returned before throwing an exception.
     * </p>
     * <p>A value less than or equal to zero means the pool is set to wait
     * indefinitely.</p>
     *
     * @return the maxWait property value
     */
    long getMaxWait();

    /**
     * <p>Sets the maxWait property.
     * </p>
     * <p>Use -1 to make the pool wait indefinitely.
     * </p>
     *
     * @param maxWait the new value for maxWait
     * @see #getMaxWait()
     */
    void setMaxWait(long maxWait);

    /**
     * Returns true if we are pooling statements.
     *
     * @return true if prepared and callable statements are pooled
     */
    boolean isPoolPreparedStatements();

    /**
     * <p>Sets whether to pool statements or not.</p>
     * <p>
     * Note: this method currently has no effect once the pool has been
     * initialized.  The pool is initialized the first time one of the
     * following methods is invoked: <code>getConnection, setLogwriter,
     * setLoginTimeout, getLoginTimeout, getLogWriter.</code></p>
     *
     * @param poolingStatements pooling on or off
     */
    void setPoolPreparedStatements(boolean poolingStatements);

    /**
     * Gets the value of the {@link #maxOpenPreparedStatements} property.
     *
     * @return the maximum number of open statements
     * @see #maxOpenPreparedStatements
     */
    int getMaxOpenPreparedStatements();

    /**
     * <p>Sets the value of the {@link #maxOpenPreparedStatements}
     * property.</p>
     * <p>
     * Note: this method currently has no effect once the pool has been
     * initialized.  The pool is initialized the first time one of the
     * following methods is invoked: <code>getConnection, setLogwriter,
     * setLoginTimeout, getLoginTimeout, getLogWriter.</code></p>
     *
     * @param maxOpenStatements the new maximum number of prepared statements
     * @see #maxOpenPreparedStatements
     */
    void setMaxOpenPreparedStatements(int maxOpenStatements);

    /**
     * Returns the {@link #testOnBorrow} property.
     *
     * @return true if objects are validated before being borrowed from the
     * pool
     *
     * @see #testOnBorrow
     */
    boolean getTestOnBorrow();

    /**
     * Sets the {@link #testOnBorrow} property. This property determines
     * whether or not the pool will validate objects before they are borrowed
     * from the pool. For a <code>true</code> value to have any effect, the
     * <code>validationQuery</code> property must be set to a non-null string.
     *
     * @param testOnBorrow new value for testOnBorrow property
     */
    void setTestOnBorrow(boolean testOnBorrow);

    /**
     * Returns the value of the {@link #testOnReturn} property.
     *
     * @return true if objects are validated before being returned to the
     * pool
     * @see #testOnReturn
     */
    boolean getTestOnReturn();

    /**
     * Sets the <code>testOnReturn</code> property. This property determines
     * whether or not the pool will validate objects before they are returned
     * to the pool. For a <code>true</code> value to have any effect, the
     * <code>validationQuery</code> property must be set to a non-null string.
     *
     * @param testOnReturn new value for testOnReturn property
     */
    void setTestOnReturn(boolean testOnReturn);

    /**
     * Returns the value of the {@link #timeBetweenEvictionRunsMillis}
     * property.
     *
     * @return the time (in miliseconds) between evictor runs
     * @see #timeBetweenEvictionRunsMillis
     */
    long getTimeBetweenEvictionRunsMillis();

    /**
     * Sets the {@link #timeBetweenEvictionRunsMillis} property.
     *
     * @param timeBetweenEvictionRunsMillis the new time between evictor runs
     * @see #timeBetweenEvictionRunsMillis
     */
    void setTimeBetweenEvictionRunsMillis(
            long timeBetweenEvictionRunsMillis);

    /**
     * Returns the value of the {@link #numTestsPerEvictionRun} property.
     *
     * @return the number of objects to examine during idle object evictor
     * runs
     * @see #numTestsPerEvictionRun
     */
    int getNumTestsPerEvictionRun();

    /**
     * Sets the value of the {@link #numTestsPerEvictionRun} property.
     *
     * @param numTestsPerEvictionRun the new {@link #numTestsPerEvictionRun}
     * value
     * @see #numTestsPerEvictionRun
     */
    void setNumTestsPerEvictionRun(int numTestsPerEvictionRun);

    /**
     * Returns the {@link #minEvictableIdleTimeMillis} property.
     *
     * @return the value of the {@link #minEvictableIdleTimeMillis} property
     * @see #minEvictableIdleTimeMillis
     */
    long getMinEvictableIdleTimeMillis();

    /**
     * Sets the {@link #minEvictableIdleTimeMillis} property.
     *
     * @param minEvictableIdleTimeMillis the minimum amount of time an object
     * may sit idle in the pool
     * @see #minEvictableIdleTimeMillis
     */
    void setMinEvictableIdleTimeMillis(
            long minEvictableIdleTimeMillis);

    /**
     * Returns the value of the {@link #testWhileIdle} property.
     *
     * @return true if objects examined by the idle object evictor are
     * validated
     * @see #testWhileIdle
     */
    boolean getTestWhileIdle();

    /**
     * Sets the <code>testWhileIdle</code> property. This property determines
     * whether or not the idle object evictor will validate connections.  For a
     * <code>true</code> value to have any effect, the
     * <code>validationQuery</code> property must be set to a non-null string.
     *
     * @param testWhileIdle new value for testWhileIdle property
     */
    void setTestWhileIdle(boolean testWhileIdle);

    /**
     * [Read Only] The current number of active connections that have been
     * allocated from this data source.
     *
     * @return the current number of active connections
     */
    int getNumActive();

    /**
     * [Read Only] The current number of idle connections that are waiting
     * to be allocated from this data source.
     *
     * @return the current number of idle connections
     */
    int getNumIdle();

    /**
     * Returns the JDBC connection {@link #url} property.
     *
     * @return the {@link #url} passed to the JDBC driver to establish
     * connections
     */
    String getUrl();

    /**
     * Returns the JDBC connection {@link #username} property.
     *
     * @return the {@link #username} passed to the JDBC driver to establish
     * connections
     */
    String getUsername();

    /**
     * Returns the validation query used to validate connections before
     * returning them.
     *
     * @return the SQL validation query
     * @see #validationQuery
     */
    String getValidationQuery();

    /**
     * <p>Sets the {@link #validationQuery}.</p>
     * <p>
     * Note: this method currently has no effect once the pool has been
     * initialized.  The pool is initialized the first time one of the
     * following methods is invoked: <code>getConnection, setLogwriter,
     * setLoginTimeout, getLoginTimeout, getLogWriter.</code></p>
     *
     * @param validationQuery the new value for the validation query
     */
    void setValidationQuery(String validationQuery);

    /**
     * Returns the validation query timeout.
     *
     * @return the timeout in seconds before connection validation queries fail.
     * @since 1.3
     */
    int getValidationQueryTimeout();

    /**
     * Sets the validation query timeout, the amount of time, in seconds, that
     * connection validation will wait for a response from the database when
     * executing a validation query.  Use a value less than or equal to 0 for
     * no timeout.
     * <p>
     * Note: this method currently has no effect once the pool has been
     * initialized.  The pool is initialized the first time one of the
     * following methods is invoked: <code>getConnection, setLogwriter,
     * setLoginTimeout, getLoginTimeout, getLogWriter.</code></p>
     *
     * @param timeout new validation query timeout value in seconds
     * @since 1.3
     */
    void setValidationQueryTimeout(int timeout);


    /**
     * Returns the value of the accessToUnderlyingConnectionAllowed property.
     *
     * @return true if access to the underlying connection is allowed, false
     * otherwise.
     */
    boolean isAccessToUnderlyingConnectionAllowed();

    /**
     * <p>Sets the value of the accessToUnderlyingConnectionAllowed property.
     * It controls if the PoolGuard allows access to the underlying connection.
     * (Default: false)</p>
     * <p>
     * Note: this method currently has no effect once the pool has been
     * initialized.  The pool is initialized the first time one of the
     * following methods is invoked: <code>getConnection, setLogwriter,
     * setLoginTimeout, getLoginTimeout, getLogWriter.</code></p>
     *
     * @param allow Access to the underlying connection is granted when true.
     */
    void setAccessToUnderlyingConnectionAllowed(boolean allow);

    /**
     * <strong>BasicDataSource does NOT support this method. </strong>
     *
     * <p>Returns the login timeout (in seconds) for connecting to the database.
     * </p>
     * <p>Calls {@link #createDataSource()}, so has the side effect
     * of initializing the connection pool.</p>
     *
     * @throws SQLException if a database access error occurs
     * @throws UnsupportedOperationException If the DataSource implementation
     *   does not support the login timeout feature.
     * @return login timeout in seconds
     */
    int getLoginTimeout() throws SQLException;

    /**
     * <strong>BasicDataSource does NOT support this method. </strong>
     *
     * <p>Set the login timeout (in seconds) for connecting to the
     * database.</p>
     * <p>
     * Calls {@link #createDataSource()}, so has the side effect
     * of initializing the connection pool.</p>
     *
     * @param loginTimeout The new login timeout, or zero for no timeout
     * @throws UnsupportedOperationException If the DataSource implementation
     *   does not support the login timeout feature.
     * @throws SQLException if a database access error occurs
     */
    void setLoginTimeout(int loginTimeout) throws SQLException;

    /**
     * Flag to remove abandoned connections if they exceed the
     * removeAbandonedTimout.
     *
     * Set to true or false, default false.
     * If set to true a connection is considered abandoned and eligible
     * for removal if it has been idle longer than the removeAbandonedTimeout.
     * Setting this to true can recover db connections from poorly written
     * applications which fail to close a connection.
     * <p>
     * Abandonded connections are identified and removed when
     * {@link #getConnection()} is invoked and the following conditions hold
     * <ul><li>{@link #getRemoveAbandoned()} = true </li>
     *     <li>{@link #getNumActive()} > {@link #getMaxActive()} - 3 </li>
     *     <li>{@link #getNumIdle()} < 2 </li></ul></p>
     */
    boolean getRemoveAbandoned();

    /**
     * @param removeAbandoned new removeAbandoned property value
     * @see #getRemoveAbandoned()
     */
    void setRemoveAbandoned(boolean removeAbandoned);

    /**
     * Timeout in seconds before an abandoned connection can be removed.
     *
     * Defaults to 300 seconds.
     * @return abandoned connection timeout
     */
    int getRemoveAbandonedTimeout();

    /**
     * @param removeAbandonedTimeout new removeAbandonedTimeout value
     */
    void setRemoveAbandonedTimeout(int removeAbandonedTimeout);

    /**
     * <p>Flag to log stack traces for application code which abandoned
     * a Statement or Connection.
     * </p>
     * <p>Defaults to false.
     * </p>
     * <p>Logging of abandoned Statements and Connections adds overhead
     * for every Connection open or new Statement because a stack
     * trace has to be generated. </p>
     */
    boolean getLogAbandoned();

    /**
     * @param logAbandoned new logAbandoned property value
     */
    void setLogAbandoned(boolean logAbandoned);

    /**
     * Add a custom connection property to the set that will be passed to our
     * JDBC driver. This <strong>MUST</strong> be called before the first
     * connection is retrieved (along with all the other configuration
     * property setters). Calls to this method after the connection pool
     * has been initialized have no effect.
     *
     * @param name Name of the custom connection property
     * @param value Value of the custom connection property
     */
    void addConnectionProperty(String name, String value);

    /**
     * Remove a custom connection property.
     *
     * @param name Name of the custom connection property to remove
     * @see #addConnectionProperty(String, String)
     */
    void removeConnectionProperty(String name);

    /**
     * Sets the connection properties passed to driver.connect(...).
     *
     * Format of the string must be [propertyName=property;]*
     *
     * NOTE - The "user" and "password" properties will be added
     * explicitly, so they do not need to be included here.
     *
     * @param connectionProperties the connection properties used to
     * create new connections
     */
    void setConnectionProperties(String connectionProperties);

    /**
     * If true, this data source is closed and no more connections can be retrieved from this datasource.
     * @return true, if the data source is closed; false otherwise
     */
    boolean isClosed();

}