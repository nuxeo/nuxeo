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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Florent Guillaume
 */
public abstract class SQLBackendTestCase extends NXRuntimeTestCase {

    private static final String DATABASE_DIRECTORY = "target/test/repository";

    public Repository repository;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.core.schema");

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
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
            fail("Expected Derby shutdown exception");
        } catch (SQLException e) {
            assertEquals("Derby system shutdown.", e.getMessage());
        }
        super.tearDown();
    }

    protected File prepareDBDirectory() {
        File dbdir = new File(DATABASE_DIRECTORY);
        FileUtils.deleteTree(dbdir);
        return dbdir;
    }

    protected RepositoryDescriptor prepareDescriptor() {
        System.setProperty(
                "derby.stream.error.file",
                new File(Framework.getRuntime().getHome(), "derby.log").getAbsolutePath());
        RepositoryDescriptor descriptor = new RepositoryDescriptor();
        String className = org.apache.derby.jdbc.EmbeddedXADataSource.class.getName();
        descriptor.xaDataSourceName = className;
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("createDatabase", "create");
        properties.put("databaseName", prepareDBDirectory().getAbsolutePath());
        properties.put("user", "sa");
        properties.put("password", "");
        descriptor.properties = properties;
        return descriptor;
    }

    protected RepositoryDescriptor prepareDescriptorPG() {
        RepositoryDescriptor descriptor = new RepositoryDescriptor();
        descriptor.xaDataSourceName = "org.postgresql.xa.PGXADataSource";
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("ServerName", "localhost");
        properties.put("PortNumber", "5432");
        properties.put("DatabaseName", "nuxeo");
        properties.put("User", "postgres");
        properties.put("Password", "");
        descriptor.properties = properties;
        return descriptor;
    }

}
