/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.core.events.EventHandler;
import org.nuxeo.ecm.automation.core.events.EventHandlerRegistry;
import org.nuxeo.ecm.automation.core.impl.adapters.StringToProperties;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core" })
@Deploy("org.nuxeo.ecm.automation.core:test-events.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class EventOperationsTest {

    protected DocumentModel src;

    protected DocumentModel dst;

    @Inject
    CoreSession session;

    @Inject
    RuntimeHarness harness;

    @Inject
    EventHandlerRegistry registry;

    @Inject
    protected EventService eventService;

    @Before
    public void initRepo() throws Exception {
        src = session.createDocumentModel("/", "src", "Workspace");
        src.setPropertyValue("dc:title", "Source");
        src = session.createDocument(src);
        session.save();
        src = session.getDocument(src.getRef());

        dst = session.createDocumentModel("/", "dst", "Workspace");
        dst.setPropertyValue("dc:title", "Destination");
        dst = session.createDocument(dst);
        session.save();
        dst = session.getDocument(dst.getRef());
    }

    protected void nextTransaction() {
        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
        TransactionHelper.startTransaction();
    }

    // ------ Tests comes here --------

    /**
     * Create | Copy | Set Property This is also testing {@link StringToProperties} adapter
     *
     * @throws Exception
     */
    @Test
    public void testCreateNoteWhenFolderCreated() throws Exception {
        // now create a new folder inside src
        DocumentModel folder = session.createDocumentModel("/src", "myfolder", "Folder");
        folder.setPropertyValue("dc:title", "MyFolder");
        folder = session.createDocument(folder);
        session.save();
        folder = session.getDocument(folder.getRef());

        DocumentModel doc = session.getChild(folder.getRef(), "note");
        assertEquals("MyDoc", doc.getTitle());
        assertEquals("Note", doc.getType());
    }

    /**
     * Create | Copy | Set Property in a post commit listener This is also testing {@link StringToProperties} adapter
     *
     * @throws Exception
     */
    @Test
    public void testCreateNoteWhenFolderCreatedInPostCommit() throws Exception {
        // now create a new folder inside src
        DocumentModel folder = session.createDocumentModel("/src", "myfolder", "Folder");
        folder.setPropertyValue("dc:title", "MyFolder");
        folder = session.createDocument(folder);
        session.save();
        folder = session.getDocument(folder.getRef());

        nextTransaction();

        // reopen session since the modification occurred in another session in
        // another thread
        try (CloseableCoreSession session2 = CoreInstance.openCoreSession(null)) {
            DocumentModel doc = session2.getChild(folder.getRef(), "note_pc");
            assertEquals("MyDocPc", doc.getTitle());
            assertEquals("Note", doc.getType());
        }
    }

    @Test
    public void testCreateNoteWhenExpressionNOK() throws Exception {
        DocumentModel file = session.createDocumentModel("/src", "myfile", "File");
        file.setPropertyValue("dc:title", "MyFile");
        assertEquals("MyFile", file.getPropertyValue("dc:title"));
        file = session.createDocument(file);
        assertEquals("Modified with false expression", file.getPropertyValue("dc:title"));
    }

    @Test
    public void testCreateNoteWhenConditionOK() throws Exception {
        DocumentModel folder = session.createDocumentModel("/src", "myws", "Workspace");
        folder.setPropertyValue("dc:title", "My workspace");
        assertEquals("My workspace", folder.getPropertyValue("dc:title"));
        folder = session.createDocument(folder);
        assertEquals("Modified with true condition", folder.getPropertyValue("dc:title"));
        session.save();

    }

    @Test
    public void testShallowFiltering() {
        DocumentModel doc = session.createDocumentModel("/src", "myfile", "File");
        doc.setPropertyValue("dc:description", "ChangeMySource");
        doc = session.createDocument(doc);
        session.save();

        nextTransaction();

        doc = session.getDocument(doc.getRef());
        assertEquals("New source", doc.getPropertyValue("dc:source"));
    }

    @Test
    public void testDynamicHandlerRegistring() {
        EventHandler handler = new EventHandler("documentCreated", "changeSource");
        handler.setCondition("Document.getProperty(\"dc:description\") == \"/src/myfile\"");
        registry.putEventHandler(handler);
        DocumentModel doc = session.createDocumentModel("/src", "myfile", "File");
        doc.setPropertyValue("dc:description", doc.getPathAsString());
        doc = session.createDocument(doc);
        session.save();

        nextTransaction();

        doc = session.getDocument(doc.getRef());
        assertEquals("New source", doc.getPropertyValue("dc:source"));
    }

}
