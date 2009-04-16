/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.content.template.tests;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;

/**
*   Simple test class for ContentCreationListener
* @ JULIEN THIMONIER < jt@nuxeo.com >
**/
public class ContentCreationListenerTest extends RepositoryOSGITestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.content.template");
        openRepository();
    }

    public void testContentCreationListener() throws Exception {
        DocumentModel root = getCoreSession().getRootDocument();
        DocumentModel model = getCoreSession().createDocumentModel(
                root.getPathAsString(), "mondomaine", "Domain");
        DocumentModel doc = getCoreSession().createDocument(model);
        getCoreSession().saveDocument(doc);
        assert (doc != null);

        DocumentModelList modelList = getCoreSession().getChildren(doc.getRef());

        // Check that 3 elements have been created on the new domain
        // (Section,Workspace and Templates)
        // This should be done by ContentCreationListener
        assert (modelList.size() == 3);
    }
}
