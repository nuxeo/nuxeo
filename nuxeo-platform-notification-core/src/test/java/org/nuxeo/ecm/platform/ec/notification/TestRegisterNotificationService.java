/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestRegisterPlacefulService.java 13110 2007-03-01 17:25:47Z rspivak $
 */
package org.nuxeo.ecm.platform.ec.notification;

import java.net.URL;
import java.util.List;

import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.notification.api.NotificationRegistry;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 *
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 *
 */
public class TestRegisterNotificationService extends NXRuntimeTestCase {

    NotificationService notificationService;
    NotificationRegistry notificationRegistry;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deploy("NotificationService.xml");
        deploy("notification-contrib.xml");
        deploy("notification-contrib-overridden.xml");
        notificationService = (NotificationService) runtime.getComponent(NotificationService.NAME);
        notificationRegistry = notificationService.getNotificationRegistry();
    }

    public void testResgitration() {
        List<Notification> notifsForVersion = notificationRegistry.getNotificationsForEvent(
                "version_created");
        List<Notification> notifsForWorkflowStarted = notificationRegistry.getNotificationsForEvent(
                "workflowStarted");

        assertEquals(0, notifsForVersion.size());
    }

    public void testTemplateOverride() {
        URL newModifTemplate = notificationService.getTemplateURL("modif");
        assertTrue(newModifTemplate.getFile().endsWith("templates/modif_fr.ftl"));
    }

}
