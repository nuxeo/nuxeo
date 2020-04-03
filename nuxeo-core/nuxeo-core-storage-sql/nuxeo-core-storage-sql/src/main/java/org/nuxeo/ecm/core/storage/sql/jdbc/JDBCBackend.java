/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.storage.FulltextDescriptor;
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
import org.nuxeo.runtime.datasource.ConnectionHelper;
import org.nuxeo.runtime.datasource.DataSourceHelper;
import org.nuxeo.runtime.datasource.PooledDataSourceRegistry.PooledDataSource;

/**
 * JDBC Backend for a repository.
 */
public class JDBCBackend implements RepositoryBackend {

    private static final Log log = LogFactory.getLog(JDBCBackend.class);

    private RepositoryImpl repository;

    private Model model;

    private SQLInfo sqlInfo;

    private ClusterInvalidator clusterInvalidator;

    private boolean isPooledDataSource;

    @Override
    public Model initialize(RepositoryImpl repository) {
        this.repository = repository;
        RepositoryDescriptor repositoryDescriptor = repository.getRepositoryDescriptor();
        String dataSourceName = JDBCConnection.getDataSourceName(repositoryDescriptor.name);

        // check datasource
        try {
            DataSource ds = DataSourceHelper.getDataSource(dataSourceName);
            if (ds instanceof PooledDataSource) {
                isPooledDataSource = true;
            }
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
        sqlInfo = new SQLInfo(model, dialect);

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
            Mapper mapper = newMapper(null, false);
            try {
                mapper.createDatabase(ddlMode);
            } finally {
                mapper.close();
            }
        }
        if (log.isDebugEnabled()) {
            FulltextDescriptor fulltextDescriptor = repositoryDescriptor.getFulltextDescriptor();
            log.debug(String.format("Database ready, fulltext: disabled=%b storedInBlob=%b searchDisabled=%b.",
                    fulltextDescriptor.getFulltextDisabled(), fulltextDescriptor.getFulltextStoredInBlob(),
                    fulltextDescriptor.getFulltextSearchDisabled()));
        }

        return model;
    }

    @Override
    public void setClusterInvalidator(ClusterInvalidator clusterInvalidator) {
        this.clusterInvalidator = clusterInvalidator;
    }

    @Override
    public Mapper newMapper(PathResolver pathResolver, boolean useInvalidations) {
        boolean noSharing = !useInvalidations;
        ClusterInvalidator cnh = useInvalidations ? clusterInvalidator : null;
        Mapper mapper = new JDBCMapper(model, pathResolver, sqlInfo, cnh, repository);
        if (isPooledDataSource) {
            mapper = JDBCMapperConnector.newConnector(mapper, noSharing);
        } else {
            mapper.connect(false);
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
