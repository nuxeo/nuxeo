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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.RestoreDocumentInput;
import org.nuxeo.ecm.automation.core.operations.SetInputAsVar;
import org.nuxeo.ecm.automation.core.operations.blob.AttachBlob;
import org.nuxeo.ecm.automation.core.operations.blob.BlobToFile;
import org.nuxeo.ecm.automation.core.operations.blob.CreateBlob;
import org.nuxeo.ecm.automation.core.operations.blob.GetAllDocumentBlobs;
import org.nuxeo.ecm.automation.core.operations.blob.GetDocumentBlob;
import org.nuxeo.ecm.automation.core.operations.blob.GetDocumentBlobs;
import org.nuxeo.ecm.automation.core.operations.blob.SetBlobFileName;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.FetchDocument;
import org.nuxeo.ecm.automation.core.operations.document.RemoveDocumentBlob;
import org.nuxeo.ecm.automation.core.operations.document.SetDocumentBlob;
import org.nuxeo.ecm.automation.core.operations.document.UpdateDocument;
import org.nuxeo.ecm.automation.core.scripting.MvelTemplate;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.seleniumhq.jetty7.util.log.Log;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
public class DocumentUpdateOperationTest {

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    OperationChain chain;

    @Before
    public void initChain() throws Exception {
        chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("type", "File");
        params.put("name", "file");
        chain.add(CreateDocument.ID).from(params);

        params = new HashMap<String, Object>();
        params.put("properties", new MvelTemplate(
                "dc:title=Test\ndc:issued=@{org.nuxeo.ecm.core.schema.utils.DateParser.formatW3CDateTime(CurrentDate.date)}"));
        params.put("save", "true");
        chain.add(UpdateDocument.ID).from(params);
    }

    @After
    public void clearRepo() throws Exception {
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
    }

    @Test
    public void shouldUpdateProperties() throws Exception {

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(session.getRootDocument());

        DocumentModel doc = (DocumentModel) service.run(ctx, chain);
        assertNotNull(doc);
        assertEquals("Test", doc.getTitle());
        assertNotNull(doc.getPropertyValue("dc:issued"));
//        System.out.println(doc.getPropertyValue("dc:issued"));
    }
}
