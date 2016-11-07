/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Sun Seng David TAN
 *     Florent Guillaume
 *     Antoine Taillefer
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.nuxeo.ecm.platform.ui.web.auth.plugins.FormAuthenticator;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Tests login / logout.
 */
public class ITLoginLogoutTest extends AbstractTest {

    @Test
    public void testLoginLogout() throws UserNotConnectedException {
        // Login
        login();
        // Logout
        logout();
    }

    /**
     * Tests that only POST requests are accepted by form authentication. See
     * {@link FormAuthenticator#handleRetrieveIdentity(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
     *
     * @since 8.4
     */
    @Test
    public void testFormAuthentication() throws FailingHttpStatusCodeException, MalformedURLException, IOException {
        try (WebClient client = new WebClient()) {
            WebClientOptions options = client.getOptions();
            options.setJavaScriptEnabled(false);
            client.getPage(NUXEO_URL + "/logout");

            // POST
            HtmlPage page = client.getPage(new WebRequest(
                    new URL(NUXEO_URL + "/nxstartup.faces?user_name=Administrator&user_password=Administrator"),
                    HttpMethod.POST));
            // Expect successful authentication and redirection to the Domain page
            assertEquals(HttpServletResponse.SC_OK, page.getWebResponse().getStatusCode());
            assertEquals("Nuxeo Platform - Domain", page.getTitleText());
            client.getPage(NUXEO_URL + "/logout");

            // GET
            page = client.getPage(NUXEO_URL + "/nxstartup.faces?user_name=Administrator&user_password=Administrator");
            // Expect redirection to the login page
            assertEquals(HttpServletResponse.SC_OK, page.getWebResponse().getStatusCode());
            assertEquals("Nuxeo Platform", page.getTitleText());
            client.getPage(NUXEO_URL + "/logout");
        }

    }
}
