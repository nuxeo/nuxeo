/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benjamin JALON <bjalon@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.notification;

import static org.mockito.Mockito.when;
import static junit.framework.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author <a href="mailto:bjalon@nuxeo.com">Benjamin JALON</a>
 * @since 5.9.1
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, MockitoFeature.class })
public class EmailResolutionTest {

    @Mock
    @RuntimeService
    UserManager um;

    @Mock
    DocumentModel user1;

    @Mock
    DocumentModel user2;

    @Before
    public void doBefore() throws Exception {

        when(user1.getPropertyValue("email")).thenReturn("user1@example.com");
        when(user1.getPropertyValue("firstName")).thenReturn("Victor");
        when(user1.getPropertyValue("lastName")).thenReturn("Hugo");

        when(user2.getPropertyValue("email")).thenReturn("user2@example.com");
        when(user2.getPropertyValue("firstName")).thenReturn(null);
        when(user2.getPropertyValue("lastName")).thenReturn(null);

        when(um.getUserModel("user1")).thenReturn(user1);
        when(um.getUserModel("user2")).thenReturn(user2);
    }

    @Test
    public void shouldResolveEmail() {
        List<MailBox> mailBoxes = MailBox.fetchPersonsFromString("unknown@example.com", true);

        assertEquals(1, mailBoxes.size());
        assertEquals("unknown@example.com", mailBoxes.get(0).toString());

        mailBoxes = MailBox.fetchPersonsFromString("unknown@example.com", false);

        assertEquals(1, mailBoxes.size());
        assertEquals("unknown@example.com", mailBoxes.get(0).toString());
    }

    @Test
    public void shouldResolveCommaSeparatedEmailsAsEmails() {
        List<MailBox> mailBoxes = MailBox.fetchPersonsFromString("unknown@example.com,unknown2@example.com", true);

        assertEquals(1, mailBoxes.size());
        assertEquals("unknown@example.com,unknown2@example.com", mailBoxes.get(0).toString());

        mailBoxes = MailBox.fetchPersonsFromString("unknown@example.com,unknown2@example.com", false);

        assertEquals(2, mailBoxes.size());
        assertEquals("unknown@example.com", mailBoxes.get(0).toString());
        assertEquals("unknown2@example.com", mailBoxes.get(1).toString());
    }

    @Test
    public void shouldResolveUserWithoutPrefix() {
        List<MailBox> mailBoxes = MailBox.fetchPersonsFromString("user1", true);
        assertEquals(1, mailBoxes.size());
        // As strict is considered as email address
        assertEquals("user1", mailBoxes.get(0).toString());

        mailBoxes = MailBox.fetchPersonsFromString("user1", false);
        assertEquals(1, mailBoxes.size());
        assertEquals("Victor Hugo<user1@example.com>", mailBoxes.get(0).toString());

        mailBoxes = MailBox.fetchPersonsFromString("user2", false);
        assertEquals(1, mailBoxes.size());
        assertEquals("user2@example.com", mailBoxes.get(0).toString());
    }

    @Test
    public void shouldResolveUserWithPrefix() {
        List<MailBox> mailBoxes = MailBox.fetchPersonsFromString("user:user1", true);

        assertEquals(1, mailBoxes.size());
        assertEquals("user1@example.com", mailBoxes.get(0).toString());

        mailBoxes = MailBox.fetchPersonsFromString("user:user1", false);

        assertEquals(1, mailBoxes.size());
        assertEquals("user1@example.com", mailBoxes.get(0).toString());
    }

    @Test
    public void shouldResolveUsersWithoutPrefix() {
        List<MailBox> mailBoxes = MailBox.fetchPersonsFromString("user1,user2", true);
        assertEquals(1, mailBoxes.size());
        // As strict is considered as email
        assertEquals("user1,user2", mailBoxes.get(0).toString());

        mailBoxes = MailBox.fetchPersonsFromString("user1,user2", false);
        assertEquals(2, mailBoxes.size());
        assertEquals("Victor Hugo<user1@example.com>", mailBoxes.get(0).toString());
        assertEquals("user2@example.com", mailBoxes.get(1).toString());
    }

    @Test
    public void shouldResolveUsersWithPrefix() {
        List<MailBox> mailBoxes = MailBox.fetchPersonsFromString("user:user1,user:user2", true);
        assertEquals(1, mailBoxes.size());
        assertEquals("user:user1,user:user2", mailBoxes.get(0).toString());

        mailBoxes = MailBox.fetchPersonsFromString("user:user1,user:user2", false);
        assertEquals(2, mailBoxes.size());
        assertEquals("user1@example.com", mailBoxes.get(0).toString());
        assertEquals("user2@example.com", mailBoxes.get(1).toString());
    }
}
