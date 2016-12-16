/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.9.3
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.userworkspace.core", "org.nuxeo.ecm.platform.collections.core",
        "org.nuxeo.ecm.platform.userworkspace.types", "org.nuxeo.ecm.platform.query.api",
        "org.nuxeo.ecm.platform.web.common" })
public class CollectionRightsTest {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    CollectionManager collectionManager;

    @Inject
    CoreSession session;

    protected CoreSession userSession;

    @After
    public void tearDown() throws Exception {
        if (userSession != null) {
            userSession.close();
        }
    }

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

        DocumentRef docRef = testFile.getRef();

        String repositoryName = coreFeature.getStorageConfiguration().getRepositoryName();
        userSession = CoreInstance.openCoreSession(repositoryName, "user1");

        testFile = userSession.getDocument(docRef);

        collectionManager.addToNewCollection("Collection1", "blablabla", testFile, userSession);

        DataModel dm = testFile.getDataModel("dublincore");

        String[] contributorsArray = (String[]) dm.getData("contributors");

        assertNotNull(contributorsArray);

        assertEquals(1, contributorsArray.length);

        assertFalse(contributorsArray[0].equals("user1"));

    }

}
