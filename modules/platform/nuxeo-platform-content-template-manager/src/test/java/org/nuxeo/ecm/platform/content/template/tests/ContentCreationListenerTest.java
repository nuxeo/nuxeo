/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 * @author JULIEN THIMONIER
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
