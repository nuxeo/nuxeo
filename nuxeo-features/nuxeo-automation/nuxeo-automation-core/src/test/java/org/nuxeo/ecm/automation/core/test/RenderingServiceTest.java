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

import java.net.URL;

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.rendering.Renderer;
import org.nuxeo.ecm.automation.core.rendering.operations.RenderDocument;
import org.nuxeo.ecm.automation.core.rendering.operations.RenderDocumentFeed;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.resource.ResourceService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
public class RenderingServiceTest {

    protected DocumentModel src;

    protected DocumentModel dst;

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Before
    public void initRepo() throws Exception {
        session.removeChildren(session.getRootDocument().getRef());
        session.save();

        src = session.createDocumentModel("/", "src", "File");
        src.setPropertyValue("dc:title", "Source");
        src = session.createDocument(src);
        session.save();
        src = session.getDocument(src.getRef());

        dst = session.createDocumentModel("/", "dst", "Workspace");
        dst.setPropertyValue("dc:title", "Destination");
        dst = session.createDocument(dst);
        session.save();
        dst = session.getDocument(dst.getRef());

    }

    // ------ Tests comes here --------

    @Test
    public void testRendering() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        OperationChain chain = new OperationChain("testRenderingFm");
        chain.add(FetchContextDocument.ID);
        chain.add(RenderDocument.ID).set("template", "Hello ${This.title}");
        Blob blob = (Blob) service.run(ctx, chain);
        assertEquals("Hello Source", blob.getString());

        // again but with mvel
        ctx = new OperationContext(session);
        ctx.setInput(src);
        chain = new OperationChain("testRenderingMvel");
        chain.add(FetchContextDocument.ID);
        chain.add(RenderDocument.ID).set("template", "Hello ${This.title} ${CurrentUser.name}").set("type", "mvel");
        blob = (Blob) service.run(ctx, chain);
        assertEquals("Hello Source Administrator", blob.getString());

        // same test but using a list of docs
        ctx = new OperationContext(session);
        DocumentModelList list = new DocumentModelListImpl();
        list.add(src);
        list.add(dst);
        ctx.setInput(list);
        chain = new OperationChain("testRenderingMvel2");
        chain.add(RenderDocument.ID).set("template", "${This.title}").set("type", "mvel");
        BlobList blobs = (BlobList) service.run(ctx, chain);
        assertEquals(2, blobs.size());
        assertEquals("Source", blobs.get(0).getString());
        assertEquals("Destination", blobs.get(1).getString());

        ctx = new OperationContext(session);
        ctx.setInput(list);
        chain = new OperationChain("testRenderingFtl2");
        chain.add(RenderDocument.ID).set("template", "${This.title}");
        blobs = (BlobList) service.run(ctx, chain);
        assertEquals(2, blobs.size());
        assertEquals("Source", blobs.get(0).getString());
        assertEquals("Destination", blobs.get(1).getString());

    }

    @Test
    public void testRenderingFeed() throws Exception {
        URL url = getClass().getClassLoader().getResource("render.mvel");
        Framework.getService(ResourceService.class).addResource("render.mvel", url);
        url = getClass().getClassLoader().getResource("render.ftl");
        Framework.getService(ResourceService.class).addResource("render.ftl", url);

        OperationContext ctx = new OperationContext(session);
        DocumentModelList list = new DocumentModelListImpl();
        list.add(src);
        list.add(dst);
        ctx.setInput(list);
        OperationChain chain = new OperationChain("testRenderingFeed");
        chain.add(RenderDocumentFeed.ID).set("template", Renderer.TEMPLATE_PREFIX + "render.mvel").set("type", "mvel");
        Blob blob = (Blob) service.run(ctx, chain);
        String r = blob.getString();
        r = r.replaceAll("\\s+", "");
        assertEquals("SourceDestination", r);

        ctx = new OperationContext(session);
        ctx.setInput(list);
        chain = new OperationChain("testRenderingFeed2");
        chain.add(RenderDocumentFeed.ID).set("template", Renderer.TEMPLATE_PREFIX + "render.ftl");
        blob = (Blob) service.run(ctx, chain);
        r = blob.getString();
        r = r.replaceAll("\\s+", "");
        assertEquals("SourceDestination", r);

    }

}
