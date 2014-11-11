/*
 * (C) Copyright 2008-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

/**
 * @author Florent Guillaume
 */
public class DatabaseDerby extends DatabaseHelper {

    public static DatabaseHelper INSTANCE = new DatabaseDerby();

    /** This directory will be deleted and recreated. */
    private static final String DIRECTORY = "target/test/derby";

    private static final String DEF_USER = "sa";

    private static final String DEF_PASSWORD = "";

    private static final String CONTRIB_XML = "OSGI-INF/test-repo-repository-derby-contrib.xml";

    private static final String LOG = "target/test/derby.log";

    private static void setProperties() {
        setProperty(DATABASE_PROPERTY, new File(DIRECTORY).getAbsolutePath());
        setProperty(USER_PROPERTY, DEF_USER);
        setProperty(PASSWORD_PROPERTY, DEF_PASSWORD);
    }

    @Override
    public void setUp() {
        File dbdir = new File(DIRECTORY);
        File parent = dbdir.getParentFile();
        FileUtils.deleteTree(dbdir);
        parent.mkdirs();
        System.setProperty("derby.stream.error.file",
                new File(LOG).getAbsolutePath());
        // the following noticeably improves performance
        System.setProperty("derby.system.durability", "test");
        setProperties();
    }

    @Override
    public void tearDown() throws Exception {
        Exception ex = null;
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (SQLException e) {
            String message = e.getMessage();
            if ("Derby system shutdown.".equals(message)) {
                return;
            }
            if ("org.apache.derby.jdbc.EmbeddedDriver is not registered with the JDBC driver manager".equals(message)) {
                // huh? happens for testClustering
                return;
            }
            ex = e;
        }
        throw new RuntimeException("Expected Derby shutdown exception instead",
                ex);
    }

    @Override
    public String getDeploymentContrib() {
        return CONTRIB_XML;
    }

    @Override
    public RepositoryDescriptor getRepositoryDescriptor() {
        RepositoryDescriptor descriptor = new RepositoryDescriptor();
        descriptor.xaDataSourceName = "org.apache.derby.jdbc.EmbeddedXADataSource";
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("createDatabase", "create");
        properties.put("databaseName", System.getProperty(DATABASE_PROPERTY));
        properties.put("user", System.getProperty(USER_PROPERTY));
        properties.put("password", System.getProperty(PASSWORD_PROPERTY));
        descriptor.properties = properties;
        return descriptor;
    }

    @Override
    public boolean supportsMultipleFulltextIndexes() {
        return false;
    }

}
