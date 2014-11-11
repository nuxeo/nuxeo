/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map.Entry;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.Credentials;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.ModelSetup;
import org.nuxeo.ecm.core.storage.sql.RepositoryBackend;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.runtime.api.Framework;

/**
 * JDBC Backend for a repository.
 */
public class JDBCBackend implements RepositoryBackend {

    private static final Log log = LogFactory.getLog(JDBCBackend.class);

    private RepositoryImpl repository;

    private XADataSource xadatasource;

    private Dialect dialect;

    private SQLInfo sqlInfo;

    private ClusterNodeHandler clusterNodeHandler;

    public JDBCBackend() {
    }

    public void initialize(RepositoryImpl repository) throws StorageException {
        this.repository = repository;
        RepositoryDescriptor repositoryDescriptor = repository.getRepositoryDescriptor();

        // instantiate the datasource
        String className = repositoryDescriptor.xaDataSourceName;
        Class<?> klass;
        try {
            klass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new StorageException("Unknown class: " + className, e);
        }
        Object instance;
        try {
            instance = klass.newInstance();
        } catch (Exception e) {
            throw new StorageException(
                    "Cannot instantiate class: " + className, e);
        }
        if (!(instance instanceof XADataSource)) {
            throw new StorageException("Not a XADataSource: " + className);
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
                name = Character.toLowerCase(name.charAt(0))
                        + name.substring(1);
            }
            try {
                BeanUtils.setProperty(xadatasource, name, value);
            } catch (Exception e) {
                log.error(String.format("Cannot set %s = %s", name, value));
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Opens a connection to get the dialect and finish initializing the
     * {@link ModelSetup}.
     */
    public void initializeModelSetup(ModelSetup modelSetup)
            throws StorageException {
        try {
            XAConnection xaconnection = xadatasource.getXAConnection();
            Connection connection = null;
            try {
                connection = xaconnection.getConnection();
                dialect = Dialect.createDialect(connection,
                        repository.getBinaryManager(),
                        repository.getRepositoryDescriptor());
            } finally {
                if (connection != null) {
                    connection.close();
                }
                xaconnection.close();
            }
        } catch (SQLException e) {
            throw new StorageException(e);
        }
        modelSetup.materializeFulltextSyntheticColumn = dialect.getMaterializeFulltextSyntheticColumn();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Creates the {@link SQLInfo} from the model and the dialect.
     */
    public void initializeModel(Model model) throws StorageException {
        sqlInfo = new SQLInfo(repository, model, dialect);
    }

    public Mapper newMapper(Model model, PathResolver pathResolver,
            Credentials credentials, boolean create) throws StorageException {
        Mapper mapper = createMapper(model, pathResolver);
        if (create) {
            // first connection, initialize the database
            mapper.createDatabase();
            // create cluster mapper if needed
            RepositoryDescriptor repositoryDescriptor = repository.getRepositoryDescriptor();
            if (repositoryDescriptor.clusteringEnabled) {
                log.info("Clustering enabled with "
                        + repositoryDescriptor.clusteringDelay
                        + " ms delay for repository: " + repository.getName());
                // use the mapper that created the database as cluster mapper
                clusterNodeHandler = new ClusterNodeHandler(mapper,
                        repositoryDescriptor);
                mapper = createMapper(model, pathResolver);
            }
        }
        return mapper;
    }

    protected JDBCMapper createMapper(Model model, PathResolver pathResolver)
            throws StorageException {
        return new JDBCMapper(model, pathResolver, sqlInfo, xadatasource,
                clusterNodeHandler);
    }

    public void shutdown() throws StorageException {
        if (clusterNodeHandler != null) {
            clusterNodeHandler.close();
        }
    }

}
