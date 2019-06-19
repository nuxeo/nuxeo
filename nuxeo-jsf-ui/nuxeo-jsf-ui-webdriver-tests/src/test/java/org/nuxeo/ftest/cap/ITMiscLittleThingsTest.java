/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.admincenter.AdminCenterBasePage;
import org.nuxeo.functionaltests.pages.admincenter.WorkflowsPage;
import org.nuxeo.functionaltests.pages.workflow.WorkflowGraph;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * ITTest class to test very simple things.
 *
 * @since 6.0
 */
public class ITMiscLittleThingsTest extends AbstractTest {

    public static final String NUXEO_URL = System.getProperty("nuxeoURL", "http://localhost:8080/nuxeo")
                                                 .replaceAll("/$", "");

    private static final String EXPECTED_HREF = NUXEO_URL
            + "/nxpath/default/default-domain/workspaces@view_documents?tabIds=%3A";

    private static final String EXPECTED_ONCLICK = "if(!(event.ctrlKey||event.shiftKey||event.metaKey||event.button==1)){this.href='"
            + NUXEO_URL.replaceAll("/", "\\\\/")
            + "\\/nxpath\\/default\\/default-domain\\/workspaces@view_documents?tabIds=%3A&conversationId=0NXMAIN'}";

    @Before
    public void setup() throws UserNotConnectedException {
        login();
    }

    @After
    public void tearDown() {
        logout();
    }

    @Test
    public void testDoubleClickShield() throws UserNotConnectedException {
        // Check that we have at least a form protected against double click
        List<WebElement> forms = driver.findElements(By.xpath("//form[contains(@class,'doubleClickShielded')]"));
        assertTrue(forms.size() > 0);
    }

    @Test
    public void testRequiredAsterisk() throws UserNotConnectedException {
        DocumentBasePage page = asPage(DocumentBasePage.class);
        page.getEditTab();
        String labelPattern = "//form[@id='document_edit']//tr[%s]//td[@class='labelColumn']//span";
        // title is required
        WebElement title = driver.findElement(By.xpath(String.format(labelPattern, 1)));
        assertEquals("Title", title.getText());
        String titleStyleClass = title.getAttribute("class");
        assertNotNull(titleStyleClass);
        assertTrue(titleStyleClass.contains("required"));
        String bgimage = title.getCssValue("background-image");
        assertNotNull(bgimage);
        assertTrue(bgimage.contains("required.gif"));
        // desc is not required
        WebElement desc = driver.findElement(By.xpath(String.format(labelPattern, 2)));
        assertEquals("Description", desc.getText());
        String descStyleClass = desc.getAttribute("class");
        assertNotNull(descStyleClass);
        assertFalse(descStyleClass.contains("required"));
        String descbgimage = desc.getCssValue("background-image");
        assertNotNull(descbgimage);
        assertFalse(descbgimage.contains("required.gif"));
    }

    @Test
    public void testRestDocumentLinkRenderer() throws UserNotConnectedException {
        // Check that rest document link will open new tab in a new conversation
        WebElement workspaces = driver.findElement(By.linkText("Workspaces"));
        String href = workspaces.getAttribute("href");
        String onclick = workspaces.getAttribute("onclick");
        assertEquals(EXPECTED_HREF, href);
        assertEquals(EXPECTED_ONCLICK, onclick);
    }

    /**
     * Test the existing workflow overview works in admin center.
     *
     * @since 7.1
     */
    @Test
    public void testWorkflowAdminOverview() {
        AdminCenterBasePage adminCenterBasePage = asPage(DocumentBasePage.class).getAdminCenter();
        WorkflowsPage workflowsPage = adminCenterBasePage.getWorkflowsPage();
        WorkflowGraph graph = workflowsPage.getParallelDocumentReviewGraph();
        assertEquals(1, graph.getWorkflowStartNodes().size());
        assertEquals(3, graph.getWorkflowEndNodes().size());

        workflowsPage = asPage(DocumentBasePage.class).getAdminCenter().getWorkflowsPage();
        graph = workflowsPage.getSerialDocumentReviewGraph();
        assertEquals(1, graph.getWorkflowStartNodes().size());
        assertEquals(1, graph.getWorkflowEndNodes().size());
    }

}
