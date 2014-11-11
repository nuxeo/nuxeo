/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     George Lefter
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.sql;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryServiceImpl;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public abstract class SQLDirectoryTestCase extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.event");

        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");

        deployBundle("org.nuxeo.ecm.directory.types.contrib");
        // override user schema with intField & dateField
        deployContrib("org.nuxeo.ecm.directory.sql.tests",
                "test-sql-directories-schema-override.xml");

        DatabaseHelper.DATABASE.setUp();

        deployContrib("org.nuxeo.ecm.directory.sql.tests",
                "test-sql-directories-bundle.xml");
    }

    protected static Session getSession(String dirName) throws ClientException {
        DirectoryService dirService = Framework.getLocalService(DirectoryService.class);
        return dirService.open(dirName);
    }

    protected static Directory getDirectory(String dirName)
            throws DirectoryException {
        DirectoryServiceImpl dirServiceImpl = (DirectoryServiceImpl) Framework.getRuntime().getComponent(
                DirectoryService.NAME);
        Directory dir = dirServiceImpl.getDirectory(dirName);
        return dir;
    }

}
