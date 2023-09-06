/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.mail;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(SmtpMailServerFeature.class)
public class TestSmtpMailServerFeature {

    @Inject
    protected MailService mailService;

    @Inject
    protected SmtpMailServerFeature.MailsResult result;

    @Test
    public void testFeature() {
        var mail = new MailMessage.Builder("someone@nuxeo.com").content("Some content").build();

        mailService.sendMail(mail);

        assertEquals(1, result.getSize());
    }

    @Test
    public void testFeatureResultIsolation() {
        // send a mail again and check we only have this one
        testFeature();
    }
}
