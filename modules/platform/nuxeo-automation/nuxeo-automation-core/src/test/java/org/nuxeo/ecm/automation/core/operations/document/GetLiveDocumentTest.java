/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benjamin JALON <bjalon@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.document;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class GetLiveDocumentTest {

    @Inject
    CoreSession session;

    private DocumentModel doc;

    private DocumentModel folder;

    private GetLiveDocument operation;

    @Before
    public void setup() throws Exception {
        folder = session.createDocumentModel("/", "test", "Folder");
        folder = session.createDocument(folder);
        session.save();

        doc = session.createDocumentModel("/", "my-doc", "File");
        doc.setPropertyValue("dc:title", "Test1");
        doc = session.createDocument(doc);
        session.save();

        operation = new GetLiveDocument();
        operation.session = session;
    }

    @Test
    public void shouldCreateProxyLiveFromVersion() {
        DocumentRef docRef = session.checkIn(doc.getRef(), VersioningOption.MINOR, "Test");
        assertTrue(session.exists(docRef));
        DocumentModel version = session.getDocument(docRef);

        DocumentModel result = operation.run(version);

        assertNotNull(result);
        assertEquals(doc.getPathAsString(), result.getPathAsString());
    }

    @Test
    public void shouldCreateProxyLiveFromProxy() {
        DocumentModel proxy = session.publishDocument(doc, folder, true);

        DocumentModel result = operation.run(proxy);

        assertNotNull(result);
        assertEquals(doc.getPathAsString(), result.getPathAsString());
        assertEquals(doc.getId(), result.getId());
    }

    @Test
    public void shouldCreateProxyLiveFromProxyAfterModification() {

        doc.setPropertyValue("dc:title", "Test2");
        doc = session.saveDocument(doc);
        session.save();

        DocumentModel proxy = session.publishDocument(doc, folder, true);

        DocumentModel result = operation.run(proxy);

        assertNotNull(result);
        assertEquals(doc.getPathAsString(), result.getPathAsString());
        assertEquals(doc.getId(), result.getId());
    }

}
