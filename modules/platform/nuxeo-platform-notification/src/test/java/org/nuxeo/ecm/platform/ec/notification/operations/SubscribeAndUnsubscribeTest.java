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
 *     Miguel Nixo
 */
package org.nuxeo.ecm.platform.ec.notification.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

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
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.ec.notification.SubscriptionAdapter;
import org.nuxeo.ecm.platform.ec.notification.automation.SubscribeOperation;
import org.nuxeo.ecm.platform.ec.notification.automation.UnsubscribeOperation;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 8.10
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.platform.notification")
public class SubscribeAndUnsubscribeTest {

    protected DocumentModel testWorkspace;

    protected DocumentModelList listDocModel;

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected AutomationService automationService;

    protected OperationContext ctx;

    @Before
    public void setUp() {
        testWorkspace = coreSession.createDocumentModel("/default-domain/workspaces", "testWorkspace", "Workspace");
        testWorkspace = coreSession.createDocument(testWorkspace);
        String testWorkspacePath = testWorkspace.getPath().toString();
        List<DocumentModel> listDocs = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            DocumentModel testFile = coreSession.createDocumentModel(testWorkspacePath, "testFile" + i, "File");
            testFile = coreSession.createDocument(testFile);
            listDocs.add(testFile);
        }
        listDocModel = new DocumentModelListImpl(listDocs);
        ctx = new OperationContext(coreSession);
    }

    @After
    public void closeOperationContext() {
        ctx.close();
    }

    @Test
    public void testSubscribeAndUnsubscribeOperations() throws OperationException {
        OperationChain chain = new OperationChain("test-chain");
        chain.add(SubscribeOperation.ID);
        ctx.setInput(listDocModel);
        String username = NotificationConstants.USER_PREFIX + ctx.getPrincipal().getName();

        for (DocumentModel doc : listDocModel) {
            List<?> docSubscriptions = doc.getAdapter(SubscriptionAdapter.class).getUserSubscriptions(username);
            assertTrue(docSubscriptions.isEmpty());
        }

        // subscribe all documents
        listDocModel = (DocumentModelList) automationService.run(ctx, chain);

        for (DocumentModel doc : listDocModel) {
            List<?> docSubscriptions = doc.getAdapter(SubscriptionAdapter.class).getUserSubscriptions(username);
            assertEquals(4, docSubscriptions.size());
            assertTrue(docSubscriptions.contains("Creation"));
            assertTrue(docSubscriptions.contains("Modification"));
            assertTrue(docSubscriptions.contains("Workflow Change"));
            assertTrue(docSubscriptions.contains("Approbation review started"));
        }

        chain = new OperationChain("test-chain");
        chain.add(UnsubscribeOperation.ID);
        ctx.clear();
        ctx.setInput(listDocModel);
        username = NotificationConstants.USER_PREFIX + ctx.getPrincipal().getName();

        // unsubscribe all documents
        listDocModel = (DocumentModelList) automationService.run(ctx, chain);

        for (DocumentModel doc : listDocModel) {
            List<?> docSubscriptions = doc.getAdapter(SubscriptionAdapter.class).getUserSubscriptions(username);
            assertTrue(docSubscriptions.isEmpty());
        }
    }

    @Test
    public void testSelectiveSubscribeAndUnsubscribeOperations() throws OperationException {
        Map<String, Object> params = Map.of("notifications", List.of("Creation"));

        OperationChain chain = new OperationChain("test-chain");
        chain.add(SubscribeOperation.ID).from(params);
        ctx.setInput(listDocModel);
        String username = NotificationConstants.USER_PREFIX + ctx.getPrincipal().getName();

        for (DocumentModel doc : listDocModel) {
            List<?> docSubscriptions = doc.getAdapter(SubscriptionAdapter.class).getUserSubscriptions(username);
            assertTrue(docSubscriptions.isEmpty());
        }

        // subscribe all documents
        listDocModel = (DocumentModelList) automationService.run(ctx, chain);

        for (DocumentModel doc : listDocModel) {
            List<?> docSubscriptions = doc.getAdapter(SubscriptionAdapter.class).getUserSubscriptions(username);
            assertEquals(1, docSubscriptions.size());
            assertTrue(docSubscriptions.contains("Creation"));
        }

        chain = new OperationChain("test-chain");
        chain.add(UnsubscribeOperation.ID).from(params);
        ctx.clear();
        ctx.setInput(listDocModel);
        username = NotificationConstants.USER_PREFIX + ctx.getPrincipal().getName();

        // unsubscribe all documents
        listDocModel = (DocumentModelList) automationService.run(ctx, chain);

        for (DocumentModel doc : listDocModel) {
            List<?> docSubscriptions = doc.getAdapter(SubscriptionAdapter.class).getUserSubscriptions(username);
            assertTrue(docSubscriptions.isEmpty());
        }
    }

}
