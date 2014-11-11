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

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.io.File;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.nuxeo.ecm.core.query.test.QueryTestCase;

/**
 * @author Florent Guillaume
 */
public class TestSQLRepositoryQuery extends QueryTestCase {

    @Override
    public void deployRepository() throws Exception {
        File testdir = new File("target/test");
        testdir.mkdirs();
        File dbdir = new File(testdir, "repository");
        deleteRecursive(dbdir);
        System.setProperty("derby.stream.error.file", new File(testdir,
                "derby.log").getAbsolutePath());
        deployContrib("org.nuxeo.ecm.core.storage.sql.tests",
                "OSGI-INF/test-repo-repository-contrib.xml");
    }

    protected static void deleteRecursive(File file) {
        if (file.isDirectory()) {
            for (String child : file.list()) {
                deleteRecursive(new File(file, child));
            }
        }
        file.delete();
    }

    @Override
    public void undeployRepository() throws Exception {
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
            fail("Expected Derby shutdown exception");
        } catch (SQLException e) {
            assertEquals("Derby system shutdown.", e.getMessage());
        }
    }

}
