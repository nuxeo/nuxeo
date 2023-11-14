/*
 * (C) Copyright 2013-2023 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2023.4
 */
@RunWith(FeaturesRunner.class)
@Features(SmtpMailServerFeature.class)
public class TestSMTPMailSender {

    @Inject
    protected MailService mailService;

    @Inject
    protected SmtpMailServerFeature.MailsResult result;

    @Test
    public void testSendEmptyContentWithAttachment() {
        var foo = new StringBlob("bar");
        foo.setFilename("foo");
        var mail = new MailMessage.Builder("someone@nuxeo.com").attachments(foo).build();

        mailService.sendMail(mail);

        assertEquals(1, result.getSize());
        var received = result.getMails().get(0);
        assertEquals("noreply@nuxeo.com", received.getSenders().get(0));
        assertEquals("someone@nuxeo.com", received.getRecipients().get(0));
        assertTrue(received.content.contains(
                "Content-Type: text/plain; charset=us-ascii; name=fooContent-Transfer-Encoding: 7bitContent-Disposition: attachment; filename=foo"));
    }

}
