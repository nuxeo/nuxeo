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
package org.nuxeo.ecm.platform.picture.core.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeFalse;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.platform.picture.operation.PictureResize;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@Deploy("org.nuxeo.ecm.platform.picture.api")
@Deploy("org.nuxeo.ecm.platform.picture.convert")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@Deploy("org.nuxeo.ecm.platform.tag")
@Deploy("org.nuxeo.ecm.platform.rendition.core")
public class TestPictureResize {

    @Inject
    AutomationService service;

    @Inject
    protected EventService eventService;

    @Inject
    CoreSession session;

    protected OperationContext ctx;

    @Before
    public void createOperationContext() {
        ctx = new OperationContext(session);
    }

    @After
    public void closeOperationContext() {
        ctx.close();
    }

    @Ignore(value = "NXP-29089")
    @Test
    public void testResizer() throws Exception {
        // TODO: NXP-27622
        assumeFalse(SystemUtils.IS_OS_MAC);
        Blob source = Blobs.createBlob(FileUtils.getResourceFileFromContext("images/test.jpg"), "image/jpeg");

        ctx.setInput(source);

        Map<String, Object> params = new HashMap<>();
        params.put("maxWidth", 150);
        params.put("maxHeight", 300);
        OperationChain chain = new OperationChain("fakeChain");
        OperationParameters oparams = new OperationParameters(PictureResize.ID, params);
        chain.add(oparams);

        Blob result = (Blob) service.run(ctx, chain);

        assertNotNull(result);
    }

    @Ignore(value = "NXP-29089")
    @Test
    public void testResizerForDoc() throws Exception{
        // TODO: NXP-27622
        assumeFalse(SystemUtils.IS_OS_MAC);

        DocumentModel pictureDoc = session.createDocumentModel("/", "testpicture", "Picture");
        Blob source = Blobs.createBlob(FileUtils.getResourceFileFromContext("images/test.jpg"), "image/jpeg");
        pictureDoc.setPropertyValue("file:content", (Serializable) source);
        pictureDoc = session.createDocument(pictureDoc);

        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        eventService.waitForAsyncCompletion();

        OperationContext ctx = new OperationContext(session);

        ctx.setInput(pictureDoc);

        Map<String, Object> params = new HashMap<>();
        params.put("maxWidth", 150);
        params.put("maxHeight", 300);
        OperationChain chain = new OperationChain("fakeChain2");
        OperationParameters oparams = new OperationParameters(PictureResize.ID, params);
        chain.add(oparams);

        Blob result = (Blob) service.run(ctx, chain);

        assertNotNull(result);
    }
}
