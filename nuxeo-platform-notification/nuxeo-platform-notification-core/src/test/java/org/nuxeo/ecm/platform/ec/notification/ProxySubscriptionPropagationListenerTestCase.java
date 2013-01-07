/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.ec.notification;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.runtime.api.Framework;

public class ProxySubscriptionPropagationListenerTestCase extends
        SQLRepositoryTestCase {

    protected DocumentModel workspace;

    protected DocumentModel section;

    protected DocumentModel section2;

    protected DocumentModel doc1;

    protected DocumentModel proxy1;

    protected DocumentModel proxy2;

    protected EventServiceImpl eventService;

    protected void waitForEventsDispatched() {
        if (eventService == null) {
            eventService = (EventServiceImpl) Framework.getLocalService(EventService.class);
        }
        eventService.waitForAsyncCompletion();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.core.persistence");
        deployBundle("org.nuxeo.ecm.platform.placeful.api");
        deployBundle("org.nuxeo.ecm.platform.placeful.core");

        deployContrib("org.nuxeo.ecm.platform.placeful.core",
                "nxplacefulservice-configs-tests.xml");
        deployContrib("org.nuxeo.ecm.platform.placeful.core",
                "nxplaceful-tests.xml");

        deployBundle("org.nuxeo.ecm.platform.notification.api");
        deployBundle("org.nuxeo.ecm.platform.notification.core");

        openSession();

        workspace = session.createDocumentModel("Folder");
        workspace.setProperty("dublincore", "title", "Workspace");
        workspace.setPathInfo("/", "workspace");
        workspace = session.createDocument(workspace);

        section = session.createDocumentModel("Folder");
        section.setProperty("dublincore", "title", "Section");
        section.setPathInfo("/", "section");
        section = session.createDocument(section);

        section2 = session.createDocumentModel("Folder");
        section2.setProperty("dublincore", "title", "Section 2");
        section2.setPathInfo("/", "section-2");
        section2 = session.createDocument(section2);

        doc1 = session.createDocumentModel("File");
        doc1.setProperty("dublincore", "title", "Some file");
        doc1.setPathInfo("/workspace/", "file-1");
        doc1 = session.createDocument(doc1);

        proxy1 = session.publishDocument(doc1, section);
        proxy2 = session.publishDocument(doc1, section2);

        session.save();
        waitForEventsDispatched();
        closeSession();
    }

    public void testProxySubscriptionPropagation() throws Exception {
        // disable test because of transaction rollback problem that seem to be
        // a test setup artifact
        if ("disabled without eclipse notice".equals("disabled without eclipse notice")) {
            // one day we will switch to JUnit 4 an will no longer such hacks
            return;
        }

        NotificationManager notificationService = Framework.getService(NotificationManager.class);

        String user1 = "Mr Smith";
        String user2 = "Mr Anderson";

        openSession();
        proxy1 = session.getDocument(proxy1.getRef());
        proxy2 = session.getDocument(proxy2.getRef());

        // add a couple of subscription on proxy1 and proxy2
        addSomeSubscriptions(notificationService, user1, user2, proxy1);
        addSomeSubscriptions(notificationService, user1, user2, proxy2);

        checkSubscriptions(notificationService, user1, user2, proxy1);
        checkSubscriptions(notificationService, user1, user2, proxy2);

        // republish the document to section 1
        proxy1 = session.publishDocument(doc1, section);
        session.save();
        waitForEventsDispatched();

        closeSession();
        openSession();
        proxy1 = session.getDocument(proxy1.getRef());
        proxy2 = session.getDocument(proxy2.getRef());

        // previous subscriptions on proxies are still there
        checkSubscriptions(notificationService, user1, user2, proxy1);
        checkSubscriptions(notificationService, user1, user2, proxy2);

        // republish the document to section 1
        proxy2 = session.publishDocument(doc1, section2);
        session.save();
        waitForEventsDispatched();

        closeSession();
        openSession();
        proxy1 = session.getDocument(proxy1.getRef());
        proxy2 = session.getDocument(proxy2.getRef());

        // previous subscriptions on proxies are still there
        checkSubscriptions(notificationService, user1, user2, proxy1);
        checkSubscriptions(notificationService, user1, user2, proxy2);

    }

    protected void addSomeSubscriptions(
            NotificationManager notificationService, String user1,
            String user2, DocumentModel doc) throws ClientException {
        notificationService.addSubscription(user1, "Task assigned", doc, false,
                null, null);
        notificationService.addSubscription(user2,
                "Approbation review started", doc, false, null, null);
        notificationService.addSubscription(user2, "Task assigned", doc, false,
                null, null);
    }

    protected void checkSubscriptions(NotificationManager notificationService,
            String user1, String user2, DocumentModel doc)
            throws ClassNotFoundException, ClientException {
        List<String> subscriptions = notificationService.getSubscriptionsForUserOnDocument(
                user1, doc.getRef().toString());
        assertNotNull(subscriptions);
        assertEquals(1, subscriptions.size());
        assertEquals(Arrays.asList("Task assigned"), subscriptions);

        subscriptions = notificationService.getSubscriptionsForUserOnDocument(
                user2, doc.getRef().toString());
        assertNotNull(subscriptions);
        assertEquals(2, subscriptions.size());
    }

}
