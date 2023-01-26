/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.event;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_REMOVE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_REMOVE_VERSION;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_REMOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.PROXY_REMOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.VERSION_REMOVED;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 2023.0
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/disable-schedulers.xml")
public class TestEventRepositoryAPI {

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void testDocumentRemove() {
        DocumentModel doc = session.createDocumentModel("/", "file001", "File");
        doc = session.createDocument(doc);
        String docId = doc.getId();
        txFeature.nextTransaction();

        try (CapturingEventListener listener = new CapturingEventListener()) {
            session.removeDocument(doc.getRef());

            Event aboutToRemoveEvent = listener.findFirstCapturedEventOrElseThrow(ABOUT_TO_REMOVE);
            DocumentEventContext eventCtx = (DocumentEventContext) aboutToRemoveEvent.getContext();
            assertEquals("file001", eventCtx.getProperty("docTitle"));
            assertEquals(docId, eventCtx.getSourceDocument().getId());

            Event documentRemoved = listener.findFirstCapturedEventOrElseThrow(DOCUMENT_REMOVED);
            eventCtx = (DocumentEventContext) documentRemoved.getContext();
            assertEquals("file001", eventCtx.getProperty("docTitle"));
            assertEquals(docId, eventCtx.getSourceDocument().getId());
        }
    }

    @Test
    public void testVersionRemove() {
        DocumentModel doc = session.createDocumentModel("/", "file001", "File");
        doc = session.createDocument(doc);
        String docId = doc.getId();
        txFeature.nextTransaction();

        DocumentRef versionRef = doc.checkIn(VersioningOption.MINOR, "a comment");
        String versionId = versionRef.toString();
        // check out the document otherwise we can't delete the version
        doc.checkOut();
        txFeature.nextTransaction();

        try (CapturingEventListener listener = new CapturingEventListener()) {
            session.removeDocument(versionRef);

            Event aboutToRemoveVersionEvent = listener.findFirstCapturedEventOrElseThrow(ABOUT_TO_REMOVE_VERSION);
            DocumentEventContext eventCtx = (DocumentEventContext) aboutToRemoveVersionEvent.getContext();
            assertEquals("file001", eventCtx.getProperty("docTitle"));
            assertEquals(docId, eventCtx.getProperty("docSource"));
            assertEquals(versionId, eventCtx.getSourceDocument().getId());

            Event versionRemoved = listener.findFirstCapturedEventOrElseThrow(VERSION_REMOVED);
            eventCtx = (DocumentEventContext) versionRemoved.getContext();
            assertEquals("file001", eventCtx.getProperty("docTitle"));
            assertEquals(docId, eventCtx.getProperty("docSource"));
            assertEquals(docId, eventCtx.getSourceDocument().getId());

            Event documentRemoved = listener.findFirstCapturedEventOrElseThrow(DOCUMENT_REMOVED);
            eventCtx = (DocumentEventContext) documentRemoved.getContext();
            assertEquals("file001", eventCtx.getProperty("docTitle"));
            assertEquals(versionId, eventCtx.getSourceDocument().getId());
        }
    }

    // NXP-31575
    @Test
    public void testProxyRemove() {
        session.createDocument(session.createDocumentModel("/", "folder", "Folder"));
        txFeature.nextTransaction();

        DocumentModel doc = session.createDocumentModel("/", "file001", "File");
        doc = session.createDocument(doc);
        String docId = doc.getId();
        txFeature.nextTransaction();

        DocumentModel proxy = session.createProxy(doc.getRef(), new PathRef("/folder"));
        String proxyId = proxy.getId();
        txFeature.nextTransaction();

        try (CapturingEventListener listener = new CapturingEventListener()) {
            session.removeDocument(proxy.getRef());

            Event aboutToRemoveEvent = listener.findFirstCapturedEventOrElseThrow(ABOUT_TO_REMOVE);
            DocumentEventContext eventCtx = (DocumentEventContext) aboutToRemoveEvent.getContext();
            assertEquals("file001", eventCtx.getProperty("docTitle"));
            assertEquals(proxyId, eventCtx.getSourceDocument().getId());

            Event proxyRemoved = listener.findFirstCapturedEventOrElseThrow(PROXY_REMOVED);
            eventCtx = (DocumentEventContext) proxyRemoved.getContext();
            assertEquals("file001", eventCtx.getProperty("docTitle"));
            assertEquals(docId, eventCtx.getSourceDocument().getId());

            Event documentRemoved = listener.findFirstCapturedEventOrElseThrow(DOCUMENT_REMOVED);
            eventCtx = (DocumentEventContext) documentRemoved.getContext();
            assertEquals("file001", eventCtx.getProperty("docTitle"));
            assertEquals(proxyId, eventCtx.getSourceDocument().getId());
        }
    }

    // NXP-31575
    @Test
    public void testProxyOnVersionRemove() {
        session.createDocument(session.createDocumentModel("/", "folder", "Folder"));
        txFeature.nextTransaction();

        DocumentModel doc = session.createDocumentModel("/", "file001", "File");
        doc = session.createDocument(doc);
        String docId = doc.getId();
        txFeature.nextTransaction();

        DocumentRef versionRef = doc.checkIn(VersioningOption.MINOR, "a comment");
        txFeature.nextTransaction();

        DocumentModel proxy = session.createProxy(versionRef, new PathRef("/folder"));
        String proxyId = proxy.getId();
        txFeature.nextTransaction();

        try (CapturingEventListener listener = new CapturingEventListener()) {
            session.removeDocument(proxy.getRef());

            Event aboutToRemoveEvent = listener.findFirstCapturedEventOrElseThrow(ABOUT_TO_REMOVE);
            DocumentEventContext eventCtx = (DocumentEventContext) aboutToRemoveEvent.getContext();
            assertEquals("file001", eventCtx.getProperty("docTitle"));
            assertEquals(proxyId, eventCtx.getSourceDocument().getId());

            Event proxyRemoved = listener.findFirstCapturedEventOrElseThrow(PROXY_REMOVED);
            eventCtx = (DocumentEventContext) proxyRemoved.getContext();
            assertEquals("file001", eventCtx.getProperty("docTitle"));
            assertEquals(docId, eventCtx.getSourceDocument().getId());

            Event documentRemoved = listener.findFirstCapturedEventOrElseThrow(DOCUMENT_REMOVED);
            eventCtx = (DocumentEventContext) documentRemoved.getContext();
            assertEquals("file001", eventCtx.getProperty("docTitle"));
            assertEquals(proxyId, eventCtx.getSourceDocument().getId());
        }
    }
}
