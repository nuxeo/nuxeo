/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.webengine.test;

import static org.junit.Assert.assertFalse;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;
import org.nuxeo.runtime.test.runner.web.Browser;
import org.nuxeo.runtime.test.runner.web.BrowserFamily;
import org.nuxeo.runtime.test.runner.web.HomePage;

@RunWith(FeaturesRunner.class)
@Features(WebEngineFeature.class)
@Browser(type = BrowserFamily.HTML_UNIT)
@HomePage(type = WebEngineHomePage.class, url = "http://localhost:8082")
@ServletContainer(port = 8082)
public class ICanLoginInWebEngineWithoutJavascriptTest {

    @Inject
    WebEngine engine;

    @Inject
    WebEngineHomePage home;

    @Test
    public void iCanLogAdministrator() {
        LoginPage login = home.getLoginPage();
        assertFalse(login.isAuthenticated());
        login.ensureLogin("Administrator", "Administrator");
    }

}
