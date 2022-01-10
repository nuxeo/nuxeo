/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.ecm.core.mongodb;

import static java.util.stream.Collectors.toList;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentExistsException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.mongodb.MongoDBFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ MongoDBFeature.class, CoreFeature.class })
@Deploy("org.nuxeo.ecm.core.mongodb.test:OSGI-INF/mongodb-disable-renaming-listener-test.xml")
@Deploy("org.nuxeo.ecm.core.mongodb.test:OSGI-INF/mongodb-enable-unique-indices-contrib.xml")
public class TestMongoDBIndices {

    protected static final String DOCUMENT_NAME = "anyFile";

    @Inject
    protected CoreSession coreSession;

    protected DocumentModel folder;

    @Before
    public void before() {
        folder = coreSession.createDocumentModel("/", "Folder", "Folder");
        folder = coreSession.createDocument(folder);
        coreSession.save();
        addPermissions(folder, "james");
        addPermissions(folder, "bob");
    }

    @Test
    public void shouldFailWhenCreatingExistingChildNameDocument() {
        try (CloseableCoreSession bobSession = CoreInstance.openCoreSession(coreSession.getRepositoryName(), "bob")) {
            createDocument(bobSession, folder, DOCUMENT_NAME);
        }

        try (CloseableCoreSession jamesSession = CoreInstance.openCoreSession(coreSession.getRepositoryName(),
                "james")) {
            createDocument(jamesSession, folder, DOCUMENT_NAME);
            fail("should throw a ConcurrentUpdateException");
        } catch (ConcurrentUpdateException cue) {
            assertEquals(SC_CONFLICT, cue.getStatusCode());
            String message = cue.getMessage();
            assertTrue(message, message.contains("E11000 duplicate key error collection"));
            assertTrue(message, message.contains(DOCUMENT_NAME));
            assertTrue(message, message.contains(folder.getId()));
        }
    }

    @Test
    public void shouldFailWhenMovingDocumentToExistingChildName() {
        try (CloseableCoreSession bobSession = CoreInstance.openCoreSession(coreSession.getRepositoryName(), "bob")) {
            createDocument(bobSession, folder, DOCUMENT_NAME);
        }

        try (CloseableCoreSession jamesSession = CoreInstance.openCoreSession(coreSession.getRepositoryName(),
                "james")) {
            DocumentModel documentModel = createDocument(jamesSession, folder, "jamesFileName");

            // DBSession.move will check the unicity and throw a DocumentExistsException before calling the backend
            jamesSession.move(documentModel.getRef(), null, DOCUMENT_NAME);
            fail("should throw a DocumentExistsException");
        } catch (DocumentExistsException dee) {
            assertEquals(String.format("Destination name already exists: %s", DOCUMENT_NAME), dee.getMessage());
        }
    }

    @Test
    public void shouldNotFailWhenCreatingVersions() {
        try (CloseableCoreSession bobSession = CoreInstance.openCoreSession(coreSession.getRepositoryName(), "bob")) {
            DocumentModel doc = createDocument(bobSession, folder, DOCUMENT_NAME);

            doc.checkIn(VersioningOption.MINOR, null);
            doc.checkOut();
            assertVersionLabels(bobSession, doc, "0.1");

            doc.checkIn(VersioningOption.MINOR, null);
            doc.checkOut();
            assertVersionLabels(bobSession, doc, "0.1", "0.2");
        }
    }

    protected DocumentModel createDocument(CoreSession session, DocumentModel parent, String fileName) {
        DocumentModel doc = session.createDocumentModel(parent.getPathAsString(), fileName, "File");
        session.createDocument(doc);
        session.save();
        return doc;
    }

    protected void addPermissions(DocumentModel documentModel, String userName) {
        ACPImpl acp = new ACPImpl();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE(userName, SecurityConstants.READ_WRITE, true));
        coreSession.setACP(documentModel.getRef(), acp, false);
    }

    protected void assertVersionLabels(CoreSession session, DocumentModel doc, String... labels) {
        List<String> versionLabels = session.getVersionsForDocument(doc.getRef())
                                            .stream()
                                            .map(VersionModel::getLabel)
                                            .collect(toList());
        assertEquals(Arrays.asList(labels), versionLabels);
    }
}
