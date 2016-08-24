/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.webengine.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.web.Browser;
import org.nuxeo.runtime.test.runner.web.BrowserFamily;
import org.nuxeo.runtime.test.runner.web.HomePage;

@RunWith(FeaturesRunner.class)
@Features(WebEngineFeature.class)
@Browser(type = BrowserFamily.HTML_UNIT_JS)
@HomePage(type = WebEngineHomePage.class, url = "http://localhost:8082")
@Jetty(port = 8082)
// @RepositoryConfig(type=BackendType.H2)
public class WebEngineTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected WebEngine we;

    @Inject
    private WebEngineHomePage home;

    @Test
    public void iCanRunWebEngine() {
        LoginPage login = home.getLoginPage();
        assertTrue(home.hasModule("Admin"));
        assertFalse(login.isAuthenticated());
        login.ensureLogin("Administrator", "Administrator");
        assertTrue(login.isAuthenticated());
        AdminModulePage admin = home.getModulePage("Admin", AdminModulePage.class);
        DocumentPage doc = admin.getDocumentPage("default-domain");
        Assertions.assertThat(doc.getTitle()).isEqualTo("Domain");
    }

}
