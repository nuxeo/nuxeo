/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.api.AssociatedDocuments;
import org.nuxeo.apidoc.documentation.DefaultDocumentationType;
import org.nuxeo.apidoc.documentation.ResourceDocumentationItem;
import org.nuxeo.apidoc.introspection.BundleGroupImpl;
import org.nuxeo.apidoc.introspection.BundleInfoImpl;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeSnaphotFeature.class })
public class TestSnapshotWithDocPersist {

    @Inject
    protected CoreSession session;

    @Inject
    protected SnapshotManager snapshotManager;

    @Inject
    protected TransactionalFeature tx;

    @Test
    public void testPersistWithLiveDoc() throws Exception {

        DistributionSnapshot runtimeSnapshot = snapshotManager.getRuntimeSnapshot();

        Map<String, ResourceDocumentationItem> liveDoc = new HashMap<>();
        BundleInfoImpl bi = (BundleInfoImpl) runtimeSnapshot.getBundle("org.nuxeo.ecm.core.api");

        ResourceDocumentationItem desc = new ResourceDocumentationItem("readMe.md", "<p>Hello this is core API</p>",
                DefaultDocumentationType.DESCRIPTION.toString(), bi);
        ResourceDocumentationItem ht = new ResourceDocumentationItem("HowTo.md", "This is simple",
                DefaultDocumentationType.HOW_TO.toString(), bi);

        liveDoc.put(DefaultDocumentationType.DESCRIPTION.toString(), desc);
        liveDoc.put(DefaultDocumentationType.HOW_TO.toString(), ht);

        bi.setLiveDoc(liveDoc);

        Map<String, ResourceDocumentationItem> liveDocP = new HashMap<>();
        ResourceDocumentationItem descP = new ResourceDocumentationItem("readMe.md", "<p>Hello this is core</p>",
                DefaultDocumentationType.DESCRIPTION.toString(), bi);
        ResourceDocumentationItem htP = new ResourceDocumentationItem("HowTo.md", "This is simple !",
                DefaultDocumentationType.HOW_TO.toString(), bi);

        liveDocP.put(DefaultDocumentationType.DESCRIPTION.toString(), descP);
        liveDocP.put(DefaultDocumentationType.HOW_TO.toString(), htP);

        bi.setParentLiveDoc(liveDocP);

        ((BundleGroupImpl) bi.getBundleGroup()).addLiveDoc(bi.getParentLiveDoc());

        DistributionSnapshot persistent = snapshotManager.persistRuntimeSnapshot(session);
        assertNotNull(persistent);

        DocumentModelList docs = session.query("select * from NXDocumentation");
        int nbDocs = docs.size();
        // number actually depends on the content of the jar
        // and from the deployment method ...
        // assertEquals(4, docs.size());

        // save an other time
        persistent = snapshotManager.persistRuntimeSnapshot(session);
        assertNotNull(persistent);

        docs = session.query("select * from NXDocumentation");
        assertEquals(nbDocs, docs.size());

        persistent = snapshotManager.getSnapshot(runtimeSnapshot.getKey(), session);
        assertNotNull(persistent);

        AssociatedDocuments docItems = persistent.getBundle("org.nuxeo.ecm.core.api").getAssociatedDocuments(session);
        assertNotNull(docItems);
        assertEquals(2, docItems.getDocumentationItems(session).size());
        assertEquals("<p>Hello this is core API</p>", docItems.getDescription(session).getContent().trim());

        docItems = persistent.getBundleGroup("grp:org.nuxeo.ecm.core").getAssociatedDocuments(session);
        assertNotNull(docItems);
        assertEquals(2, docItems.getDocumentationItems(session).size());
        assertEquals("<p>Hello this is core</p>", docItems.getDescription(session).getContent().trim());
    }

}
