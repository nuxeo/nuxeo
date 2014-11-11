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

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.DirectoryServiceImpl;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

public class TestIntIdField extends SQLDirectoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.directory.sql.tests", "intIdDirectory-contrib.xml");
    }

    @SuppressWarnings("boxing")
    public void testIntIdDirectory() throws Exception {
        DirectoryServiceImpl dirServiceImpl =
            (DirectoryServiceImpl) Framework.getRuntime().getComponent(DirectoryService.NAME);

        Session session = dirServiceImpl.open("testIdDirectory");
        assertNotNull(session);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", 1);
        map.put("label", "toto");
        DocumentModel entry = session.createEntry(map);
        assertNotNull(entry);

        map.put("id", 2);
        map.put("label", "titi");
        DocumentModel entry2 = session.createEntry(map);
        assertNotNull(entry2);
        session.commit();

        assertNotNull(session.getEntry("1"));
        assertNotNull(session.getEntry("2"));
        assertNull(session.getEntry("3"));
    }

}
