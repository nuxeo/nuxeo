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

    /* Constant mentioned in the ...-derby-contrib.xml file: */

    /** This directory will be deleted and recreated. */
    private static final String DERBY_DIRECTORY = "target/test/derby";

    private static final String DERBY_LOG = "target/test/derby.log";

    private static final String CONTRIB_XML = "OSGI-INF/test-repo-repository-derby-contrib.xml";

    @Override
    public void setUp() {
        File dbdir = new File(DERBY_DIRECTORY);
        File parent = dbdir.getParentFile();
        FileUtils.deleteTree(dbdir);
        parent.mkdirs();
        System.setProperty("derby.stream.error.file",
                new File(DERBY_LOG).getAbsolutePath());
        // the following noticeably improves performance
        System.setProperty("derby.system.durability", "test");
    }

    @Override
    public void tearDown() throws Exception {
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (SQLException e) {
            if ("Derby system shutdown.".equals(e.getMessage())) {
                return;
            }
        }
        throw new RuntimeException("Expected Derby shutdown exception");
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
        properties.put("databaseName",
                new File(DERBY_DIRECTORY).getAbsolutePath());
        properties.put("user", "sa");
        properties.put("password", "");
        descriptor.properties = properties;
        return descriptor;
    }

}
