/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nour AL KOTOB
 */
package org.nuxeo.ecm.automation.core.operations.document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.Serializable;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class CopyDocumentTest {

    public static final String COPY_FOLDER_NAME = "CopyFolder";

    public static final String ROOT = "/";

    public static final String COPY_DOC_NAME = "copyDoc";

    public static final String TARGET_PROPERTY_KEY = "target";

    public static final String NAME_PROPERTY_KEY = "name";

    public static final String FILE = "File";

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void testCopyChildren() throws OperationException {
        DocumentModel folder = session.createDocumentModel(ROOT, "SourceFolder", "Folder");
        folder = session.createDocument(folder);

        DocumentModel childDoc = session.createDocumentModel(folder.getPathAsString(), "childDoc", FILE);
        session.createDocument(childDoc);

        try (OperationContext context = new OperationContext(session)) {
            context.setInput(folder);

            Map<String, Serializable> params = Map.of(TARGET_PROPERTY_KEY, ROOT, NAME_PROPERTY_KEY, COPY_FOLDER_NAME);
            DocumentModel result = (DocumentModel) automationService.run(context, CopyDocument.ID, params);

            result = session.getDocument(result.getRef());
            assertNotEquals(folder.getId(), result.getId());
            assertEquals(COPY_FOLDER_NAME, result.getName());
            DocumentModel childCopy = session.getChild(result.getRef(), childDoc.getName());
            assertNotEquals(childDoc.getId(), childCopy.getId());
            assertEquals(1, session.getChildren(result.getRef()).size());
        }
    }

    @Test
    public void testLifeCycleResetOnCopy() throws OperationException {
        DocumentModel source = session.createDocumentModel(ROOT, "Source", FILE);
        source = session.createDocument(source);

        String initialLifeCycleState = source.getCurrentLifeCycleState();

        // Change the source current lifecycle state
        source.followTransition("approve");
        source = session.getDocument(source.getRef());

        // Call Document.Copy without resetting the lifecycle state of the copy
        Map<String, Serializable> params = Map.of(TARGET_PROPERTY_KEY, ROOT, NAME_PROPERTY_KEY, COPY_DOC_NAME);

        try (OperationContext context = new OperationContext(session)) {
            context.setInput(source);
            DocumentModel result = (DocumentModel) automationService.run(context, CopyDocument.ID, params);
            txFeature.nextTransaction();

            result = session.getDocument(result.getRef());
            assertEquals("approved", result.getCurrentLifeCycleState());
        }

        // Call Document.Copy and reset the lifecycle state of the copy
        params = Map.of(TARGET_PROPERTY_KEY, ROOT, NAME_PROPERTY_KEY, COPY_DOC_NAME, "resetLifeCycle", true);

        try (OperationContext context = new OperationContext(session)) {
            context.setInput(source);
            DocumentModel result = (DocumentModel) automationService.run(context, CopyDocument.ID, params);
            txFeature.nextTransaction();

            result = session.getDocument(result.getRef());
            assertEquals(initialLifeCycleState, result.getCurrentLifeCycleState());
        }
    }

    @Test
    public void testVersionNotNullOnCopy() throws OperationException {
        DocumentModel source = session.createDocumentModel(ROOT, "Source", FILE);
        source = session.createDocument(source);

        Map<String, Serializable> params = Map.of(TARGET_PROPERTY_KEY, ROOT, NAME_PROPERTY_KEY, COPY_DOC_NAME);

        try (OperationContext context = new OperationContext(session)) {
            context.setInput(source);
            DocumentModel result = (DocumentModel) automationService.run(context, CopyDocument.ID, params);
            assertEquals(0, (long) result.getPropertyValue("uid:major_version"));
            assertEquals(0, (long) result.getPropertyValue("uid:minor_version"));
        }
    }
}
