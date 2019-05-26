/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.ui.web.restAPI;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.filemanager.api")
@Deploy("org.nuxeo.ecm.platform.filemanager.core")
@Deploy("org.nuxeo.ecm.platform.query.api")
public class TestCreationContainerListRestlet extends AbstractRestletTest {

    protected static final String ENDPOINT = "/creationContainerList";

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreSession session;

    protected String repositoryName;

    protected DocumentModel doc;

    @Before
    public void before() {
        repositoryName = session.getRepositoryName();
        doc = session.createDocumentModel("/", "work", "Workspace");
        doc.setPropertyValue("dc:title", "myworkspace");
        doc = session.createDocument(doc);
        session.save();
        txFeature.nextTransaction();
    }

    @Test
    public void testList() throws Exception {
        String path = ENDPOINT; // optional docType param is ignored by default CreationContainerListProvider
        String expected = XML //
                + "<containers>" //
                + "<document>" //
                + "<repository>" + repositoryName + "</repository>" //
                + "<docRef>" + doc.getId() + "</docRef>" //
                + "<docTitle>" + doc.getTitle() + "</docTitle>" //
                + "<docPath>" + doc.getPathAsString() + "</docPath>" //
                + "</document>" //
                + "</containers>";
        executeRequest(path, expected);
    }

}
