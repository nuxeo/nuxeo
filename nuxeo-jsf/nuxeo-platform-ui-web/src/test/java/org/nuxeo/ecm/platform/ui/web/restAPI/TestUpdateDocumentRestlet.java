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

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestUpdateDocumentRestlet extends AbstractRestletTest {

    protected static final String ENDPOINT = "/update";

    protected static final String ENDPOINT2 = "/updateDocumentRestlet";

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreSession session;

    protected String repositoryName;

    protected DocumentModel doc;

    @Before
    public void before() {
        repositoryName = session.getRepositoryName();
        doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        session.save();
        txFeature.nextTransaction();
    }

    @Test
    public void testUpdate() throws Exception {
        dotTestUpdate(ENDPOINT);
    }

    @Test
    public void testUpdateEndpoint2() throws Exception {
        dotTestUpdate(ENDPOINT2);
    }

    protected void dotTestUpdate(String endpoint) throws Exception {
        String path = "/" + repositoryName + "/" + doc.getId() + endpoint + "?dc:title=mytitle&dc:description=mydescr";
        String expected = XML //
                + "<document><docRef>Document " + doc.getId() + " has been updated</docRef></document>";
        executeRequest(path, expected);

        // check doc has been updated
        txFeature.nextTransaction();
        assertEquals("mytitle", doc.getTitle());
        assertEquals("mydescr", doc.getPropertyValue("dc:description"));
    }

}
