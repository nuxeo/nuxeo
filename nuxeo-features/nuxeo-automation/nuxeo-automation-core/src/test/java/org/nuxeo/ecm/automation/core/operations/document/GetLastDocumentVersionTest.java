/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Ricardo Dias
 */

package org.nuxeo.ecm.automation.core.operations.document;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.*;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @author rdias
 * @since 8.3
 */

@RunWith(FeaturesRunner.class)
@Features({CoreFeature.class})
@Deploy("org.nuxeo.ecm.automation.core")
public class GetLastDocumentVersionTest {

    @Inject
    CoreSession session;

    @Inject
    AutomationService service;

    protected DocumentModel folder;

    protected DocumentModel section;

    protected DocumentModel docWithMajorVersions;

    protected DocumentModel docWithMinorVersions;

    protected DocumentModel docWithMajorMinorVersions;

    protected DocumentModel docWithNoVersion;

    @Before
    public void initRepo() throws Exception {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();

        folder = session.createDocumentModel("/", "Folder", "Folder");
        folder.setPropertyValue("dc:title", "Folder");
        folder = session.createDocument(folder);
        session.save();
        folder = session.getDocument(folder.getRef());

        // major versions
        docWithMajorVersions = session.createDocumentModel("/Folder", "DocWithVersions", "File");
        docWithMajorVersions.setPropertyValue("dc:title", "DocWithVersions");
        docWithMajorVersions = session.createDocument(docWithMajorVersions);
        session.save();
        docWithMajorVersions = session.getDocument(docWithMajorVersions.getRef());

        // minor versions
        docWithMinorVersions = session.createDocumentModel("/Folder", "DocWithVersions", "File");
        docWithMinorVersions.setPropertyValue("dc:title", "DocWithVersions");
        docWithMinorVersions = session.createDocument(docWithMinorVersions);
        session.save();
        docWithMinorVersions = session.getDocument(docWithMinorVersions.getRef());

        // major and minor versions
        docWithMajorMinorVersions = session.createDocumentModel("/Folder", "DocWithMoreVersions", "File");
        docWithMajorMinorVersions.setPropertyValue("dc:title", "DocWithMoreVersions");
        docWithMajorMinorVersions = session.createDocument(docWithMajorMinorVersions);
        session.save();
        docWithMajorMinorVersions = session.getDocument(docWithMajorMinorVersions.getRef());

        docWithNoVersion = session.createDocumentModel("/Folder", "docWithNoVersion", "File");
        docWithNoVersion.setPropertyValue("dc:title", "docWithNoVersion");
        docWithNoVersion = session.createDocument(docWithNoVersion);
        session.save();
        docWithNoVersion = session.getDocument(docWithNoVersion.getRef());

    }

    @After
    public void cleanRepo() {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();
    }

    protected void createDocumentVersions(DocumentModel doc, VersioningOption vo, int nrVersions) {
        // create a document with major versions
        for (int i = 1; i <= nrVersions; i++) {
            doc.setPropertyValue("dc:description", "" + i);
            doc.putContextData(VersioningService.VERSIONING_OPTION, vo);
            session.saveDocument(doc);
        }
        session.save();
    }

    @Test
    public void testGetLastMajorVersion() throws OperationException {

        createDocumentVersions(docWithMajorVersions, VersioningOption.MAJOR, 3);

        DocumentModel lastVersion = runOperation(docWithMajorVersions);
        assertNotNull(lastVersion);
        assertEquals("3", lastVersion.getPropertyValue("dc:description"));
        assertEquals("3.0", lastVersion.getVersionLabel());

    }

    @Test
    public void testGetLastMinorVersion() throws OperationException {

        createDocumentVersions(docWithMinorVersions, VersioningOption.MINOR, 3);

        DocumentModel lastVersion = runOperation(docWithMinorVersions);
        assertNotNull(lastVersion);
        assertEquals("3", lastVersion.getPropertyValue("dc:description"));
        assertEquals("0.3", lastVersion.getVersionLabel());

    }

    @Test
    public void testGetLastMinorMajorVersion() throws OperationException {

        createDocumentVersions(docWithMajorMinorVersions, VersioningOption.MAJOR, 3);
        createDocumentVersions(docWithMajorMinorVersions, VersioningOption.MINOR, 3);

        DocumentModel lastVersion = runOperation(docWithMajorMinorVersions);
        assertNotNull(lastVersion);
        assertEquals("3", lastVersion.getPropertyValue("dc:description"));
        assertEquals("3.3", lastVersion.getVersionLabel());
    }

    @Test
    public void testNonexistentLastVersion() throws OperationException {

        DocumentModel lastVersion = runOperation(docWithNoVersion);
        assertNull(lastVersion);
    }

    /**
     * Runs the operation for the different document models of the test.
     * @param input
     * @return
     * @throws OperationException
     */
    protected DocumentModel runOperation(DocumentModel input) throws OperationException {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(input);
        OperationChain chain = new OperationChain("testGetLastVersion");
        chain.add(GetLastDocumentVersion.ID);
        return (DocumentModel) service.run(ctx, chain);
    }

}
