/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Ricardo Dias
 *     Thibaud Arguillere
 */
package org.nuxeo.ecm.automation.core.operations.document;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @since 8.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.core:OSGI-INF/add-facet-test-contrib.xml")
public class FacetOperationsTest {

    @Inject
    CoreSession session;

    @Inject
    AutomationService service;

    // MyNewFacet is declared in OSGI-INF/add-facet-test-contrib.xml
    public static final String THE_FACET = "MyNewFacet";

    protected DocumentModel folder;

    protected DocumentModel docNoFacet;

    protected DocumentModel docWithFacet;

    @Before
    public void initRepo() throws Exception {

        folder = session.createDocumentModel("/", "Folder", "Folder");
        folder.setPropertyValue("dc:title", "Folder");
        folder = session.createDocument(folder);
        session.save();

        docNoFacet = session.createDocumentModel("/Folder", "DocNoFacet", "File");
        docNoFacet.setPropertyValue("dc:title", "DocNotFacet");
        docNoFacet = session.createDocument(docNoFacet);
        session.save();
        docNoFacet = session.getDocument(docNoFacet.getRef());

        docWithFacet = session.createDocumentModel("/Folder", "DocWithFacet", "File");
        docWithFacet.setPropertyValue("dc:title", "DocWithFacet");
        docWithFacet = session.createDocument(docWithFacet);
        session.save();
        docWithFacet = session.getDocument(docWithFacet.getRef());

        docWithFacet.addFacet(THE_FACET);
        docWithFacet = session.saveDocument(docWithFacet);
        session.save();

    }

    @After
    public void clearRepo() {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();
    }

    @Test
    public void testAddFacet() throws OperationException {

        assertNotNull(docNoFacet);
        assertFalse("New doc should not have the facet.", docNoFacet.hasFacet(THE_FACET));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(docNoFacet);
        OperationChain chain = new OperationChain("testAddFacet");
        chain.add(AddFacet.ID).set("facet", THE_FACET);
        DocumentModel resultDoc = (DocumentModel)service.run(ctx, chain);

        assertNotNull(resultDoc);
        assertTrue("The doc should now have the facet.", resultDoc.hasFacet(THE_FACET));

    }

    @Test(expected=OperationException.class)
    public void testAddUnknownFacet() throws OperationException {

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(docNoFacet);
        OperationChain chain = new OperationChain("testAddFacet");
        chain.add(AddFacet.ID).set("facet", "UnknownFacet");

        // the facet does not exist
        DocumentModel resultDoc = (DocumentModel) service.run(ctx, chain);
        assertNull(resultDoc);
    }

    @Test
    public void testRemoveFacet() throws OperationException {

        //remove from a document with facet
        assertNotNull(docWithFacet);
        assertTrue("New doc should have the facet.", docWithFacet.hasFacet(THE_FACET));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(docWithFacet);
        OperationChain chain = new OperationChain("testRemoveFacet");
        chain.add(RemoveFacet.ID).set("facet", THE_FACET);
        DocumentModel resultDoc = (DocumentModel)service.run(ctx, chain);

        assertNotNull(resultDoc);
        assertFalse("The doc should not have the facet.", resultDoc.hasFacet(THE_FACET));

    }

    @Test
    public void testRemoveUnknownFacet() throws OperationException {

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(docNoFacet);
        OperationChain chain = new OperationChain("testRemoveUnknownFacet");
        chain.add(RemoveFacet.ID).set("facet", "UnknownFacet");

        DocumentModel resultDoc = (DocumentModel) service.run(ctx, chain);
        assertFalse(resultDoc.hasFacet("UnknownFacet"));

    }

    @Test
    public void testAddFacetNoSave() throws OperationException {

        assertNotNull(docNoFacet);
        assertFalse("New doc should not have the facet.", docNoFacet.hasFacet(THE_FACET));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(docNoFacet);
        OperationChain chain = new OperationChain("testAddFacet");
        chain.add(AddFacet.ID).set("facet", THE_FACET).set("save", false);
        DocumentModel resultDoc = (DocumentModel)service.run(ctx, chain);

        assertNotNull(resultDoc);
        assertTrue("The doc should now have the facet.", resultDoc.hasFacet(THE_FACET));

        resultDoc.refresh();
        assertFalse("Doc should not have the facet after refresh", resultDoc.hasFacet(THE_FACET));

    }

    @Test
    public void testRemoveFacetNoSave() throws OperationException {

        //remove from a document with facet
        assertNotNull(docWithFacet);
        assertTrue("New doc should have the facet.", docWithFacet.hasFacet(THE_FACET));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(docWithFacet);
        OperationChain chain = new OperationChain("testRemoveFacet");
        chain.add(RemoveFacet.ID).set("facet", THE_FACET).set("save",  false);
        DocumentModel resultDoc = (DocumentModel)service.run(ctx, chain);

        assertNotNull(resultDoc);
        assertFalse("The doc should not have the facet.", resultDoc.hasFacet(THE_FACET));

        resultDoc.refresh();
        assertTrue("Doc should have the facet after refresh", resultDoc.hasFacet(THE_FACET));
    }

}
