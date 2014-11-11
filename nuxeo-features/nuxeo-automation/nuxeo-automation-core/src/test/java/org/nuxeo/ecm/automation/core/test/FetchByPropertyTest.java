/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;

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
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.platform.versioning" })
// For version label info
@LocalDeploy("org.nuxeo.ecm.automation.core:test-operations.xml")
// @RepositoryConfig(cleanup=Granularity.METHOD)
public class FetchByPropertyTest {

    protected DocumentModel doc1;

    protected DocumentModel doc2;

    protected DocumentModel doc3;

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Before
    public void initRepo() throws Exception {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();

        doc1 = session.createDocumentModel("/", "doc1", "Workspace");
        doc1.setPropertyValue("dc:title", "title1");
        doc1 = session.createDocument(doc1);
        session.save();
        doc1 = session.getDocument(doc1.getRef());

        doc2 = session.createDocumentModel("/", "doc2", "Workspace");
        doc2.setPropertyValue("dc:title", "title2");
        doc2 = session.createDocument(doc2);
        session.save();
        doc2 = session.getDocument(doc2.getRef());

        doc3 = session.createDocumentModel("/doc1", "doc3", "Folder");
        doc3.setPropertyValue("dc:title", "title3");
        doc3 = session.createDocument(doc3);
        session.save();
        doc3 = session.getDocument(doc3.getRef());

    }

    // ------ Tests comes here --------

    @Test
    public void testFetch() throws Exception {
        OperationContext ctx = new OperationContext(session);

        OperationChain chain = new OperationChain("testChain");
        chain.add(FetchByProperty.ID).set("property", "dc:title").set("values",
                "title1, title3");

        DocumentModelList docs = (DocumentModelList) service.run(ctx, chain);
        assertEquals(2, docs.size());
        ArrayList<String> titles = new ArrayList<String>();
        titles.add(docs.get(0).getTitle());
        titles.add(docs.get(1).getTitle());
        Collections.sort(titles);
        assertEquals("title1", titles.get(0));
        assertEquals("title3", titles.get(1));

        // add a where clause

        chain = new OperationChain("testChain");
        chain.add(FetchByProperty.ID).set("property", "dc:title").set("values",
                "title1, title3").set("query", "ecm:primaryType=\"Workspace\"");

        docs = (DocumentModelList) service.run(ctx, chain);
        assertEquals(1, docs.size());
        assertEquals("title1", titles.get(0));

    }

}
