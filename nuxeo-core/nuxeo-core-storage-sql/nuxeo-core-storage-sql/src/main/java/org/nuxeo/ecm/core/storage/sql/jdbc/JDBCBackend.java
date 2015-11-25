/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.jdbc;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.storage.sql.ClusterInvalidator;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Model.IdType;
import org.nuxeo.ecm.core.storage.sql.ModelSetup;
import org.nuxeo.ecm.core.storage.sql.RepositoryBackend;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.datasource.ConnectionHelper;
import org.nuxeo.runtime.datasource.DataSourceHelper;
import org.nuxeo.runtime.datasource.PooledDataSourceRegistry.PooledDataSource;

/**
 * JDBC Backend for a repository.
 */
public class JDBCBackend implements RepositoryBackend {

    private static final Log log = LogFactory.getLog(JDBCBackend.class);

    private RepositoryImpl repository;

    private String pseudoDataSourceName;

    private XADataSource xadatasource;

    private Dialect dialect;

    private SQLInfo sqlInfo;

    private boolean firstMapper = true;

    // static because in Nuxeo 6.0 if a component init fails, RegistrationInfoImpl marks it
    // as RESOLVED which means that the next lookup will re-create it, which in turn will
    // create a new RepositoryService instance and a new RepositoryImpl / JDBCBackend too.
    // FALSE if init failed, TRUE if succeeded, null if not yet done
    private static Map<String, Boolean> REPOSITORY_INITIALIZED = new HashMap<>();

    private ClusterInvalidator clusterInvalidator;

    private boolean isPooledDataSource;

    @Override
    public void initialize(RepositoryImpl repository) {
        this.repository = repository;
        RepositoryDescriptor repositoryDescriptor = repository.getRepositoryDescriptor();
        pseudoDataSourceName = ConnectionHelper.getPseudoDataSourceNameForRepository(repositoryDescriptor.name);

        try {
            DataSource ds = DataSourceHelper.getDataSource(pseudoDataSourceName);
            if (ds instanceof PooledDataSource) {
                isPooledDataSource = true;
                return;
            }
        } catch (NamingException cause) {;
        }

        // try single-datasource non-XA mode
        try (Connection connection = ConnectionHelper.getConnection(pseudoDataSourceName)) {
            if (connection != null) {
                return;
            }
        } catch (SQLException cause) {
            throw new NuxeoException("Connection error", cause);
        }

        // standard XA mode
        // instantiate the XA datasource
        String className = repositoryDescriptor.xaDataSourceName;
        Class<?> klass;
        try {
            klass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new NuxeoException("Unknown class: " + className, e);
        }
        Object instance;
        try {
            instance = klass.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("Cannot instantiate class: " + className, e);
        }
        if (!(instance instanceof XADataSource)) {
            throw new NuxeoException("Not a XADataSource: " + className);
        }
        xadatasource = (XADataSource) instance;

        // set JavaBean properties on the datasource
        for (Entry<String, String> entry : repositoryDescriptor.properties.entrySet()) {
            String name = entry.getKey();
            Object value = Framework.expandVars(entry.getValue());
            if (name.contains("/")) {
                // old syntax where non-String types were explicited
                name = name.substring(0, name.indexOf('/'));
            }
            // transform to proper JavaBean convention
            if (Character.isLowerCase(name.charAt(1))) {
                name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
            }
            try {
                BeanUtils.setProperty(xadatasource, name, value);
            } catch (ReflectiveOperationException e) {
                log.error(String.format("Cannot set %s = %s", name, value));
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Opens a connection to get the dialect and finish initializing the {@link ModelSetup}.
     */
    @Override
    public void initializeModelSetup(ModelSetup modelSetup) {
        try {
            XAConnection xaconnection = null;
            // try single-datasource non-XA mode
            Connection connection = ConnectionHelper.getConnection(pseudoDataSourceName);
            try {
                if (connection == null) {
                    // standard XA mode
                    xaconnection = xadatasource.getXAConnection();
                    connection = xaconnection.getConnection();
                }
                dialect = Dialect.createDialect(connection, repository.getRepositoryDescriptor());
            } finally {
                if (connection != null) {
                    connection.close();
                }
                if (xaconnection != null) {
                    xaconnection.close();
                }
            }
        } catch (SQLException cause) {
            throw new NuxeoException("Cannot connect to database", cause);
        }
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
    }

    /**
     * {@inheritDoc}
     * <p>
     * Creates the {@link SQLInfo} from the model and the dialect.
     */
    @Override
    public void initializeModel(Model model) {
        sqlInfo = new SQLInfo(model, dialect);
    }

    @Override
    public void setClusterInvalidator(ClusterInvalidator clusterInvalidator) {
        this.clusterInvalidator = clusterInvalidator;
    }

    @Override
    public Mapper newMapper(Model model, PathResolver pathResolver, boolean useInvalidations) {
        boolean noSharing = !useInvalidations;
        RepositoryDescriptor repositoryDescriptor = repository.getRepositoryDescriptor();

        ClusterInvalidator cnh = useInvalidations ? clusterInvalidator : null;
        Mapper mapper = new JDBCMapper(model, pathResolver, sqlInfo, xadatasource, cnh, noSharing, repository);
        if (isPooledDataSource) {
            mapper = JDBCMapperConnector.newConnector(mapper);
            if (noSharing) {
                mapper = JDBCMapperTxSuspender.newConnector(mapper);
            }
        } else {
            mapper.connect();
        }
        String repositoryName = repository.getName();
        if (FALSE.equals(REPOSITORY_INITIALIZED.get(repositoryName))) {
            throw new NuxeoException("Database initialization failed previously for: " + repositoryName);
        }
        if (firstMapper) {
            REPOSITORY_INITIALIZED.put(repositoryName, FALSE);
            firstMapper = false;
            String ddlMode = repositoryDescriptor.getDDLMode();
            if (ddlMode == null) {
                // compat
                ddlMode = repositoryDescriptor.getNoDDL() ? RepositoryDescriptor.DDL_MODE_IGNORE
                        : RepositoryDescriptor.DDL_MODE_EXECUTE;
            }
            if (ddlMode.equals(RepositoryDescriptor.DDL_MODE_IGNORE)) {
                log.info("Skipping database creation");
            } else {
                // first connection, initialize the database
                mapper.createDatabase(ddlMode);
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("Database ready, fulltext: disabled=%b searchDisabled=%b.",
                        repositoryDescriptor.getFulltextDisabled(), repositoryDescriptor.getFulltextSearchDisabled()));
            }
            REPOSITORY_INITIALIZED.put(repositoryName, TRUE);
        }
        return mapper;
    }

    @Override
    public void shutdown() {
        if (clusterInvalidator != null) {
            clusterInvalidator.close();
        }
    }

}
