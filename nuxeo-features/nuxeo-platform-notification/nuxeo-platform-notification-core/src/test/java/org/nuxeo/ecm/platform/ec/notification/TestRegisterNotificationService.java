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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.notification.api.Notification;
import org.nuxeo.ecm.platform.notification.api.NotificationRegistry;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;
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
    public void setUp() throws Exception {
        super.setUp();

        // set properties needed for tests
        assertTrue(runtime instanceof OSGiRuntimeService);
        File notificationsPropertiesFile = FileUtils.getResourceFileFromContext("notifications.properties");
        InputStream notificationsProperties = new FileInputStream(notificationsPropertiesFile);
        ((OSGiRuntimeService) runtime).loadProperties(notificationsProperties);

        deployContrib("org.nuxeo.ecm.platform.notification.core.tests",
                "NotificationService.xml");
        deployContrib("org.nuxeo.ecm.platform.notification.core.tests",
                "notification-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.notification.core.tests",
                "notification-contrib-overridden.xml");
        notificationService = (NotificationService) runtime.getComponent(NotificationService.NAME);
        notificationRegistry = notificationService.getNotificationRegistry();
    }

    public void testRegistration() {
        List<Notification> notifsForVersion = notificationRegistry.getNotificationsForEvent("version_created");
        List<Notification> notifsForWorkflowStarted = notificationRegistry.getNotificationsForEvent("workflowStarted");

        assertEquals(0, notifsForVersion.size());
        assertEquals(1, notifsForWorkflowStarted.size());
    }

    public void testTemplateOverride() {
        URL newModifTemplate = notificationService.getTemplateURL("modif");
        assertTrue(newModifTemplate.getFile().endsWith("templates/modif_fr.ftl"));
    }

    protected List<String> sortedNotificationNames(List<Notification> notifs) {
        List<String> names = new ArrayList<String>(notifs.size());
        for (Notification notif : notifs) {
            names.add(notif.getName());
        }
        Collections.sort(names);
        return names;
    }

    public void testAvailableIn() {
        List<Notification> notifs = notificationRegistry.getNotificationsForSubscriptions("section");
        assertEquals(Arrays.asList("Ajout d'un commentaire",
                "Publication de contenu", "Something important",
                "Workflow Change"), sortedNotificationNames(notifs));

        notifs = notificationRegistry.getNotificationsForSubscriptions("workspace");
        assertEquals(Arrays.asList("Ajout d'un commentaire",
                "Approbation review started",
                "Cr\u00e9ation/modification de contenu", "Something important",
                "Workflow Change"), sortedNotificationNames(notifs));

        notifs = notificationRegistry.getNotificationsForSubscriptions("something");
        assertEquals(
                Arrays.asList("Ajout d'un commentaire", "Workflow Change"),
                sortedNotificationNames(notifs));
    }

    public void testExpandVarsInGeneralSettings() {
        assertEquals("http://testServerPrefix/nuxeo", notificationService.getServerUrlPrefix());
        assertEquals("testSubjectPrefix", notificationService.getEMailSubjectPrefix());

        // this on should not be expanded
        assertEquals("${not.existing.property}", notificationService.getMailSessionJndiName());
    }

}
