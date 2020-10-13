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

package org.nuxeo.ecm.platform.ec.notification;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.mail.SmtpMailServerFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Tests the whole email notification process: from subscribing to a given event to the reception of the email for this
 * event.
 *
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, SmtpMailServerFeature.class })
@Deploy("org.nuxeo.ecm.platform.notification.core")
@Deploy("org.nuxeo.ecm.platform.notification.api")
@Deploy("org.nuxeo.ecm.platform.url")
@Deploy("org.nuxeo.ecm.platform.notification.core.tests:OSGI-INF/notification-event-listener-contrib.xml")
public class TestEmailNotification {

    protected static final String DUMMY_NOTIFICATION_NAME = "DummyNotificationToSendMail";

    protected static final String DUMMY_EVENT_NAME = "dummyNotificationToSendMail";

    protected static final String DOCUMENT_NAME = "anyFile";

    protected static final SimpleDateFormat EVENT_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy - HH:mm");

    @Inject
    protected CoreSession session;

    @Inject
    protected NotificationManager notificationManager;

    @Inject
    protected EventService eventService;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected SmtpMailServerFeature.MailsResult emailsResult;

    protected DocumentModel domain;

    @Before
    public void before() {
        domain = session.createDocumentModel("/", "domain", "Domain");
        domain = session.createDocument(domain);
    }

    @Test
    public void shouldReceiveNotificationMailWhenSubscribeToDocument() {
        DocumentModel documentModel = session.createDocumentModel("/domain", DOCUMENT_NAME, "File");
        documentModel = session.createDocument(documentModel);

        addDummySubscription(documentModel);

        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), documentModel);
        Event event = ctx.newEvent(DUMMY_EVENT_NAME);
        eventService.fireEvent(event);
        transactionalFeature.nextTransaction();

        checkMailContent(documentModel, event);
    }

    @Test
    public void shouldReceiveNotificationMailWhenSubscribeToParentDocument() {
        DocumentModel folder = session.createDocumentModel("/domain", "anyFolder", "Folder");
        folder = session.createDocument(folder);

        addDummySubscription(folder);

        DocumentModel mainDocModel = session.createDocumentModel("/domain/anyFolder", DOCUMENT_NAME, "File");
        mainDocModel = session.createDocument(mainDocModel);

        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), mainDocModel);
        Event event = ctx.newEvent(DUMMY_EVENT_NAME);
        eventService.fireEvent(event);
        transactionalFeature.nextTransaction();

        checkMailContent(mainDocModel, event);
    }

    protected void checkMailContent(DocumentModel documentModel, Event event) {
        assertEquals(1, emailsResult.getMails().size());
        SmtpMailServerFeature.MailMessage mailMessage = emailsResult.getMails().get(0);
        // check the subject
        assertEquals(String.format("[Dummy]Notification on the document '%s'", documentModel.getTitle()),
                mailMessage.getSubject());

        // check the text content
        String expectedMailContent = getExpectedMailContent(documentModel, event);
        assertEquals(expectedMailContent, mailMessage.getContent());
    }

    protected void addDummySubscription(DocumentModel documentModel) {
        NuxeoPrincipal principal = session.getPrincipal();
        String subscriber = NotificationConstants.USER_PREFIX + principal.getName();
        notificationManager.addSubscription(subscriber, DUMMY_NOTIFICATION_NAME, documentModel, false, principal,
                DUMMY_NOTIFICATION_NAME);
    }

    protected String getExpectedMailContent(DocumentModel documentModel, Event event) {
        try {
            URL url = TestEmailNotification.class.getResource("/templates/dummyExpectedMail.html");
            String fileContent = Files.readString(Paths.get(url.toURI()));
            var model = Map.of("TITLE", documentModel.getTitle(), //
                    "CREATION_DATE", EVENT_DATE_FORMAT.format(Date.from(Instant.ofEpochMilli(event.getTime()))), //
                    "AUTHOR", ((String[]) documentModel.getPropertyValue("dc:contributors"))[0], //
                    "LOCATION", documentModel.getPathAsString(), //
                    "VERSION", documentModel.getVersionLabel(), //
                    "STATE", documentModel.getCurrentLifeCycleState());
            String content = StringUtils.expandVars(fileContent, model);
            // just put the content on one line as it will be when retrieved from fake smtp
            return content.replaceAll("\\n", "");
        } catch (URISyntaxException | IOException e) {
            throw new NuxeoException(e);
        }
    }
}
