package org.nuxeo.ecm.core.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.CleanupLevel;
import org.nuxeo.ecm.core.test.annotations.RepositoryFactory;

import com.google.inject.Inject;


@RunWith(NuxeoCoreRunner.class)
@CleanupLevel(Level.METHOD)
@RepositoryFactory(DefaultRepoFactory.class)
public class CleanUpWithFactoryTest {
    @Inject CoreSession session;

    @Test
    public void iCreateADoc() throws Exception {
        DocumentModel doc = session.createDocumentModel("/default-domain/workspaces/", "myWorkspace", "Workspace");
        doc.setProperty("dublincore", "title", "My Workspace");
        doc = session.createDocument(doc);
        session.saveDocument(doc);
        session.save();
        assertTrue(session.exists(new PathRef("/default-domain/workspaces/myWorkspace")));
    }

    @Test
    public void myWorkspaceIsNotHereAnymore() throws Exception {
        assertTrue(session.exists(new PathRef("/default-domain/workspaces/")));
        assertFalse(session.exists(new PathRef("/default-domain/workspaces/myWorkspace")));
    }
}
