/*
 * (C) Copyright 2010-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.version.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.versioning.StandardVersioningService;
import org.nuxeo.runtime.test.runner.Deploy;

public class TestVersioningService extends AbstractTestVersioning {

    @Test
    public void testStandardVersioning() {
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        doc.setPropertyValue("dc:title", "A");
        doc = session.saveDocument(doc);
        DocumentRef docRef = doc.getRef();
        assertTrue(doc.isCheckedOut());
        assertVersion("0.0", doc);
        assertVersionLabel("0.0", doc);
        assertLatestVersion(null, doc);

        // save with no option, use default
        doc.setPropertyValue("dc:title", "B");
        doc = session.saveDocument(doc);
        assertTrue(doc.isCheckedOut());
        assertVersion("0.0", doc);
        assertVersionLabel("0.0", doc);
        assertLatestVersion(null, doc);

        // change and save with new minor
        doc.setPropertyValue("dc:title", "C");
        doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
        maybeSleepToNextSecond();
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertVersion("0.1", doc);
        assertVersionLabel("0.1", doc);
        assertLatestVersion("0.1", doc);
        DocumentModel v01 = session.getLastDocumentVersion(docRef);
        assertEquals(v01.getId(), session.getBaseVersion(docRef).reference());

        // checkout
        doc.checkOut();
        assertTrue(doc.isCheckedOut());
        assertVersion("0.1", doc);
        assertVersionLabel("0.1+", doc);
        assertLatestVersion("0.1", doc);

        // change and save with new major
        doc.setPropertyValue("dc:title", "D");
        doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MAJOR);
        maybeSleepToNextSecond();
        doc = session.saveDocument(doc);
        assertFalse(doc.isCheckedOut());
        assertVersion("1.0", doc);
        assertVersionLabel("1.0", doc);
        assertLatestVersion("1.0", doc);
        DocumentModel v10 = session.getLastDocumentVersion(docRef);
        assertEquals(v10.getId(), session.getBaseVersion(docRef).reference());

        // direct save for autocheckout
        doc.setPropertyValue("dc:title", "E");
        doc = session.saveDocument(doc);
        assertTrue(doc.isCheckedOut());
        assertVersion("1.0", doc);
        assertVersionLabel("1.0+", doc);
        assertLatestVersion("1.0", doc);

        // checkin
        maybeSleepToNextSecond();
        DocumentRef v11ref = doc.checkIn(VersioningOption.MINOR, "foo");
        assertFalse(doc.isCheckedOut());
        assertVersion("1.1", doc);
        assertVersionLabel("1.1", doc);
        assertLatestVersion("1.1", doc);
        assertEquals(v11ref.reference(), session.getBaseVersion(docRef).reference());

        // wait before doing a restore
        session.save();
        waitForAsyncCompletion();

        // restore 0.1
        doc = session.restoreToVersion(docRef, v01.getRef());
        assertFalse(doc.isCheckedOut());
        assertVersion("0.1", doc);
        assertVersionLabel("0.1", doc);
        assertLatestVersion("1.1", doc);
        assertEquals(v01.getId(), session.getBaseVersion(docRef).reference());

        // checkout restored version
        doc.checkOut();
        assertTrue(doc.isCheckedOut());
        assertVersion("1.1", doc);
        assertVersionLabel("1.1+", doc);
        assertLatestVersion("1.1", doc);

        // publish (checks in first)
        maybeSleepToNextSecond();
        DocumentModel proxy = session.publishDocument(doc, folder);
        assertFalse(doc.isCheckedOut());
        assertVersion("1.2", doc);
        assertVersionLabel("1.2", doc);
        assertLatestVersion("1.2", doc);
        assertVersion("1.2", proxy);

        // republish, no new version
        proxy = session.publishDocument(doc, folder);
        assertFalse(doc.isCheckedOut());
        assertVersion("1.2", doc);
        assertVersionLabel("1.2", doc);
        assertLatestVersion("1.2", doc);
        assertVersion("1.2", proxy);

        // do a change (autocheckout), and republish
        doc.setPropertyValue("dc:title", "F");
        session.saveDocument(doc);
        maybeSleepToNextSecond();
        proxy = session.publishDocument(doc, folder);
        assertFalse(doc.isCheckedOut());
        assertVersion("1.3", doc);
        assertVersionLabel("1.3", doc);
        assertLatestVersion("1.3", doc);
    }

    @Test
    public void testVersioningOnLiveProxy() {
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        DocumentModel section = session.createDocumentModel("/", "section", "Folder");
        section = session.createDocument(section);
        DocumentModel doc = session.createDocumentModel("/", "testfile1", "File");
        doc = session.createDocument(doc);
        doc.setPropertyValue("dc:title", "A");
        doc = session.saveDocument(doc);
        DocumentRef docRef = doc.getRef();
        assertTrue(doc.isCheckedOut());
        assertVersion("0.0", doc);
        assertVersionLabel("0.0", doc);
        assertLatestVersion(null, doc);

        // create a live proxy
        DocumentModel proxy = session.createProxy(doc.getRef(), section.getRef());
        assertTrue(proxy.isCheckedOut());
        assertVersion("0.0", proxy);
        assertVersionLabel("0.0", proxy);
        assertLatestVersion(null, proxy);

        // save live proxy with no option, use default
        proxy.setPropertyValue("dc:title", "B");
        proxy = session.saveDocument(proxy);
        assertTrue(proxy.isCheckedOut());
        assertVersion("0.0", proxy);
        assertVersionLabel("0.0", proxy);
        assertLatestVersion(null, proxy);

        // change live proxy and save with minor increment
        proxy.setPropertyValue("dc:title", "C");
        proxy.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
        proxy = session.saveDocument(proxy);
        assertFalse(proxy.isCheckedOut());
        assertVersion("0.1", proxy);
        assertVersionLabel("0.1", proxy);
        assertLatestVersion("0.1", proxy);

        // check the source document is also changed
        doc = session.getDocument(docRef);
        assertFalse(doc.isCheckedOut());
        assertVersion("0.1", doc);
        assertVersionLabel("0.1", doc);
        assertLatestVersion("0.1", doc);
        DocumentModel v01 = session.getLastDocumentVersion(docRef);
        assertEquals(v01.getId(), session.getBaseVersion(docRef).reference());

        // change with no increment, the proxy is checked out
        proxy.setPropertyValue("dc:title", "D");
        proxy = session.saveDocument(proxy);
        assertTrue(proxy.isCheckedOut());
        assertVersion("0.1", proxy);
        assertVersionLabel("0.1+", proxy);

        // check source doc
        doc = session.getDocument(docRef);
        assertEquals("D", doc.getPropertyValue("dc:title"));
        assertTrue(doc.isCheckedOut());
        assertVersion("0.1", doc);
        assertVersionLabel("0.1+", doc);
    }

    @Test
    public void testLiveProxyUpdate() {
        DocumentModel section = session.createDocumentModel("/", "section", "Folder");
        section = session.createDocument(section);
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        session.save();

        // create a live proxy
        DocumentModel proxy = session.createProxy(doc.getRef(), section.getRef());

        // --- change the doc, see changes on the proxy immediately without save ---

        // update
        doc.setPropertyValue("dc:title", "foo");
        doc = session.saveDocument(doc);
        assertEquals("foo", doc.getPropertyValue("dc:title"));
        // visible immediately on the proxy
        proxy.refresh();
        assertEquals("foo", proxy.getPropertyValue("dc:title"));

        // lifecycle change
        doc.followTransition("approve");
        assertEquals("approved", doc.getCurrentLifeCycleState());
        // visible immediately on the proxy
        proxy.refresh();
        assertEquals("approved", proxy.getCurrentLifeCycleState());

        // check in for version change
        doc.checkIn(VersioningOption.MINOR, null);
        assertFalse(doc.isCheckedOut());
        assertEquals("0.1", doc.getVersionLabel());
        // visible immediately on the proxy
        proxy.refresh();
        assertFalse(proxy.isCheckedOut());
        assertEquals("0.1", proxy.getVersionLabel());

        // check out
        doc.checkOut();
        assertTrue(doc.isCheckedOut());
        assertEquals("0.1+", doc.getVersionLabel());
        // visible immediately on the proxy
        proxy.refresh();
        assertTrue(proxy.isCheckedOut());
        assertEquals("0.1+", proxy.getVersionLabel());

        // --- change the proxy, see changes on the doc immediately without save ---

        // update
        proxy.setPropertyValue("dc:title", "bar");
        proxy = session.saveDocument(proxy);
        assertEquals("bar", proxy.getPropertyValue("dc:title"));
        // visible immediately on the doc, no save() needed
        doc.refresh();
        assertEquals("bar", doc.getPropertyValue("dc:title"));

        // lifecycle change
        proxy.followTransition("backToProject");
        assertEquals("project", proxy.getCurrentLifeCycleState());
        // visible immediately on the doc
        doc.refresh();
        assertEquals("project", doc.getCurrentLifeCycleState());

        // check in for version change
        proxy.checkIn(VersioningOption.MINOR, null);
        assertFalse(proxy.isCheckedOut());
        assertEquals("0.2", proxy.getVersionLabel());
        // visible immediately on the doc
        doc.refresh();
        assertFalse(doc.isCheckedOut());
        assertEquals("0.2", doc.getVersionLabel());

        // check out
        proxy.checkOut();
        assertTrue(proxy.isCheckedOut());
        assertEquals("0.2+", proxy.getVersionLabel());
        // visible immediately on the doc
        doc.refresh();
        assertTrue(doc.isCheckedOut());
        assertEquals("0.2+", doc.getVersionLabel());
    }

    /**
     * Tests that versioning a document having {@link StandardVersioningService#APPROVED_STATE} or
     * {@link StandardVersioningService#OBSOLETE_STATE} don't crash when it doesn't have
     * {@link StandardVersioningService#BACK_TO_PROJECT_TRANSITION}.
     */
    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:test-life-cycle-almost-default.xml")
    public void testDontBackToProjectTransition() {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        session.save();
        // method calling followTransition is during preSave step - checkin our document after putting it to approved
        doc.followTransition("to_approved");
        doc.checkIn(VersioningOption.MINOR, "minor version");

        // trigger versioning during a save
        doc.setPropertyValue("dc:description", "New description");
        doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MAJOR);
        doc = session.saveDocument(doc);
        session.save();

        assertEquals("approved", doc.getCurrentLifeCycleState());
        assertEquals("1.0", doc.getVersionLabel());
        assertFalse(doc.isCheckedOut());

        // check service still follow transition with right context
        doc = session.createDocumentModel("/", "note", "Note");
        doc = session.createDocument(doc);
        session.save();
        // method calling followTransition is during preSave step - checkin our document after putting it to approved
        doc.followTransition("approve");
        doc.checkIn(VersioningOption.MINOR, "minor version");

        // trigger versioning during a save
        doc.setPropertyValue("dc:description", "New description");
        doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MAJOR);
        doc = session.saveDocument(doc);
        session.save();

        assertEquals("project", doc.getCurrentLifeCycleState());
        assertEquals("1.0", doc.getVersionLabel());
        assertFalse(doc.isCheckedOut());
    }

}
