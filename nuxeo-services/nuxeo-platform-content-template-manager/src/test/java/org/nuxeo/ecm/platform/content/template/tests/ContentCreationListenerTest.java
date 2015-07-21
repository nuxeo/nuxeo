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
 */

package org.nuxeo.ecm.platform.content.template.tests;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Simple test class for ContentCreationListener
 *
 * @author JULIEN THIMONIER < jt@nuxeo.com >
 **/
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.content.template")
public class ContentCreationListenerTest {

    @Inject
    protected CoreSession session;

    @Test
    public void testContentCreationListener() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel model = session.createDocumentModel(root.getPathAsString(), "mondomaine", "Domain");
        DocumentModel doc = session.createDocument(model);
        session.saveDocument(doc);
        assert (doc != null);

        DocumentModelList modelList = session.getChildren(doc.getRef());

        // Check that 3 elements have been created on the new domain
        // (Section,Workspace and Templates)
        // This should be done by ContentCreationListener
        assert (modelList.size() == 3);
    }
}
