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
import java.io.IOException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.nuxeo.common.mock.jndi.MockContextFactory;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Florent Guillaume
 */
public abstract class SQLStorageTestCase extends NXRuntimeTestCase {

    public final static String DATASOURCE_NAME = "java:/nuxeo-repo-datasource";

    public Repository repository;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.core.schema");

        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        assertNotNull(schemaManager);

        prepareDataSource();

        RepositoryDescriptor descriptor = new RepositoryDescriptor();
        descriptor.dataSourceName = DATASOURCE_NAME;
        repository = new RepositoryImpl(descriptor, schemaManager);
    }

    @Override
    protected void tearDown() throws Exception {
        if (repository != null) {
            repository.close();
        }
        super.tearDown();
    }



    protected void prepareDataSource() throws IOException, NamingException {
        // set a mock initial context
        MockContextFactory.setAsInitial();

        // clear the initial database
        File dbdir = new File("target/test/repository");
        deleteRecursive(dbdir);
        org.apache.derby.jdbc.EmbeddedXADataSource datasource = new org.apache.derby.jdbc.EmbeddedXADataSource();
        datasource.setCreateDatabase("create");
        datasource.setDatabaseName(dbdir.getAbsolutePath());
        datasource.setUser("sa");
        datasource.setPassword("");

        // bind the datasource in the initial context
        Context context = new InitialContext();
        context.bind(DATASOURCE_NAME, datasource);
    }

    protected static void deleteRecursive(File file) {
        if (file.isDirectory()) {
            for (String child : file.list()) {
                deleteRecursive(new File(file, child));
            }
        }
        file.delete();
    }

}
