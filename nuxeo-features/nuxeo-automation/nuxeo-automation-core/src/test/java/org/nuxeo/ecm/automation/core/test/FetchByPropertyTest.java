/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.document.FetchByProperty;
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
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
// For version label info
@Deploy("org.nuxeo.ecm.automation.core:test-operations.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class FetchByPropertyTest {

    protected DocumentModel doc1;

    protected DocumentModel doc2;

    protected DocumentModel doc3;

    protected DocumentModel doc4;

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Before
    public void initRepo() throws Exception {
        doc1 = session.createDocumentModel("/", "doc1", "Workspace");
        doc1.setPropertyValue("dc:title", "title1");
        doc1 = session.createDocument(doc1);
        doc1 = session.getDocument(doc1.getRef());

        doc2 = session.createDocumentModel("/", "doc2", "Workspace");
        doc2.setPropertyValue("dc:title", "title2");
        doc2 = session.createDocument(doc2);
        doc2 = session.getDocument(doc2.getRef());

        doc3 = session.createDocumentModel("/doc1", "doc3", "Folder");
        doc3.setPropertyValue("dc:title", "title3");
        doc3 = session.createDocument(doc3);
        doc3 = session.getDocument(doc3.getRef());

        doc4 = session.createDocumentModel("/doc1", "doc4", "Folder");
        doc4.setPropertyValue("dc:title", "\"sinead o'connor\"");
        doc4 = session.createDocument(doc4);
        doc4 = session.getDocument(doc4.getRef());

        session.save();
    }

    // ------ Tests comes here --------

    @Test
    public void testFetchOne() throws Exception {
        OperationContext ctx = new OperationContext(session);
        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchByProperty.ID).set("property", "dc:title").set("values", "title1");

        DocumentModelList docs = (DocumentModelList) service.run(ctx, chain);
        assertEquals(1, docs.size());
        assertEquals("doc1", docs.get(0).getName());

        // add a where clause

        chain = new OperationChain("testChain");
        chain.add(FetchByProperty.ID).set("property", "dc:title").set("values", "title1").set("query",
                "ecm:primaryType = 'Workspace'");

        docs = (DocumentModelList) service.run(ctx, chain);
        assertEquals(1, docs.size());
        assertEquals("doc1", docs.get(0).getName());
    }

    @Test
    public void testFetchMultiple() throws Exception {
        OperationContext ctx = new OperationContext(session);
        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchByProperty.ID).set("property", "dc:title").set("values", "title1, title3, title3"); // twice
                                                                                                           // title3

        DocumentModelList docs = (DocumentModelList) service.run(ctx, chain);
        assertEquals(2, docs.size());
        List<String> titles = new ArrayList<String>();
        titles.add(docs.get(0).getTitle());
        titles.add(docs.get(1).getTitle());
        Collections.sort(titles);
        assertEquals("title1", titles.get(0));
        assertEquals("title3", titles.get(1)); // only once in result

        // add a where clause

        chain = new OperationChain("testChain");
        chain.add(FetchByProperty.ID).set("property", "dc:title").set("values", "title1, title3").set("query",
                "ecm:primaryType = 'Workspace'");

        docs = (DocumentModelList) service.run(ctx, chain);
        assertEquals(1, docs.size());
        assertEquals("title1", titles.get(0));
    }

    @Test
    public void testEscaping() throws Exception {
        OperationContext ctx = new OperationContext(session);
        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchByProperty.ID).set("property", "dc:title").set("values", "\"sinead o'connor\"");

        DocumentModelList docs = (DocumentModelList) service.run(ctx, chain);
        assertEquals(1, docs.size());
        assertEquals("doc4", docs.get(0).getName());
    }

}
