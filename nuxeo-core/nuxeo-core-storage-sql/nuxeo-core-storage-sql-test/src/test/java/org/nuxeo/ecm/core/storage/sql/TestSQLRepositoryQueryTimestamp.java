/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Mariana Cedica
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;

public class TestSQLRepositoryQueryTimestamp extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.core.query.test",
                "OSGI-INF/core-types-contrib.xml");
        session = openSessionAs((String) null); // system
    }

    protected Date setupDocTest() throws ClientException {
        Date currentDate = new Date();
        DocumentModel testDocument = new DocumentModelImpl("/", "testfolder1",
                "Folder");
        testDocument.setPropertyValue("dc:title", "test");
        testDocument.setPropertyValue("dc:modified", currentDate);
        testDocument = session.createDocument(testDocument);
        session.save();
        return ((Calendar) testDocument.getPropertyValue("dc:modified")).getTime();
    }

    protected static Date addSecond(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.SECOND, 1);
        return cal.getTime();
    }

    protected static String formatTimestamp(Date date) {
        return new SimpleDateFormat("'TIMESTAMP' ''yyyy-MM-dd HH:mm:ss.SSS''").format(date);
    }

    public void testEqualsTimeWithMilliseconds() throws ClientException {
        Date currentDate = setupDocTest();
        String testQuery = String.format(
                "SELECT * FROM Folder WHERE dc:title = 'test' AND dc:modified = %s"
                        + " AND ecm:isProxy = 0", formatTimestamp(currentDate));
        DocumentModelList docs = session.query(testQuery);
        assertEquals(1, docs.size());
    }

    public void testLTTimeWithMilliseconds() throws ClientException {
        Date currentDate = setupDocTest();
        // add a second to be sure that the document is found
        currentDate = addSecond(currentDate);
        String testQuery = String.format(
                "SELECT * FROM Folder WHERE dc:title = 'test' AND dc:modified < %s"
                        + " AND ecm:isProxy = 0", formatTimestamp(currentDate));
        DocumentModelList docs = session.query(testQuery);
        assertEquals(1, docs.size());
    }

}
