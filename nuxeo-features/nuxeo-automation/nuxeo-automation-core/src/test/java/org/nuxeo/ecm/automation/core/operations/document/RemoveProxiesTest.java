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
 *     Frederic Vadon
 *     Ricardo Dias
 */
package org.nuxeo.ecm.automation.core.operations.document;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
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
public class RemoveProxiesTest {

    @Inject
    CoreSession session;

    @Inject
    AutomationService service;

    protected DocumentModel folder;
    protected DocumentModel section;
    protected DocumentModel fileToPublish;

    protected OperationContext ctx;

    @Before
    public void initRepo() throws Exception {
        folder = session.createDocumentModel("/", "Folder", "Folder");
        folder.setPropertyValue("dc:title", "Folder");
        folder = session.createDocument(folder);
        session.save();
        folder = session.getDocument(folder.getRef());

        section = session.createDocumentModel("/", "Section", "Section");
        section.setPropertyValue("dc:title", "Section");
        section = session.createDocument(section);
        session.save();
        section = session.getDocument(section.getRef());

        fileToPublish = session.createDocumentModel("/Folder", "FileToPublish", "File");
        fileToPublish.setPropertyValue("dc:title", "File");
        fileToPublish = session.createDocument(fileToPublish);
        session.save();
        fileToPublish = session.getDocument(fileToPublish.getRef());
        ctx = new OperationContext(session);
    }

    @After
    public void closeOperationContext() {
        ctx.close();
    }

    @Test
    public void testRemoveProxies() throws OperationException {
        ctx.setInput(fileToPublish);
        OperationChain chain = new OperationChain("publishDocument");
        chain.add(FetchContextDocument.ID);
        chain.add(PublishDocument.ID).set("target",section.getId());
        DocumentModel publishedDoc = (DocumentModel)service.run(ctx, chain);

        Assert.assertEquals("Section", session.getDocument(publishedDoc.getParentRef()).getTitle());
        Assert.assertEquals(1, session.getChildren(section.getRef()).size());

        OperationChain removeProxies = new OperationChain("testRemoveProxies");
        removeProxies.add(FetchContextDocument.ID);
        removeProxies.add(RemoveProxies.ID);
        service.run(ctx, removeProxies);

        Assert.assertEquals(0, session.getChildren(section.getRef()).size());
    }

}
