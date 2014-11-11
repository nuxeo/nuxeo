package org.nuxeo.ecm.core.management.jtajca.internal;

import java.io.PrintWriter;
import java.sql.SQLException;

import javax.management.ObjectInstance;

import org.apache.commons.dbcp.BasicDataSource;
import org.nuxeo.ecm.core.management.jtajca.DatabaseConnectionMonitor;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.JmxAttributeGauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

public class CommonsDatabaseConnectionMonitor implements
        DatabaseConnectionMonitor {

    protected final String name;

    protected final BasicDataSource ds;

    protected final MetricRegistry metrics = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected ObjectInstance self;

    public CommonsDatabaseConnectionMonitor(String name, BasicDataSource ds) {
        this.name = parseName(name);
        this.ds = ds;
    }

    protected String parseName(String jdbcName) {
        int index = jdbcName.lastIndexOf('/');
        return jdbcName.substring(index + 1);
    }

    @Override
    public boolean getDefaultAutoCommit() {
        return ds.getDefaultAutoCommit();
    }

    @Override
    public void setDefaultAutoCommit(boolean defaultAutoCommit) {
        ds.setDefaultAutoCommit(defaultAutoCommit);
    }

    @Override
    public boolean getDefaultReadOnly() {
        return ds.getDefaultReadOnly();
    }

    @Override
    public void setDefaultReadOnly(boolean defaultReadOnly) {
        ds.setDefaultReadOnly(defaultReadOnly);
    }

    @Override
    public int getDefaultTransactionIsolation() {
        return ds.getDefaultTransactionIsolation();
    }

    @Override
    public void setDefaultTransactionIsolation(int defaultTransactionIsolation) {
        ds.setDefaultTransactionIsolation(defaultTransactionIsolation);
    }

    @Override
    public String getDefaultCatalog() {
        return ds.getDefaultCatalog();
    }

    @Override
    public void setDefaultCatalog(String defaultCatalog) {
        ds.setDefaultCatalog(defaultCatalog);
    }

    @Override
    public String getDriverClassName() {
        return ds.getDriverClassName();
    }

    @Override
    public int getMaxActive() {
        return ds.getMaxActive();
    }

    @Override
    public void setMaxActive(int maxActive) {
        ds.setMaxActive(maxActive);
    }

    @Override
    public int getMaxIdle() {
        return ds.getMaxIdle();
    }

    @Override
    public void setMaxIdle(int maxIdle) {
        ds.setMaxIdle(maxIdle);
    }

    @Override
    public int getMinIdle() {
        return ds.getMinIdle();
    }

    @Override
    public void setMinIdle(int minIdle) {
        ds.setMinIdle(minIdle);
    }

    @Override
    public int getInitialSize() {
        return ds.getInitialSize();
    }

    public void setInitialSize(int initialSize) {
        ds.setInitialSize(initialSize);
    }

    @Override
    public long getMaxWait() {
        return ds.getMaxWait();
    }

    @Override
    public void setMaxWait(long maxWait) {
        ds.setMaxWait(maxWait);
    }

    @Override
    public boolean isPoolPreparedStatements() {
        return ds.isPoolPreparedStatements();
    }

    @Override
    public void setPoolPreparedStatements(boolean poolingStatements) {
        ds.setPoolPreparedStatements(poolingStatements);
    }

    @Override
    public int getMaxOpenPreparedStatements() {
        return ds.getMaxOpenPreparedStatements();
    }

    @Override
    public void setMaxOpenPreparedStatements(int maxOpenStatements) {
        ds.setMaxOpenPreparedStatements(maxOpenStatements);
    }

    @Override
    public boolean getTestOnBorrow() {
        return ds.getTestOnBorrow();
    }

    @Override
    public void setTestOnBorrow(boolean testOnBorrow) {
        ds.setTestOnBorrow(testOnBorrow);
    }

    @Override
    public boolean getTestOnReturn() {
        return ds.getTestOnReturn();
    }

    @Override
    public void setTestOnReturn(boolean testOnReturn) {
        ds.setTestOnReturn(testOnReturn);
    }

    @Override
    public long getTimeBetweenEvictionRunsMillis() {
        return ds.getTimeBetweenEvictionRunsMillis();
    }

    @Override
    public void setTimeBetweenEvictionRunsMillis(
            long timeBetweenEvictionRunsMillis) {
        ds.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
    }

    @Override
    public int getNumTestsPerEvictionRun() {
        return ds.getNumTestsPerEvictionRun();
    }

    @Override
    public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        ds.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
    }

    @Override
    public long getMinEvictableIdleTimeMillis() {
        return ds.getMinEvictableIdleTimeMillis();
    }

    @Override
    public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        ds.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
    }

    @Override
    public boolean getTestWhileIdle() {
        return ds.getTestWhileIdle();
    }

    @Override
    public void setTestWhileIdle(boolean testWhileIdle) {
        ds.setTestWhileIdle(testWhileIdle);
    }

    @Override
    public int getNumActive() {
        return ds.getNumActive();
    }

    @Override
    public int getNumIdle() {
        return ds.getNumIdle();
    }

    public String getPassword() {
        return ds.getPassword();
    }

    @Override
    public String getUrl() {
        return ds.getUrl();
    }

    @Override
    public String getUsername() {
        return ds.getUsername();
    }

    @Override
    public String getValidationQuery() {
        return ds.getValidationQuery();
    }

    @Override
    public void setValidationQuery(String validationQuery) {
        ds.setValidationQuery(validationQuery);
    }

    @Override
    public int getValidationQueryTimeout() {
        return ds.getValidationQueryTimeout();
    }

    @Override
    public void setValidationQueryTimeout(int timeout) {
        ds.setValidationQueryTimeout(timeout);
    }

    @Override
    public boolean isAccessToUnderlyingConnectionAllowed() {
        return ds.isAccessToUnderlyingConnectionAllowed();
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return ds.getLoginTimeout();
    }

    @Override
    public void setLoginTimeout(int loginTimeout) throws SQLException {
        ds.setLoginTimeout(loginTimeout);
    }

    public void setLogWriter(PrintWriter logWriter) throws SQLException {
        ds.setLogWriter(logWriter);
    }

    @Override
    public boolean getRemoveAbandoned() {
        return ds.getRemoveAbandoned();
    }

    @Override
    public void setRemoveAbandoned(boolean removeAbandoned) {
        ds.setRemoveAbandoned(removeAbandoned);
    }

    @Override
    public int getRemoveAbandonedTimeout() {
        return ds.getRemoveAbandonedTimeout();
    }

    @Override
    public void setRemoveAbandonedTimeout(int removeAbandonedTimeout) {
        ds.setRemoveAbandonedTimeout(removeAbandonedTimeout);
    }

    @Override
    public boolean getLogAbandoned() {
        return ds.getLogAbandoned();
    }

    @Override
    public void setLogAbandoned(boolean logAbandoned) {
        ds.setLogAbandoned(logAbandoned);
    }

    @Override
    public void removeConnectionProperty(String name) {
        ds.removeConnectionProperty(name);
    }

    @Override
    public boolean isClosed() {
        return ds.isClosed();
    }

    @Override
    public void install() {
        self = DefaultMonitorComponent.bind(DatabaseConnectionMonitor.class,
                this, name);
        metrics.register(
                MetricRegistry.name("nuxeo", "datasources", name, "idle"),
                new JmxAttributeGauge(self.getObjectName(), "NumIdle"));
        metrics.register(
                MetricRegistry.name("nuxeo", "datasources", name, "active"),
                new JmxAttributeGauge(self.getObjectName(), "NumActive"));
    }

    @Override
    public void uninstall() {
        DefaultMonitorComponent.unbind(self);
        metrics.remove(MetricRegistry.name("nuxeo", "datasources", name, "idle"));
        metrics.remove(MetricRegistry.name("nuxeo", "datasources", name, "active"));
        self = null;
    }

    @Override
    public void setAccessToUnderlyingConnectionAllowed(boolean allow) {
        ds.setAccessToUnderlyingConnectionAllowed(allow);
    }

    @Override
    public void addConnectionProperty(String name, String value) {
        ds.addConnectionProperty(name, value);
    }

    @Override
    public void setConnectionProperties(String connectionProperties) {
        ds.setConnectionProperties(connectionProperties);
    }

}
