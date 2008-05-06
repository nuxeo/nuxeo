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

package org.nuxeo.ecm.core.repository.jcr.schema;

import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.testing.CoreJCRConnectorTestConstants;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;

/**
 * Tests schemas and doc types in the case of
 * automatic registration (via XSD files) of types.
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestFacets extends RepositoryTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployContrib(CoreJCRConnectorTestConstants.BUNDLE,
                "CoreTestExtensions.xml");
    }

    public void testFolderishFacet() throws Exception {
        Session session = getRepository().getSession(null);
        Document root = session.getRootDocument();

        assertTrue(root.isFolder());

        Document doc = root.addChild("doc1", "MyDocType");
        assertFalse(doc.isFolder());

        doc = root.addChild("folder1", "Folder");
        assertTrue(doc.isFolder());

        doc = root.addChild("ws", "Workspace");
        assertTrue(doc.isFolder());

        doc = root.addChild("file1", "File");
        assertFalse(doc.isFolder());
    }

}
