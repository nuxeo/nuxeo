/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersGroupsBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersTabSubPage;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Tests HTTP errors.
 * <p>
 * Use HtmlUnit for the error pages themselves to be able to get to the HTTP status code.
 */
public class ITErrorTest extends AbstractTest {

    private static final String TEST_USERNAME = "jdoe_ITErrorTest";

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
        prepare();
        try {
            WebClient client = new WebClient();
            client.setJavaScriptEnabled(false);
            client.setThrowExceptionOnFailingStatusCode(false);
            client.getPage(NUXEO_URL + "/logout");
            client.getPage(
                    NUXEO_URL + "/nxstartup.faces?user_name=" + TEST_USERNAME + "&user_password=" + TEST_PASSWORD);
            HtmlPage page = client.getPage(NUXEO_URL + "/nxpath/default/default-domain@view_documents");
            assertEquals(HttpServletResponse.SC_FORBIDDEN, page.getWebResponse().getStatusCode()); // 403
            assertEquals("Security Error", page.getTitleText());
            HtmlElement h1 = page.getElementsByTagName("h1").get(0);
            assertEquals("You don't have the necessary permission to do the requested action.", h1.getTextContent());
            client.getPage(NUXEO_URL + "/logout");
        } finally {
            restore();
        }
    }

    /**
     * Creates a user which is not in group "members", so doesn't have access to the default domain.
     */
    protected void prepare() throws Exception {
        // create a test user if not already existing
        UsersTabSubPage usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = usersTab.searchUser(TEST_USERNAME);
        if (!usersTab.isUserFound(TEST_USERNAME)) {
            UsersGroupsBasePage page = usersTab.getUserCreatePage().createUser(TEST_USERNAME, TEST_USERNAME, null, null,
                    TEST_USERNAME + "@test.com", TEST_PASSWORD, null); // no group
            usersTab = page.getUsersTab(true);
        }
        logout();
    }

    protected void restore() throws Exception {
        UsersTabSubPage usersTab = login().getAdminCenter().getUsersGroupsHomePage().getUsersTab();
        usersTab = usersTab.searchUser(TEST_USERNAME);
        usersTab = usersTab.viewUser(TEST_USERNAME).deleteUser();
        logout();
    }

}
