/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.sql;

import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryServiceImpl;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public abstract class SQLDirectoryTestCase extends NXRuntimeTestCase {

    private DataSource dataSource;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.directory.sql.tests",
                "test-CoreService.xml");
        deployContrib("org.nuxeo.ecm.directory.sql.tests",
                "test-TypeService.xml");

        deployContrib("org.nuxeo.ecm.directory.sql.tests",
                "sql-test-setup/DirectoryTypes.xml");
        deployContrib("org.nuxeo.ecm.directory.sql.tests",
                "sql-test-setup/DirectoryService.xml");
        deployContrib("org.nuxeo.ecm.directory.sql.tests",
                "sql-test-setup/SQLDirectoryFactory.xml");
        deployContrib("org.nuxeo.ecm.directory.sql.tests",
                "test-sql-directories-bundle.xml");
    }

    protected static Session getSession(String dirName)
            throws ClientException {
        DirectoryService dirService =
                Framework.getLocalService(DirectoryService.class);
        return dirService.open(dirName);
    }

    protected static Directory getDirectory(String dirName)
            throws DirectoryException {
        DirectoryServiceImpl dirServiceImpl =
            (DirectoryServiceImpl) Framework.getRuntime().getComponent(DirectoryService.NAME);
        Directory dir = dirServiceImpl.getDirectory(dirName);
        if (dir instanceof SQLDirectoryProxy) {
            dir = ((SQLDirectoryProxy) dir).getDirectory();
        }
        return dir;
    }

    public Connection getConnection() throws Exception {
        if (null == dataSource) {
            dataSource = createDataSource();
        }
        return dataSource.getConnection();
    }

    public static void setUpContextFactory() {
        Properties props = System.getProperties();
        props.put("java.naming.factory.initial",
                "org.nuxeo.ecm.directory.sql.LocalContextFactory");
    }

    public static DataSource createDataSource() {
        return new SimpleDataSource("jdbc:hsqldb:mem:memid",
                "org.hsqldb.jdbcDriver", "sa", "");
    }

}
