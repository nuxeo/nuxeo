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

import java.io.IOException;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.web.Attachment;
import org.nuxeo.runtime.test.runner.web.Browser;
import org.nuxeo.runtime.test.runner.web.BrowserFamily;
import org.nuxeo.runtime.test.runner.web.Configuration;
import org.nuxeo.runtime.test.runner.web.HomePage;
import org.openqa.selenium.WebDriver;

@RunWith(FeaturesRunner.class)
@Features(WebEngineFeature.class)
@Browser(type = BrowserFamily.HTML_UNIT_JS)
@HomePage(type = WebEngineHomePage.class, url = "http://localhost:8082")
@Jetty(port = 8082)
@RepositoryConfig(init = WebEngineTest.MyInit.class)
public class WebEngineTest {

    public static class MyInit extends DefaultRepositoryInit {
        @Override
        public void populate(CoreSession session) {
            super.populate(session);
            // create a file
            DocumentModel doc = session.createDocumentModel("/default-domain/workspaces/test", "file", "File");
            StringBlob blob = new StringBlob("Content of file");
            blob.setFilename("file");
            blob.setMimeType("text/plain");
            blob.setEncoding("UTF-8");
            doc.setPropertyValue("file:content", blob);
            session.createDocument(doc);
        }
    }

    @Inject
    WebDriver driver;

    @Inject
    Configuration config;

    @Inject
    WebEngine engine;

    @Inject
    WebEngineHomePage home;

    @Before
    public void ensureAtHome() {
        if (config.getHome().equals(driver.getCurrentUrl())) {
            return;
        }
        driver.get(config.getHome());
    }

    @Before
    public void imLoggedIn() {
        LoginPage login = home.getLoginPage();
        if (!login.isAuthenticated()) {
            login.ensureLogin("Administrator", "Administrator");
        }
    }

    @Test
    public void iCanBrowseTheRepository() {
        Assertions.assertThat(home.hasModule("Admin")).isTrue();
        AdminModulePage admin = home.getModulePage("Admin", AdminModulePage.class);
        DocumentPage doc = admin.getDocumentPage("default-domain");
        Assertions.assertThat(doc.getTitle()).isEqualTo("Domain");
    }

    @Test
    public void iCanDownloadAttachment() throws IOException {
        AdminModulePage admin = home.getModulePage("Admin", AdminModulePage.class);
        DocumentPage file = admin.getDocumentPage("default-domain/workspaces/test/file");
        Attachment attachment = file.download("file");
        Assertions.assertThat(IOUtils.toString(attachment.getContent())).isEqualTo("Content of file");
    }

}
