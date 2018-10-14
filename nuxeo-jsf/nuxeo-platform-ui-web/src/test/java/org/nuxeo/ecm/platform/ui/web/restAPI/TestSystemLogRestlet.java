/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.ui.web.restAPI;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;

@Features(LogCaptureFeature.class)
public class TestSystemLogRestlet extends AbstractRestletTest {

    protected static final String ENDPOINT = "/systemLog";

    protected static final String TOKEN = "somesecret";

    @Inject
    protected LogCaptureFeature.Result logResult;

    @Before
    public void before() {
        Framework.getProperties().setProperty(SystemLogRestlet.TOKEN_PROP, TOKEN);
    }

    public void after() {
        Framework.getProperties().remove(SystemLogRestlet.TOKEN_PROP);
    }

    @Test
    @LogCaptureFeature.FilterOn(loggerClass = SystemLogRestlet.class, logLevel = "WARN")
    public void testLog() throws Exception {
        String path = ENDPOINT + "?level=warn&message=hello&token=" + TOKEN;
        executeRequestNoContent(path);
        logResult.assertHasEvent();
        assertEquals(Arrays.asList("hello"), logResult.getCaughtEventMessages());
    }

}
