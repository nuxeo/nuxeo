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
 * $Id: SummaryTest.java 21692 2007-07-01 07:57:27Z sfermigier $
 */

package org.nuxeo.ecm.webapp.clipboard;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class SummaryTest extends NXRuntimeTestCase {

    private static final DateFormat DATE_PARSER = new SimpleDateFormat(
            "dd-MM-yyyy HH:mm:ss");

    private Summary summary;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        summary = new SummaryImpl();

        SummaryEntry rootEntry = new SummaryEntry("0", "root",
                DATE_PARSER.format(new Date()), "", "");
        rootEntry.setDocumentRef(new IdRef("0"));
        summary.put(new IdRef("0").toString(), rootEntry);
    }

    public void testHasChild() {
        assertFalse(summary.hasChild(summary.getSummaryRoot()));

        // Add new child to root
        SummaryEntry childEntry = new SummaryEntry("1", "child 2",
                DATE_PARSER.format(new Date()), "attached file", "1.0");
        childEntry.setDocumentRef(new IdRef("1"));
        childEntry.setParent(summary.getSummaryRoot());
        summary.put(childEntry.getPath(), childEntry);

        assertFalse(summary.hasChild(childEntry));
        assertTrue(summary.hasChild(summary.getSummaryRoot()));

        // Add new child to childEntry1
        SummaryEntry childEntry2 = new SummaryEntry("2", "child 1",
                DATE_PARSER.format(new Date()), "attached file", "1.0");
        childEntry2.setDocumentRef(new IdRef("2"));
        childEntry2.setParent(childEntry);
        summary.put(childEntry2.getPath(), childEntry2);

        assertFalse(summary.hasChild(childEntry2));
        assertTrue(summary.hasChild(childEntry));
    }

    public void testGetChildren() {
        // Add new child to root
        SummaryEntry childEntry = new SummaryEntry("1", "child 1",
                DATE_PARSER.format(new Date()), "attached file", "1.0");
        childEntry.setDocumentRef(new IdRef("1"));
        childEntry.setParent(summary.getSummaryRoot());
        summary.put(childEntry.getPath(), childEntry);

        // Add new child to root
        SummaryEntry childEntry2 = new SummaryEntry("2", "child 2",
                DATE_PARSER.format(new Date()), "attached file", "1.0");
        childEntry2.setDocumentRef(new IdRef("3"));
        childEntry2.setParent(summary.getSummaryRoot());
        summary.put(childEntry2.getPath(), childEntry2);

        assertNotNull(summary.getChildren(summary.getSummaryRoot()));
        assertNull(summary.getChildren(childEntry));
        assertNull(summary.getChildren(childEntry2));
        assertEquals(2, summary.getChildren(summary.getSummaryRoot()).size());
    }

    public void testGetPath() {
        // Add new child to root
        SummaryEntry childEntry = new SummaryEntry("1", "child 1",
                DATE_PARSER.format(new Date()), "attached file", "1.0");
        childEntry.setDocumentRef(new IdRef("1"));
        childEntry.setParent(summary.getSummaryRoot());

        // Add new child to root
        SummaryEntry childEntry2 = new SummaryEntry("2", "child 2",
                DATE_PARSER.format(new Date()), "attached file", "1.0");
        childEntry2.setDocumentRef(new IdRef("2"));
        childEntry2.setParent(summary.getSummaryRoot());

        // Add new child to child 2
        SummaryEntry childEntry3 = new SummaryEntry("3", "child 3",
                DATE_PARSER.format(new Date()), "attached file", "1.0");
        childEntry3.setDocumentRef(new IdRef("3"));
        childEntry3.setParent(childEntry2);

        assertEquals("root/child 2/child 3", childEntry3.getPath());
        assertEquals("root/child 2", childEntry2.getPath());
        assertEquals("root/child 1", childEntry.getPath());
    }

    @SuppressWarnings({"SimplifiableJUnitAssertion"})
    public void testCompareSummaryEntry() {
        // Add new child to root
        SummaryEntry childEntry = new SummaryEntry("1", "child 1",
                DATE_PARSER.format(new Date()), "attached file", "1.0");
        childEntry.setDocumentRef(new IdRef("1"));
        childEntry.setParent(summary.getSummaryRoot());

        // Add new child to root
        SummaryEntry childEntry2 = new SummaryEntry("2", "child 2",
                DATE_PARSER.format(new Date()), "attached file", "1.0");
        childEntry2.setDocumentRef(new IdRef("2"));
        childEntry2.setParent(summary.getSummaryRoot());

        // Add new child to child 2
        SummaryEntry childEntry3 = new SummaryEntry("3", "child 3",
                DATE_PARSER.format(new Date()), "attached file", "1.0");
        childEntry3.setDocumentRef(new IdRef("3"));
        childEntry3.setParent(childEntry2);

        // Add new child to root with same param as child 3
        SummaryEntry childEntry4 = new SummaryEntry("3", "child 3",
                DATE_PARSER.format(new Date()), "attached file", "1.0");
        childEntry4.setDocumentRef(new IdRef("3"));
        childEntry4.setParent(summary.getSummaryRoot());

        // Add new child child 2 with same param as child 3
        SummaryEntry childEntry5 = new SummaryEntry("3", "child 3",
                DATE_PARSER.format(new Date()), "attached file", "1.0");
        childEntry5.setDocumentRef(new IdRef("3"));
        childEntry5.setParent(childEntry2);

        assertTrue(childEntry3.compareTo(childEntry) > 0);
        assertTrue(childEntry.compareTo(childEntry2) < 0);
        assertFalse(childEntry3.equals(childEntry4));
        assertFalse(childEntry4.compareTo(childEntry3) == 0);
        assertTrue(childEntry5.compareTo(childEntry3) == 0);
        assertTrue(childEntry3.equals(childEntry5));
        assertEquals(childEntry3.hashCode(), childEntry5.hashCode());
    }

}
