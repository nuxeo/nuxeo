/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.coldstorage.operations;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.coldstorage.helpers.ColdStorageHelper;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 11.1
 */
@Deploy("org.nuxeo.ecm.platform.notification.core")
public class RequestRetrievalFromColdStorageTest extends AbstractTestColdStorageOperation {

    protected static final int NUMBER_OF_DAYS_OF_AVAILABILITY = 5;

    @Inject
    protected CoreSession session;

    @Inject
    protected NotificationManager notificationManager;

    @Test
    public void shouldRequestRetrieval() throws OperationException, IOException {
        DocumentModel documentModel = createFileDocument(session, true);
        // first make the move to cold storage
        moveContentToColdStorage(session, documentModel);
        // request a retrieval from the cold storage
        requestRetrievalContentFromColdStorage(documentModel);
    }

    @Test
    public void shouldFailRequestRetrievalBeingRetrieved() throws IOException, OperationException {
        DocumentModel documentModel = createFileDocument(session, true);

        // move the blob to cold storage
        moveContentToColdStorage(session, documentModel);

        // request a retrieval from the cold storage
        requestRetrievalContentFromColdStorage(documentModel);

        // request a retrieval for a second time
        try {
            requestRetrievalContentFromColdStorage(documentModel);
            fail("Should fail because the cold storage content is being retrieved.");
        } catch (NuxeoException e) {
            assertEquals(SC_FORBIDDEN, e.getStatusCode());
        }
    }

    @Test
    public void shouldFailRequestRetrievalNoContent() throws OperationException {
        DocumentModel documentModel = createFileDocument(session, true);
        try {
            // request a retrieval from the cold storage
            requestRetrievalContentFromColdStorage(documentModel);
            fail("Should fail because there no cold storage content associated to this document.");
        } catch (NuxeoException e) {
            assertEquals(SC_NOT_FOUND, e.getStatusCode());
        }
    }

    protected void requestRetrievalContentFromColdStorage(DocumentModel documentModel) throws OperationException {
        try (OperationContext context = new OperationContext(session)) {
            context.setInput(documentModel);
            Map<String, Integer> params = Map.of("numberOfDaysOfAvailability", NUMBER_OF_DAYS_OF_AVAILABILITY);
            DocumentModel updatedDocument = (DocumentModel) automationService.run(context,
                    RequestRetrievalFromColdStorage.ID, params);
            assertEquals(documentModel.getRef(), updatedDocument.getRef());
            assertEquals(Boolean.TRUE,
                    updatedDocument.getPropertyValue(ColdStorageHelper.COLD_STORAGE_BEING_RETRIEVED_PROPERTY));
            String username = NotificationConstants.USER_PREFIX + session.getPrincipal().getName();
            List<String> subscriptions = notificationManager.getSubscriptionsForUserOnDocument(username,
                    updatedDocument);
            assertTrue(subscriptions.contains(ColdStorageHelper.COLD_STORAGE_CONTENT_AVAILABLE_NOTIFICATION_NAME));
        }
    }

}
