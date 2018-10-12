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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @since 8.3
 */

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@Deploy("org.nuxeo.ecm.automation.core")
public class GetLastDocumentVersionTest {

    @Inject
    CoreSession session;

    @Inject
    AutomationService service;

    protected DocumentModel folder;

    protected DocumentModel section;

    protected DocumentModel doc;

    @Before
    public void initRepo() throws Exception {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();

        folder = session.createDocumentModel("/", "Folder", "Folder");
        folder.setPropertyValue("dc:title", "Folder");
        folder = session.createDocument(folder);
        session.save();
        folder = session.getDocument(folder.getRef());
    }

    @After
    public void cleanRepo() {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();
    }

    protected DocumentModel createDocumentVersions(DocumentModel doc, VersioningOption vo, int nrVersions) {
        for (int i = 1; i <= nrVersions; i++) {
            // make sure the version doesn't have the same "created" as the previous one
            try {
                Thread.sleep(2); // NOSONAR
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            doc.setPropertyValue("dc:description", String.valueOf(i));
            doc.putContextData(VersioningService.VERSIONING_OPTION, vo);
            doc = session.saveDocument(doc);
        }
        session.save();
        return doc;
    }

    @Test
    public void testGetLastMajorVersion() throws OperationException {
        doc = session.createDocumentModel("/Folder", "DocWithMajorVersions", "File");
        doc.setPropertyValue("dc:title", "DocWithMajorVersions");
        doc = session.createDocument(doc);
        session.save();
        doc = session.getDocument(doc.getRef());

        doc = createDocumentVersions(doc, VersioningOption.MAJOR, 3);

        DocumentModel lastVersion = runOperation(doc);
        assertNotNull(lastVersion);
        assertEquals("3", lastVersion.getPropertyValue("dc:description"));
        assertEquals("3.0", lastVersion.getVersionLabel());
    }

    @Test
    public void testGetLastMinorVersion() throws OperationException {
        doc = session.createDocumentModel("/Folder", "DocWithMinorVersions", "File");
        doc.setPropertyValue("dc:title", "DocWithMinorVersions");
        doc = session.createDocument(doc);
        session.save();
        doc = session.getDocument(doc.getRef());

        doc = createDocumentVersions(doc, VersioningOption.MINOR, 3);

        DocumentModel lastVersion = runOperation(doc);
        assertNotNull(lastVersion);
        assertEquals("3", lastVersion.getPropertyValue("dc:description"));
        assertEquals("0.3", lastVersion.getVersionLabel());
    }

    @Test
    public void testGetLastMinorMajorVersion() throws OperationException {
        doc = session.createDocumentModel("/Folder", "DocWithMajorMinorVersions", "File");
        doc.setPropertyValue("dc:title", "DocWithMajorMinorVersions");
        doc = session.createDocument(doc);
        session.save();
        doc = session.getDocument(doc.getRef());

        doc = createDocumentVersions(doc, VersioningOption.MAJOR, 3);
        doc = createDocumentVersions(doc, VersioningOption.MINOR, 3);

        DocumentModel lastVersion = runOperation(doc);
        assertNotNull(lastVersion);
        assertEquals("3", lastVersion.getPropertyValue("dc:description"));
        assertEquals("3.3", lastVersion.getVersionLabel());
    }

    @Test
    public void testNonexistentLastVersion() throws OperationException {
        doc = session.createDocumentModel("/Folder", "DocWithNoVersion", "File");
        doc.setPropertyValue("dc:title", "DocWithNoVersion");
        doc = session.createDocument(doc);
        session.save();
        doc = session.getDocument(doc.getRef());

        DocumentModel lastVersion = runOperation(doc);
        assertNull(lastVersion);
    }

    /**
     * Runs the operation for the different document models of the test.
     *
     * @param input
     * @return
     * @throws OperationException
     */
    protected DocumentModel runOperation(DocumentModel input) throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(input);
            OperationChain chain = new OperationChain("testGetLastVersion");
            chain.add(GetLastDocumentVersion.ID);
            return (DocumentModel) service.run(ctx, chain);
        }
    }

}
