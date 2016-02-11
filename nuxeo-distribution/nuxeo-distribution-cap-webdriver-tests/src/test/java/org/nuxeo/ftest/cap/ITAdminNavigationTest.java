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
 *     Anahide Tchertchian
 */
package org.nuxeo.ftest.cap;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.admincenter.AdminCenterBasePage;
import org.nuxeo.functionaltests.pages.admincenter.WorkflowsPage;
import org.openqa.selenium.By;

/**
 * @since 8.1
 */
public class ITAdminNavigationTest extends AbstractTest {

    @Test
    public void testWorkflowView() throws UserNotConnectedException {
        DocumentBasePage page = login();
        AdminCenterBasePage adminPage = page.getAdminCenter();
        WorkflowsPage wp = adminPage.getWorkflowsPage();
        wp.getParallelDocumentReviewGraph();

        DocumentBasePage newPage = asPage(DocumentBasePage.class);
        assertTrue(newPage.isMainTabSelected(driver.findElement(By.linkText("ADMIN"))));

        // ensure that admin tabs are still visible
        AdminCenterBasePage newAdminPage = asPage(AdminCenterBasePage.class);
        assertTrue(newAdminPage.systemInformationLink.isDisplayed());
        assertTrue(newAdminPage.userAndGroupsLink.isDisplayed());
        assertTrue(newAdminPage.updateCenterLink.isDisplayed());
        assertTrue(newAdminPage.monitoringLink.isDisplayed());
        assertTrue(newAdminPage.nuxeoConnectLink.isDisplayed());
        assertTrue(newAdminPage.vocabulariesLink.isDisplayed());
        assertTrue(newAdminPage.worflowsLink.isDisplayed());
        assertTrue(newAdminPage.activityLink.isDisplayed());

        // ensure that document tabs are visible
        DocumentBasePage newDocPage = asPage(DocumentBasePage.class);
        assertTrue(newDocPage.editTabLink.isDisplayed());
        assertTrue(newDocPage.permissionsTabLink.isDisplayed());
        assertTrue(newDocPage.historyTabLink.isDisplayed());
        assertTrue(newDocPage.manageTabLink.isDisplayed());
        assertTrue(driver.findElement(By.xpath("//div[@id='nxw_documentTabs_panel']//a/span[text()='Route']"))
                         .isDisplayed());
    }

}
