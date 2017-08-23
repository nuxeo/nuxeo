/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ftest.jsf.hotreload.studio;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.test.FakeSmtpMailServerFeature;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import static org.junit.Assert.fail;

/**
 * Tests hot reload of a Studio project on Nuxeo with JSF UI.
 *
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features({ FakeSmtpMailServerFeature.class })
public class ITDevJSFHotReloadTest extends NuxeoITCase {

    public static final String NUXEO_RELOAD_PATH = "/sdk/reload";

    protected final static Function<URL, URI> URI_MAPPER = url -> {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new NuxeoException("Unable to map the url to uri", e);
        }
    };

    @Before
    public void before() throws UserNotConnectedException {
        super.before();
        deployDevBundle();
    }

    @After
    public void after() {
        super.after();
        // reset dev.bundles file
        postToDevBundles("# AFTER TEST: removing studio_bundle");
    }

    protected void deployDevBundle() {
        // first lookup the absolute paths
        URL url = getClass().getResource("/studio_bundle");
        URI uri = URI_MAPPER.apply(url);
        String absolutePath = Paths.get(uri).toAbsolutePath().toString();
        postToDevBundles("Bundle:" + absolutePath);
    }

    protected void postToDevBundles(String line) {
        // post new dev bundles to deploy
        if (!RestHelper.post(NUXEO_RELOAD_PATH, line)) {
            fail("Unable to reload dev bundles, for line=" + line);
        }
    }

}
