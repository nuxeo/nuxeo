/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.directory.sql;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.directory.DirectoryServiceImpl;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

public class TestFilterDirectories extends SQLDirectoryTestCase {

     @Override
        public void setUp() throws Exception {
            super.setUp();
            deployContrib("org.nuxeo.ecm.directory.sql.tests", "filterDirectoryContrib.xml");
        }

        public void testFilterDirectory() throws Exception {

            DirectoryServiceImpl dirServiceImpl =
                (DirectoryServiceImpl) Framework.getRuntime().getComponent(DirectoryService.NAME);

            Session unfiltredSession = dirServiceImpl.open("unfiltredTestDirectory");
            assertNotNull(unfiltredSession);
            assertEquals(5, unfiltredSession.getEntries().size());
            assertNotNull(unfiltredSession.getEntry("1"));
            assertNotNull(unfiltredSession.getEntry("2"));
            assertNotNull(unfiltredSession.getEntry("5"));

            Map<String, Serializable> queryFilter = new HashMap<String, Serializable>();
            queryFilter.put("lang", "en");
            assertEquals(2, unfiltredSession.query(queryFilter).size());

            Session filtredSession = dirServiceImpl.open("filtredTestDirectory");
            assertNotNull(filtredSession);
            assertEquals(2, filtredSession.getEntries().size());
            assertNotNull(filtredSession.getEntry("1"));
            assertNull(filtredSession.getEntry("2"));
            assertNull(filtredSession.getEntry("5"));
            assertEquals(1, filtredSession.query(queryFilter).size());
        }

}
