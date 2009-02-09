/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Florent Guillaume
 */
public abstract class SQLBackendTestCase extends NXRuntimeTestCase {

    public Repository repository;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        SQLBackendHelper.setUpRepository();

        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        assertNotNull(schemaManager);

        RepositoryDescriptor descriptor = prepareDescriptor();
        repository = new RepositoryImpl(descriptor, schemaManager);
    }

    @Override
    protected void tearDown() throws Exception {
        if (repository != null) {
            repository.close();
        }
        SQLBackendHelper.tearDownRepository();
        super.tearDown();
    }

    protected RepositoryDescriptor prepareDescriptor() {
        switch (SQLBackendHelper.DATABASE) {
        case DERBY:
            return prepareDescriptorDerby();
        case H2:
            return prepareDescriptorH2();
        case MYSQL:
            return prepareDescriptorMySQL();
        case POSTGRESQL:
            return prepareDescriptorPostgreSQL();
        }
        throw new RuntimeException(); // not reached
    }

    protected RepositoryDescriptor prepareDescriptorDerby() {
        RepositoryDescriptor descriptor = new RepositoryDescriptor();
        descriptor.xaDataSourceName = "org.apache.derby.jdbc.EmbeddedXADataSource";
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("createDatabase", "create");
        properties.put("databaseName", new File(
                SQLBackendHelper.DERBY_DIRECTORY).getAbsolutePath());
        properties.put("user", "sa");
        properties.put("password", "");
        descriptor.properties = properties;
        return descriptor;
    }

    protected RepositoryDescriptor prepareDescriptorH2() {
        RepositoryDescriptor descriptor = new RepositoryDescriptor();
        descriptor.xaDataSourceName = "org.h2.jdbcx.JdbcDataSource";
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("URL", String.format("jdbc:h2:${%s}",
                SQLBackendHelper.H2_PATH_PROPERTY));
        properties.put("User", SQLBackendHelper.H2_DATABASE_USER);
        properties.put("Password", SQLBackendHelper.H2_DATABASE_PASSWORD);
        descriptor.properties = properties;
        return descriptor;
    }

    protected RepositoryDescriptor prepareDescriptorMySQL() {
        RepositoryDescriptor descriptor = new RepositoryDescriptor();
        descriptor.xaDataSourceName = "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource";
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("ServerName", SQLBackendHelper.MYSQL_HOST);
        properties.put("PortNumber/Integer", SQLBackendHelper.MYSQL_PORT);
        properties.put("DatabaseName", SQLBackendHelper.MYSQL_DATABASE);
        properties.put("User", SQLBackendHelper.MYSQL_DATABASE_OWNER);
        properties.put("Password", SQLBackendHelper.MYSQL_DATABASE_PASSWORD);
        descriptor.properties = properties;
        return descriptor;
    }

    protected RepositoryDescriptor prepareDescriptorPostgreSQL() {
        RepositoryDescriptor descriptor = new RepositoryDescriptor();
        descriptor.xaDataSourceName = "org.postgresql.xa.PGXADataSource";
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("ServerName", SQLBackendHelper.PG_HOST);
        properties.put("PortNumber/Integer", SQLBackendHelper.PG_PORT);
        properties.put("DatabaseName", SQLBackendHelper.PG_DATABASE);
        properties.put("User", SQLBackendHelper.PG_DATABASE_OWNER);
        properties.put("Password", SQLBackendHelper.PG_DATABASE_PASSWORD);
        descriptor.properties = properties;
        return descriptor;
    }

}
