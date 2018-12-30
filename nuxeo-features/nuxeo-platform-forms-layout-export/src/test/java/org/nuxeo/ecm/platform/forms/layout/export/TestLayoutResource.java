/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.platform.forms.layout.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.webengine.test.WebEngineFeature;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.jaxrs.test.HttpClientTestRule;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;

/**
 * @since 10.1
 */
@RunWith(FeaturesRunner.class)
@Features({ WebEngineFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.forms.layout.core")
@Deploy("org.nuxeo.ecm.platform.forms.layout.export")
@Deploy("org.nuxeo.ecm.platform.forms.layout.client:OSGI-INF/layouts-framework.xml")
@Deploy("org.nuxeo.ecm.platform.forms.layout.export.tests:OSGI-INF/layouts-test-contrib.xml")
public class TestLayoutResource {

    @Inject
    protected ServletContainerFeature servletContainerFeature;

    // do not use @Rule because it's executed before runner injection
    public HttpClientTestRule httpClientRule;

    protected HttpClientTestRule getRule() {
        int port = servletContainerFeature.getPort();
        String url = "http://localhost:" + port + "/layout-manager/layouts";
        return new HttpClientTestRule.Builder().url(url).build();
    }

    @Before
    public void before() {
        httpClientRule = getRule();
        httpClientRule.starting();
    }

    @After
    public void after() {
        httpClientRule.finished();
    }

    @Test
    public void testSimpleGet() {
        try (CloseableClientResponse response = httpClientRule.get("?layoutName=testLayout")) {
            assertEquals(200, response.getStatus());
            // see NXP-24105
            String entity = response.getEntity(String.class);
            assertTrue("Response doesn't contain the widget part", entity.contains("?widgetType="));
        }
    }

}
