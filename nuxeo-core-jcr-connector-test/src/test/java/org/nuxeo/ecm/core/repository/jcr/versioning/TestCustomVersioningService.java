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

package org.nuxeo.ecm.core.repository.jcr.versioning;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.repository.jcr.testing.CoreJCRConnectorTestConstants;
import org.nuxeo.ecm.core.versioning.DocumentVersion;
import org.nuxeo.ecm.core.versioning.DocumentVersionIterator;

/**
 * @author <a hef="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 *
 */
public class TestCustomVersioningService extends TestVersioning {

    private static final Log log = LogFactory.getLog(TestCustomVersioningService.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib(CoreJCRConnectorTestConstants.BUNDLE,
                "CustomVersioningService.xml");
    }

    public void testRetrieveLastVersion() throws Exception {
        prepareTest("child_from_testIsCheckOut", "parent_from_testIsCheckOut");

        session.save();

        doc.checkIn("1stcheckIn");
        doc.checkOut();

        doc.setString("title", "2ndValue");
        doc.save();
        doc.checkIn("2ndcheckIn");
        doc.checkOut();

        DocumentVersion lastVer = doc.getLastVersion();
        assertNotNull(lastVer);
        assertEquals("2ndValue", doc.getString("title"));

        doc.setString("title", "3rdValue");
        doc.save();
        doc.checkIn("3rdCheckIn");
        doc.checkOut();

        DocumentVersion lastVer2 = doc.getLastVersion();
        assertNotNull(lastVer2);
        assertEquals("3rdValue", doc.getString("title"));
    }

    public void testRetrievePredecessors() throws Exception {
        prepareTest("child_from_testIsCheckOut", "parent_from_testIsCheckOut");

        session.save();

        doc.setString("title", "1stValue");
        doc.save();
        doc.checkIn("1stcheckIn");
        doc.checkOut();

        doc.setString("title", "2ndValue");
        doc.save();
        doc.checkIn("2ndCheckIn");
        doc.checkOut();

        doc.setString("title", "3rdValue");
        doc.save();
        doc.checkIn("3rdCheckIn");
        doc.checkOut();

        DocumentVersion lastVersion = (DocumentVersion) doc.getVersion("3rdCheckIn");
        assertNotNull(lastVersion);
        DocumentVersion[] predecessors = lastVersion.getPredecessors();
        assertEquals(2, predecessors.length);

        assertEquals("2ndValue", predecessors[0].getString("title"));
        assertEquals("1stValue", predecessors[1].getString("title"));
    }

    public void testRetrieveSuccessors() throws Exception {
        prepareTest("child_from_testIsCheckOut", "parent_from_testIsCheckOut");

        session.save();

        doc.setString("title", "1stValue");
        doc.save();
        doc.checkIn("1stcheckIn");
        doc.checkOut();

        doc.setString("title", "2ndValue");
        doc.save();
        doc.checkIn("2ndCheckIn");
        doc.checkOut();

        doc.setString("title", "3rdValue");
        doc.save();
        doc.checkIn("3rdCheckIn");
        doc.checkOut();

        DocumentVersion lastVersion = (DocumentVersion) doc.getVersion("1stcheckIn");
        DocumentVersion[] successors = lastVersion.getSuccessors();
        assertEquals(2, successors.length);

        assertEquals("2ndValue", successors[0].getString("title"));
        assertEquals("3rdValue", successors[1].getString("title"));
    }

    public void testPath() throws Exception {
        prepareTest("child_from_testIsCheckOut", "parent_from_testIsCheckOut");

        session.save();

        Document parent = doc.getParent();

        parent.setPropertyValue("title", "parent_title");

        doc.checkIn("1stcheckIn");
        doc.checkOut();

        Document ver = doc.getVersion("1stcheckIn");

        final String verPath = ver.getPath();

        // XXX: what path should be here?
        // kind of /versionStorage/5e/ca/de/5ecade1a-e764-493f-a4d6-aac3468b944b/version1/5ecade1a-e764-493f-a4d6-aac3468b944b
        // or normal /ecm:root//......
        // System.err.println(verPath);
    }

    public void testRemoveDocument() throws Exception {
        prepareTest("child_from_testIsCheckOut", "parent_from_testIsCheckOut");

        session.save();

        Document parent = doc.getParent();

        parent.setPropertyValue("title", "parent_title");

        doc.checkIn("1stcheckIn");
        doc.checkOut();

        Document ver = doc.getVersion("1stcheckIn");

        assertNotNull(ver);

        String verUuid = ver.getUUID();

        doc.remove();

        session.save();

        Document version = session.getDocumentByUUID(verUuid);
        // version not removed as version removal is done in AbstractSession
        assertNotNull(version);
    }

    /**
     * Use case based on
     * <a href="http://jira.nuxeo.org/browse/NXP-754">NXP-754</a>.
     *
     * Check for version documents having "ecm:frozenNode" property.
     *
     * @throws Exception
     */
    public void testUseCaseNXP754() throws Exception {
        prepareTest("child_from_testIsCheckOut", "parent_from_testIsCheckOut");

        session.save();

        Document workspaceDocument = session.getRootDocument();

        Document doc = workspaceDocument.addChild("1", "File");
        session.save();
        doc.checkIn("1.0");
        session.save();

        Document doc2 = workspaceDocument.addChild("2", "File");
        session.save();
        doc2.checkIn("1.0");
        session.save();

        doc.checkOut();
        Document sDoc = doc.getVersion("1.0").getSourceDocument();
        assertNotNull(sDoc);
        assertEquals(doc.getUUID(), sDoc.getUUID());

        doc2.checkOut();
        Document sDoc2 = doc2.getVersion("1.0").getSourceDocument();
        assertNotNull(sDoc2);
        assertEquals(doc2.getUUID(), sDoc2.getUUID());
    }

    /**
     * Use case based on
     * <a href="http://jira.nuxeo.org/browse/NXP-1522">NXP-1522:
     * Can't restore some node structures with custom versioning</a>.
     *
     * Check for version documents having "ecm:frozenNode" property.
     *
     * @throws Exception
     */
    public void testUseCaseNXP1522() throws Exception {
        prepareTest("child_from_testIsCheckOut", "parent_from_testIsCheckOut");

        session.save();

        Document workspaceDocument = session.getRootDocument();

        Document doc = workspaceDocument.addChild("1", "VFolder");
        session.save();

        Document doc2 = doc.addChild("2", "File");
        session.save();

        doc.checkIn("1.0");
        session.save();

        Document doc3 = doc.addChild("3", "File");
        session.save();

        doc.checkOut();
        Document sDoc = doc.getVersion("1.0").getSourceDocument();
        assertNotNull(sDoc);
        assertEquals(doc.getUUID(), sDoc.getUUID());

        displayVersions(doc);
        displayVersions(doc2);
        displayVersions(doc3);
    }

    private static void displayVersions(Document doc) throws DocumentException {
        log.info("---------doc versions---------- for: " + doc.getPath());
        DocumentVersionIterator it = doc.getVersions();
        while (it.hasNext()) {
            DocumentVersion docVer = it.next();
            log.info(docVer.getLabel());
        }
        log.info("-------------------------------");
    }

}
