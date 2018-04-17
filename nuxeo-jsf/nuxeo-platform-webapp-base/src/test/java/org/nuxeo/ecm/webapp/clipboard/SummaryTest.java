/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: SummaryTest.java 21692 2007-07-01 07:57:27Z sfermigier $
 */

package org.nuxeo.ecm.webapp.clipboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class SummaryTest {

    private Summary summary;

    protected static String getDate() {
        return SummaryEntry.getDateFormat().format(new Date());
    }

    @Before
    public void setUp() throws Exception {
        summary = new SummaryImpl();
        SummaryEntry rootEntry = new SummaryEntry("0", "root", getDate(), "", "");
        rootEntry.setDocumentRef(new IdRef("0"));
        summary.put(new IdRef("0").toString(), rootEntry);
    }

    @Test
    public void testHasChild() {
        assertFalse(summary.hasChild(summary.getSummaryRoot()));

        // Add new child to root
        SummaryEntry childEntry = new SummaryEntry("1", "child 2", getDate(), "attached file", "1.0");
        childEntry.setDocumentRef(new IdRef("1"));
        childEntry.setParent(summary.getSummaryRoot());
        summary.put(childEntry.getPath(), childEntry);

        assertFalse(summary.hasChild(childEntry));
        assertTrue(summary.hasChild(summary.getSummaryRoot()));

        // Add new child to childEntry1
        SummaryEntry childEntry2 = new SummaryEntry("2", "child 1", getDate(), "attached file", "1.0");
        childEntry2.setDocumentRef(new IdRef("2"));
        childEntry2.setParent(childEntry);
        summary.put(childEntry2.getPath(), childEntry2);

        assertFalse(summary.hasChild(childEntry2));
        assertTrue(summary.hasChild(childEntry));
    }

    @Test
    public void testGetChildren() {
        // Add new child to root
        SummaryEntry childEntry = new SummaryEntry("1", "child 1", getDate(), "attached file", "1.0");
        childEntry.setDocumentRef(new IdRef("1"));
        childEntry.setParent(summary.getSummaryRoot());
        summary.put(childEntry.getPath(), childEntry);

        // Add new child to root
        SummaryEntry childEntry2 = new SummaryEntry("2", "child 2", getDate(), "attached file", "1.0");
        childEntry2.setDocumentRef(new IdRef("3"));
        childEntry2.setParent(summary.getSummaryRoot());
        summary.put(childEntry2.getPath(), childEntry2);

        assertNotNull(summary.getChildren(summary.getSummaryRoot()));
        assertNull(summary.getChildren(childEntry));
        assertNull(summary.getChildren(childEntry2));
        assertEquals(2, summary.getChildren(summary.getSummaryRoot()).size());
    }

    @Test
    public void testGetPath() {
        // Add new child to root
        SummaryEntry childEntry = new SummaryEntry("1", "child 1", getDate(), "attached file", "1.0");
        childEntry.setDocumentRef(new IdRef("1"));
        childEntry.setParent(summary.getSummaryRoot());

        // Add new child to root
        SummaryEntry childEntry2 = new SummaryEntry("2", "child 2", getDate(), "attached file", "1.0");
        childEntry2.setDocumentRef(new IdRef("2"));
        childEntry2.setParent(summary.getSummaryRoot());

        // Add new child to child 2
        SummaryEntry childEntry3 = new SummaryEntry("3", "child 3", getDate(), "attached file", "1.0");
        childEntry3.setDocumentRef(new IdRef("3"));
        childEntry3.setParent(childEntry2);

        assertEquals("root/child 2/child 3", childEntry3.getPath());
        assertEquals("root/child 2", childEntry2.getPath());
        assertEquals("root/child 1", childEntry.getPath());
    }

    @Test
    public void testCompareSummaryEntry() {
        // Add new child to root
        SummaryEntry childEntry = new SummaryEntry("1", "child 1", getDate(), "attached file", "1.0");
        childEntry.setDocumentRef(new IdRef("1"));
        childEntry.setParent(summary.getSummaryRoot());

        // Add new child to root
        SummaryEntry childEntry2 = new SummaryEntry("2", "child 2", getDate(), "attached file", "1.0");
        childEntry2.setDocumentRef(new IdRef("2"));
        childEntry2.setParent(summary.getSummaryRoot());

        // Add new child to child 2
        SummaryEntry childEntry3 = new SummaryEntry("3", "child 3", getDate(), "attached file", "1.0");
        childEntry3.setDocumentRef(new IdRef("3"));
        childEntry3.setParent(childEntry2);

        // Add new child to root with same param as child 3
        SummaryEntry childEntry4 = new SummaryEntry("3", "child 3", getDate(), "attached file", "1.0");
        childEntry4.setDocumentRef(new IdRef("3"));
        childEntry4.setParent(summary.getSummaryRoot());

        // Add new child child 2 with same param as child 3
        SummaryEntry childEntry5 = new SummaryEntry("3", "child 3", getDate(), "attached file", "1.0");
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
