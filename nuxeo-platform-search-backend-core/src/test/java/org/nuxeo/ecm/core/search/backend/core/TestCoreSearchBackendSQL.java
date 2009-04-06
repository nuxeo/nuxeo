/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.search.backend.core;

import java.io.File;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.nuxeo.common.utils.FileUtils;

/*
 * @author Florent Guillaume
 */
public class TestCoreSearchBackendSQL extends CoreSearchBackendTestCase {

    /*
     * ----- Derby configuration -----
     */

    /** This directory will be deleted and recreated. */
    protected static final String DERBY_DIRECTORY = "target/test/derby";

    protected static final String DERBY_LOG = "target/test/derby.log";

    @Override
    protected void deployRepository() throws Exception {
        deployBundle("org.nuxeo.ecm.core.storage.sql");
        setUpRepositoryDerby();
        deployContrib("org.nuxeo.ecm.platform.search.backend.core.tests",
                "OSGI-INF/repository-sql-contrib.xml");
    }

    @Override
    protected void undeployRepository() throws Exception {
        tearDownRepositoryDerby();
    }

    /*
     * ----- Derby -----
     */

    protected static void setUpRepositoryDerby() {
        File dbdir = new File(DERBY_DIRECTORY);
        File parent = dbdir.getParentFile();
        FileUtils.deleteTree(dbdir);
        parent.mkdirs();
        System.setProperty("derby.stream.error.file",
                new File(DERBY_LOG).getAbsolutePath());
        // the following noticeably improves performance
        System.setProperty("derby.system.durability", "test");
    }

    protected static void tearDownRepositoryDerby() {
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (SQLException e) {
            if ("Derby system shutdown.".equals(e.getMessage())) {
                return;
            }
        }
        throw new RuntimeException("Expected Derby shutdown exception");
    }

}
