/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.platform.ec.notification;

import static java.lang.Boolean.FALSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.audit.service.NXAuditEventsService.DISABLE_AUDIT_LOGGER;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.notification.core" })
public class TestNotificationManager {

    @Inject
    protected CoreSession session;

    @Inject
    protected NotificationManager notificationManager;

    @Test
    public void testSubscribedDocuments() {

        NuxeoPrincipal principal = (NuxeoPrincipal) session.getPrincipal();
        String prefixedPrincipalName = NuxeoPrincipal.PREFIX + principal.getName();
        String repositoryName = session.getRepositoryName();

        // No subscribed documents at first
        assertTrue(notificationManager.getSubscribedDocuments(prefixedPrincipalName, repositoryName).isEmpty());

        // Add subscriptions
        DocumentModel doc1 = session.createDocument(session.createDocumentModel("/", "doc1", "File"));
        DocumentModel doc2 = session.createDocument(session.createDocumentModel("/", "doc2", "File"));
        notificationManager.addSubscription(prefixedPrincipalName, "notification1", doc1, false, principal,
                "notification1");

        // Make sure we don't change the context data, that could affect following write operations
        assertNull(doc1.getContextData().get(DISABLE_AUDIT_LOGGER));

        notificationManager.addSubscription(prefixedPrincipalName, "notification2", doc1, false, principal,
                "notification1");
        notificationManager.addSubscription(prefixedPrincipalName, "notification1", doc2, false, principal,
                "notification1");
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        List<DocumentModel> subscribedDocuments = notificationManager.getSubscribedDocuments(prefixedPrincipalName,
                repositoryName);
        Collections.sort(subscribedDocuments, (a, b) -> a.getTitle().compareTo(b.getTitle()));
        assertEquals(Arrays.asList(doc1, doc2), subscribedDocuments);

        // Check that we can get the user's subscriptions from the doc adapter
        List<String> doc1Notifications = subscribedDocuments.get(0)
                                                            .getAdapter(SubscriptionAdapter.class)
                                                            .getUserSubscriptions(prefixedPrincipalName);
        Collections.sort(doc1Notifications);
        List<String> expectedDoc1Notifications = Arrays.asList("notification1", "notification2");
        assertEquals(expectedDoc1Notifications, doc1Notifications);
        List<String> doc2Notifications = subscribedDocuments.get(1)
                                                            .getAdapter(SubscriptionAdapter.class)
                                                            .getUserSubscriptions(prefixedPrincipalName);
        assertEquals(Collections.singletonList("notification1"), doc2Notifications);

        // Remove subscriptions
        notificationManager.removeSubscriptions(prefixedPrincipalName, expectedDoc1Notifications, doc1);
        notificationManager.removeSubscription(prefixedPrincipalName, "notification1", doc2);
        assertNull(doc2.getContextData().get(DISABLE_AUDIT_LOGGER));

        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

        assertTrue(notificationManager.getSubscribedDocuments(prefixedPrincipalName, repositoryName).isEmpty());
    }

    @Test
    // NXP-24185: test I can subscribe to published document
    public void testSubscriptionOnPublishedDocument() {

        NuxeoPrincipal principal = (NuxeoPrincipal) session.getPrincipal();
        String prefixedPrincipalName = NuxeoPrincipal.PREFIX + principal.getName();
        String repositoryName = session.getRepositoryName();

        // no subscribed documents at first
        assertTrue(notificationManager.getSubscribedDocuments(prefixedPrincipalName, repositoryName).isEmpty());

        // create container for publication and file
        DocumentModel section = session.createDocument(session.createDocumentModel("/", "section", "Section"));
        DocumentModel file = session.createDocument(session.createDocumentModel("/", "file", "File"));
        notificationManager.addSubscription(prefixedPrincipalName, "notification1", file, FALSE, principal,
                "notification1");
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }


        // publish it
        DocumentModel publishedDocument = session.publishDocument(file, section);
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }


        // check that notification was removed from version (which allows to subscribe to proxy)
        List<DocumentModel> subscribedDocuments = notificationManager.getSubscribedDocuments(prefixedPrincipalName,
                repositoryName);
        assertEquals(Collections.singletonList(file), subscribedDocuments);

        // add subscriptions to proxy
        notificationManager.addSubscription(prefixedPrincipalName, "notification1", publishedDocument, FALSE, principal,
                "notification1");
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }


        // check that we now have published document but not the version
        subscribedDocuments = notificationManager.getSubscribedDocuments(prefixedPrincipalName, repositoryName);
        subscribedDocuments.sort(Comparator.comparing(DocumentModel::getPathAsString));
        assertEquals(Arrays.asList(file, publishedDocument), subscribedDocuments);

        // Remove subscriptions
        notificationManager.removeSubscription(prefixedPrincipalName, "notification1", file);
        notificationManager.removeSubscription(prefixedPrincipalName, "notification1", publishedDocument);
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }


        assertTrue(notificationManager.getSubscribedDocuments(prefixedPrincipalName, repositoryName).isEmpty());
    }

}
