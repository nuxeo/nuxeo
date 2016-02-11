/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ftest.cap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import static org.junit.Assert.assertEquals;

/**
 * Tests HTTP errors.
 * <p>
 * Use HtmlUnit for the error pages themselves to be able to get to the HTTP status code.
 */
public class ITErrorTest extends AbstractTest {

    @BeforeClass
    public static void before() {
        // Creates a user which is not in group "members", so doesn't have access to the default domain.
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD);
    }

    @AfterClass
    public static void after() {
        RestHelper.cleanup();
    }

    @Test
    public void testErrorPathNotFound() throws Exception {
        WebClient client = new WebClient();
        client.setJavaScriptEnabled(false);
        client.setThrowExceptionOnFailingStatusCode(false);
        client.getPage(NUXEO_URL + "/logout");
        client.getPage(NUXEO_URL + "/nxstartup.faces?user_name=Administrator&user_password=Administrator");
        HtmlPage page = client.getPage(NUXEO_URL + "/nxpath/default/nosuchdomain@view_documents");
        assertEquals(HttpServletResponse.SC_NOT_FOUND, page.getWebResponse().getStatusCode()); // 404
        assertEquals("An error occurred.", page.getTitleText());
        assertEquals("An error occurred.", page.getElementsByTagName("h1").get(0).getTextContent());
        client.getPage(NUXEO_URL + "/logout");
    }

    @Test
    public void testErrorPermissionDenied() throws Exception {
        WebClient client = new WebClient();
        client.setJavaScriptEnabled(false);
        client.setThrowExceptionOnFailingStatusCode(false);
        getAllPage(client, NUXEO_URL + "/logout");
        getAllPage(client,
                NUXEO_URL + "/nxstartup.faces?user_name=" + TEST_USERNAME + "&user_password=" + TEST_PASSWORD);
        HtmlPage page = client.getPage(NUXEO_URL + "/nxpath/default/default-domain@view_documents");
        assertEquals(page.getWebResponse().getContentAsString(), HttpServletResponse.SC_FORBIDDEN, page.getWebResponse().getStatusCode()); // 403
        assertEquals("Security Error", page.getTitleText());
        HtmlElement h1 = page.getElementsByTagName("h1").get(0);
        assertEquals("You don't have the necessary permission to do the requested action.", h1.getTextContent());
        client.getPage(NUXEO_URL + "/logout");
    }

    /** Fully reads the stream of a page. */
    protected void getAllPage(WebClient client, String url) throws IOException {
        HtmlPage page = client.getPage(url);
        try (InputStream in = page.getWebResponse().getContentAsStream(); //
                OutputStream out = new NullOutputStream()) {
            IOUtils.copy(in, out);
        }
    }

}
