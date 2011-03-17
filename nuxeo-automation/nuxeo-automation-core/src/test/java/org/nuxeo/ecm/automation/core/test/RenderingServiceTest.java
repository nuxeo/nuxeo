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

import java.net.URL;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

import com.google.inject.Inject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core" })
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
        Blob blob = (Blob)service.run(ctx, chain);
        Assert.assertEquals("Hello Source", blob.getString());

        // again but with mvel
        ctx = new OperationContext(session);
        ctx.setInput(src);
        chain = new OperationChain("testRenderingMvel");
        chain.add(FetchContextDocument.ID);
        chain.add(RenderDocument.ID).set("template", "Hello ${This.title} ${CurrentUser.name}").set("type", "mvel");
        blob = (Blob)service.run(ctx, chain);
        Assert.assertEquals("Hello Source Administrator", blob.getString());

        // same test but using a list of docs
        ctx = new OperationContext(session);
        DocumentModelList list = new DocumentModelListImpl();
        list.add(src);
        list.add(dst);
        ctx.setInput(list);
        chain = new OperationChain("testRenderingMvel2");
        chain.add(RenderDocument.ID).set("template", "${This.title}").set("type", "mvel");
        BlobList blobs = (BlobList)service.run(ctx, chain);
        Assert.assertEquals(2, blobs.size());
        Assert.assertEquals("Source", blobs.get(0).getString());
        Assert.assertEquals("Destination", blobs.get(1).getString());

        ctx = new OperationContext(session);
        ctx.setInput(list);
        chain = new OperationChain("testRenderingFtl2");
        chain.add(RenderDocument.ID).set("template", "${This.title}");
        blobs = (BlobList)service.run(ctx, chain);
        Assert.assertEquals(2, blobs.size());
        Assert.assertEquals("Source", blobs.get(0).getString());
        Assert.assertEquals("Destination", blobs.get(1).getString());

    }

    @Test
    public void testRenderingFeed() throws Exception {
        URL url = getClass().getClassLoader().getResource("render.mvel");
        Framework.getLocalService(ResourceService.class).addResource("render.mvel", url);
        url = getClass().getClassLoader().getResource("render.ftl");
        Framework.getLocalService(ResourceService.class).addResource("render.ftl", url);

        OperationContext ctx = new OperationContext(session);
        DocumentModelList list = new DocumentModelListImpl();
        list.add(src);
        list.add(dst);
        ctx.setInput(list);
        OperationChain chain = new OperationChain("testRenderingFeed");
        chain.add(RenderDocumentFeed.ID).set("template", Renderer.TEMPLATE_PREFIX+"render.mvel").set("type", "mvel");
        Blob blob = (Blob)service.run(ctx, chain);
        String r = blob.getString();
        r = r.replaceAll("\\s+", "");
        Assert.assertEquals("SourceDestination", r);

        ctx = new OperationContext(session);
        ctx.setInput(list);
        chain = new OperationChain("testRenderingFeed2");
        chain.add(RenderDocumentFeed.ID).set("template", Renderer.TEMPLATE_PREFIX+"render.ftl");
        blob = (Blob)service.run(ctx, chain);
        r = blob.getString();
        r = r.replaceAll("\\s+", "");
        Assert.assertEquals("SourceDestination", r);

    }

}
