/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     narcis
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ec.notification;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.api.login.UserSession;
import org.nuxeo.ecm.platform.api.test.NXClientTestCase;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 *
 */
public class TestNotificationServiceEJB3 extends NXClientTestCase {

    private static final Log log = LogFactory.getLog(TestNotificationServiceEJB3.class);

    UserSession us;

    CoreSession coreSession;

    NotificationManager service;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deploy("OSGI-INF/CacheBinding.xml");
        deploy("OSGI-INF/PlatformService.xml");
        deploy("DefaultPlatform.xml");

        us = new UserSession("Administrator", "Administrator");
        us.login();
        // ------------ user session started -----------

        service = Framework.getService(NotificationManager.class);
//        coreSession = ECM.getPlatform().openRepository("demo");
//
        assertNotNull("EJB3 not found", service);
    }

    @Override
    protected void tearDown() throws Exception {
        us.logout();
        super.tearDown();
    }

    public void testNotificationService() throws Exception {
        log.info("testNotificationService");
        String usernameTest = "testUserName";
        String notifTest = "notifTest";
        String docIdTest = "My Document ID";
        DocumentModel docTest = new MockDocumentModel("type", null);
        service.addSubscription(usernameTest, notifTest, docTest, false, null, "");

        List<String> users = service.getSubscribers(notifTest, docIdTest);
        assertNotNull("No subscribers", users);
        assertEquals("Should be 1 user subscribed", 1, users.size());
        assertEquals("Wrong name!!!", usernameTest, users.get(0));

        List<String> subscriptions = service.getSubscriptionsForUserOnDocument(
                usernameTest, docIdTest);
        assertNotNull("No subscriptions", subscriptions);
        assertEquals("Should be 1 subscription for user on document",
                1, subscriptions.size());
        assertEquals("Wrong subscription", notifTest, subscriptions.get(0));

        service.removeSubscription(usernameTest, notifTest, docIdTest);

        users = service.getSubscribers(notifTest, docIdTest);
        assertNotNull("No subscribers", users);
        assertEquals("Should be no user subscribed", 0, users.size());
    }

}
