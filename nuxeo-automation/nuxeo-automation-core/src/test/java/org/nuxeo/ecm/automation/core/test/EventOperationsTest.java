/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.events.EventHandler;
import org.nuxeo.ecm.automation.core.events.EventHandlerRegistry;
import org.nuxeo.ecm.automation.core.impl.adapters.StringToProperties;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy( { "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.platform.versioning" })
// For version label info
@LocalDeploy("org.nuxeo.ecm.automation.core:test-enc.xml")
// @RepositoryConfig(cleanup=Granularity.METHOD)
public class EventOperationsTest {

    protected DocumentModel src;

    protected DocumentModel dst;

    @Inject
    AutomationService service;

    @Inject
    EventHandlerRegistry reg;

    @Inject
    CoreSession session;

    @Before
    public void initRepo() throws Exception {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();

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

    // ------ Tests comes here --------

    /**
     * Create | Copy | Set Property This is also testing
     * {@link StringToProperties} adapter
     *
     * @throws Exception
     */
    @Test
    public void testCreateNoteWhenFolderCreated() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain("createNoteWhenFolderCreated");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "Note").set("name", "note").set(
                "properties", "dc:title=MyDoc");
        service.putOperationChain(chain);

        EventHandler handler = new EventHandler(
                DocumentEventTypes.DOCUMENT_CREATED,
                "createNoteWhenFolderCreated");
        Set<String> set = new HashSet<String>();
        set.add("Folder");
        handler.setDoctypes(set);
        reg.putEventHandler(handler);

        // now create a new folder inside src
        DocumentModel folder = session.createDocumentModel("/src", "myfolder",
                "Folder");
        folder.setPropertyValue("dc:title", "MyFolder");
        folder = session.createDocument(folder);
        session.save();
        folder = session.getDocument(folder.getRef());

        DocumentModel doc = session.getChild(folder.getRef(), "note");
        assertEquals("MyDoc", doc.getTitle());
        assertEquals("Note", doc.getType());
    }

    /**
     * Create | Copy | Set Property in a post commit listener This is also
     * testing {@link StringToProperties} adapter
     *
     * @throws Exception
     */
    @Test
    public void testCreateNoteWhenFolderCreatedInPostCommit() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        OperationChain chain = new OperationChain(
                "createNoteWhenFolderCreatedPc");
        chain.add(FetchContextDocument.ID);
        chain.add(CreateDocument.ID).set("type", "Note").set("name", "note_pc").set(
                "properties", "dc:title=MyDocPc");
        service.putOperationChain(chain);

        EventHandler handler = new EventHandler(
                DocumentEventTypes.DOCUMENT_CREATED,
                "createNoteWhenFolderCreatedPc");
        Set<String> set = new HashSet<String>();
        set.add("Folder");
        handler.setDoctypes(set);
        reg.putPostCommitEventHandler(handler);

        // now create a new folder inside src
        DocumentModel folder = session.createDocumentModel("/src", "myfolder",
                "Folder");
        folder.setPropertyValue("dc:title", "MyFolder");
        folder = session.createDocument(folder);
        session.save();
        folder = session.getDocument(folder.getRef());

        Framework.getLocalService(EventService.class).waitForAsyncCompletion();

        // reopen session since the modification occurred in another session in
        // another thread
        CoreSession session2 = Framework.getService(RepositoryManager.class).getDefaultRepository().open();

        DocumentModel doc = session2.getChild(folder.getRef(), "note_pc");
        assertEquals("MyDocPc", doc.getTitle());
        assertEquals("Note", doc.getType());
        CoreInstance.getInstance().close(session2);
    }

    @Test
    public void testXmlEncoding() {
        EventHandlerRegistry reg = Framework.getLocalService(EventHandlerRegistry.class);
        List<EventHandler> eh = reg.getEventHandlers("aboutToCreate");
        assertEquals("a < b & b > c", eh.get(0).getExpression());
    }

}
