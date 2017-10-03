/*
 * (C) Copyright 2006-2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.automation.core.operations.notification;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class })
public class SendMailTest {

    protected static final String TOKEN = "ABC";

    private SendMail sendMail = new SendMail();

    @Test
    public void shouldReturnNullWhenDocUrlIsNull() {
        assertNull(sendMail.createDocUrlWithToken(null, null));
    }

    @Test
    public void shouldReturnDocUrlWhenTokenIsNull() {
        final String docUrl = "http://www.nuxeo.com";
        assertEquals(docUrl, sendMail.createDocUrlWithToken(docUrl, null));
    }

    @Test
    public void shouldPlaceTokenWhenUrlHasNoFragment() {
        final String docUrl = "http://www.nuxeo.com";
        assertEquals(docUrl + "?token=" + TOKEN, sendMail.createDocUrlWithToken(docUrl, TOKEN));
    }

    @Test
    public void shouldPlaceTokenWhenUrlHasFragment() {
        final String docUrl = "http://www.nuxeo.com/#/server";
        assertEquals("http://www.nuxeo.com/?token="+TOKEN+"#/server", sendMail.createDocUrlWithToken(docUrl, TOKEN));
    }
}
