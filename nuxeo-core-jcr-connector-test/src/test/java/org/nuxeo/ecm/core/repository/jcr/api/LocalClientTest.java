/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.repository.jcr.testing.CoreJCRConnectorTestConstants;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class LocalClientTest extends RepositoryTestCase {

    private CoreSession client;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployContrib(CoreJCRConnectorTestConstants.TESTS_BUNDLE,
                "LifeCycleService.xml");
        deployContrib(CoreJCRConnectorTestConstants.BUNDLE,
                "CoreTestExtensions.xml");
        getRepository(); // force deploying repository

        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", SecurityConstants.ADMINISTRATOR);
        client = CoreInstance.getInstance().open("demo", ctx);
    }

    @Override
    protected void tearDown() throws Exception {
        client.cancel();

        CoreInstance.getInstance().close(client);
        super.tearDown();
    }

    public void testLocalClient() throws Exception {
        DocumentModel root = client.getRootDocument();
        DocumentModel dm = new DocumentModelImpl(root.getPathAsString(), "folder", "Folder");
        client.createDocument(dm);
        dm = new DocumentModelImpl(root.getPathAsString(), "file", "File");
        client.createDocument(dm);

        assertEquals(root.getRef().toString(), root.getRef().reference().toString());

        List<DocumentModel> children = client.getChildren(root.getRef());
        assertEquals(2, children.size());
        assertEquals("folder", children.get(0).getName());
        assertEquals("file", children.get(1).getName());

        for (DocumentModel child : children) {
            assertEquals(child.getRef().toString(),
                    child.getRef().reference().toString());
        }
    }

}
