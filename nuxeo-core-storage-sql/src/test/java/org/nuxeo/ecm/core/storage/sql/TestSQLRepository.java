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

package org.nuxeo.ecm.core.storage.sql;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;

/**
 * @author Florent Guillaume
 */
public class TestSQLRepository extends SQLRepositoryTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        openSession();
    }

    @Override
    protected void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    public void testBasics() throws Exception {
        DocumentModel root = session.getRootDocument();
        String name = "domain123";
        DocumentModel child = new DocumentModelImpl(root.getPathAsString(),
                name, "Domain");
        child = session.createDocument(child);
        session.save();

        child.setProperty("dublincore", "title", "The title");
        Calendar cal = new GregorianCalendar(2008, 7, 14, 12, 34, 0);
        child.setProperty("dublincore", "modified", cal);
        session.saveDocument(child);
        session.save();
        closeSession();

        // ----- new session -----
        openSession();
        root = session.getRootDocument();
        child = session.getChild(root.getRef(), name);

        String title = (String) child.getProperty("dublincore", "title");
        assertEquals("The title", title);
        String description = (String) child.getProperty("dublincore",
                "description");
        assertNull(description);
        Calendar modified = (Calendar) child.getProperty("dublincore",
                "modified");
        assertEquals(cal, modified);
    }

}
