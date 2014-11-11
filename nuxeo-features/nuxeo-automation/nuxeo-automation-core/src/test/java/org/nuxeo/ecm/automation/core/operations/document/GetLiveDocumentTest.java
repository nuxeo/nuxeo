/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Benjamin JALON <bjalon@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.document;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

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
    public void shouldCreateProxyLiveFromVersion() throws ClientException {
        DocumentRef docRef = session.checkIn(doc.getRef(),
                VersioningOption.MINOR, "Test");
        assertTrue(session.exists(docRef));
        DocumentModel version = session.getDocument(docRef);

        DocumentModel result = operation.run(version);

        assertNotNull(result);
        assertEquals(doc.getPathAsString(), result.getPathAsString());
    }

    @Test
    public void shouldCreateProxyLiveFromProxy() throws ClientException {
        DocumentModel proxy = session.publishDocument(doc, folder, true);

        DocumentModel result = operation.run(proxy);

        assertNotNull(result);
        assertEquals(doc.getPathAsString(), result.getPathAsString());
        assertEquals(doc.getId(), result.getId());
    }

    @Test
    public void shouldCreateProxyLiveFromProxyAfterModification()
            throws ClientException {

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
