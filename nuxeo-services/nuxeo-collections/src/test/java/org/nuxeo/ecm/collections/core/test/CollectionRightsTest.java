/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.9.3
 */
@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CollectionFeature.class })
public class CollectionRightsTest {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    CollectionManager collectionManager;

    @Inject
    CoreSession session;

    @Test
    public void testDocumentNotAlteredAfterAddedToCollection() {
        DocumentModel testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace",
                "Workspace");
        testWorkspace = session.createDocument(testWorkspace);

        ACE ace = new ACE("Everyone", "Read", true);
        ACL acl = new ACLImpl();
        acl.add(ace);
        ACP acp = new ACPImpl();
        acp.addACL(acl);

        testWorkspace.setACP(acp, true);
        testWorkspace = session.saveDocument(testWorkspace);

        DocumentModel testFile = session.createDocumentModel(testWorkspace.getPath().toString(), "File1", "File");

        testFile = session.createDocument(testFile);

        session.save();

        ace = new ACE("user1", "Everything", true);
        acl = new ACLImpl();
        acl.add(ace);
        acp = new ACPImpl();
        acp.addACL(acl);
        DocumentModel root = session.getDocument(new PathRef("/"));
        root.setACP(acp, true);
        session.saveDocument(root);

        DocumentRef docRef = testFile.getRef();

        String repositoryName = coreFeature.getStorageConfiguration().getRepositoryName();
        try (CloseableCoreSession userSession = CoreInstance.openCoreSession(repositoryName, "user1")) {
            testFile = userSession.getDocument(docRef);

            collectionManager.addToNewCollection("Collection1", "blablabla", testFile, userSession);

            DataModel dm = testFile.getDataModel("dublincore");

            String[] contributorsArray = (String[]) dm.getData("contributors");

            assertNotNull(contributorsArray);

            assertEquals(1, contributorsArray.length);

            assertFalse(contributorsArray[0].equals("user1"));
        }

    }

}
