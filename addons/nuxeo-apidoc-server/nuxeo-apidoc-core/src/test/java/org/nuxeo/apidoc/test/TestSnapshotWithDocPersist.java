/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.apidoc.api.AssociatedDocuments;
import org.nuxeo.apidoc.documentation.DefaultDocumentationType;
import org.nuxeo.apidoc.documentation.ResourceDocumentationItem;
import org.nuxeo.apidoc.introspection.BundleGroupImpl;
import org.nuxeo.apidoc.introspection.BundleInfoImpl;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

public class TestSnapshotWithDocPersist extends SQLRepositoryTestCase {

    private static final Log log = LogFactory.getLog(TestSnapshotWithDocPersist.class);

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.event");

        deployBundle("org.nuxeo.ecm.directory.api");
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployBundle("org.nuxeo.ecm.platform.usermanager.api");
        deployBundle("org.nuxeo.ecm.platform.usermanager");

        deployBundle("org.nuxeo.ecm.automation.core");

        deployBundle("org.nuxeo.apidoc.core");
        openSession();

        // Force cleanup ... ???
        DocumentModelList docs = session.query("select * from NXDocumentation");
        if (docs.size() > 0) {
            for (DocumentModel doc : docs) {
                session.removeDocument(doc.getRef());
            }
        }
        session.save();

    }

    protected SnapshotManager getSnapshotManager() {
        return Framework.getLocalService(SnapshotManager.class);
    }

    @Test
    public void testPersistWithLiveDoc() throws Exception {

        DistributionSnapshot runtimeSnapshot = getSnapshotManager().getRuntimeSnapshot();

        Map<String, ResourceDocumentationItem> liveDoc = new HashMap<String, ResourceDocumentationItem>();
        BundleInfoImpl bi = (BundleInfoImpl) runtimeSnapshot.getBundle("org.nuxeo.ecm.core.api");

        ResourceDocumentationItem desc = new ResourceDocumentationItem(
                "readMe.md", "<p>Hello this is core API</p>", bi,
                DefaultDocumentationType.DESCRIPTION.toString());
        ResourceDocumentationItem ht = new ResourceDocumentationItem(
                "HowTo.md", "This is simple", bi,
                DefaultDocumentationType.HOW_TO.toString());

        liveDoc.put(DefaultDocumentationType.DESCRIPTION.toString(), desc);
        liveDoc.put(DefaultDocumentationType.HOW_TO.toString(), ht);

        bi.setLiveDoc(liveDoc);

        Map<String, ResourceDocumentationItem> liveDocP = new HashMap<String, ResourceDocumentationItem>();
        ResourceDocumentationItem descP = new ResourceDocumentationItem(
                "readMe.md", "<p>Hello this is core</p>", bi,
                DefaultDocumentationType.DESCRIPTION.toString());
        ResourceDocumentationItem htP = new ResourceDocumentationItem(
                "HowTo.md", "This is simple !", bi,
                DefaultDocumentationType.HOW_TO.toString());

        liveDocP.put(DefaultDocumentationType.DESCRIPTION.toString(), descP);
        liveDocP.put(DefaultDocumentationType.HOW_TO.toString(), htP);

        bi.setParentLiveDoc(liveDocP);

        ((BundleGroupImpl) bi.getBundleGroup()).addLiveDoc(bi.getParentLiveDoc());

        DistributionSnapshot persistent = getSnapshotManager().persistRuntimeSnapshot(
                session);
        assertNotNull(persistent);

        session.save();

        DocumentModelList docs = session.query("select * from NXDocumentation");
        int nbDocs = docs.size();
        // number actually depends on the content of the jar
        // and from the deployment method ...
        // assertEquals(4, docs.size());

        // save an other time
        persistent = getSnapshotManager().persistRuntimeSnapshot(session);
        assertNotNull(persistent);
        session.save();

        docs = session.query("select * from NXDocumentation");
        assertEquals(nbDocs, docs.size());

        persistent = getSnapshotManager().getSnapshot(runtimeSnapshot.getKey(),
                session);
        assertNotNull(persistent);

        AssociatedDocuments docItems = persistent.getBundle(
                "org.nuxeo.ecm.core.api").getAssociatedDocuments(session);
        assertNotNull(docItems);
        assertEquals(2, docItems.getDocumentationItems(session).size());
        assertEquals("<p>Hello this is core API</p>",
                docItems.getDescription(session).getContent().trim());

        docItems = persistent.getBundleGroup("grp:org.nuxeo.ecm.core").getAssociatedDocuments(
                session);
        assertNotNull(docItems);
        assertEquals(2, docItems.getDocumentationItems(session).size());
        assertEquals("<p>Hello this is core</p>",
                docItems.getDescription(session).getContent().trim());

        session.save();

    }

    @After
    public void tearDown() throws Exception {
        if (session != null) {
            CoreInstance.getInstance().close(session);
        }
        super.tearDown();
    }

}
