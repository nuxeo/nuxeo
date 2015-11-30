/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
        assertTrue(driver.findElement(
                By.xpath("//div[@id='nxw_documentTabs_panel']//a/span[text()='Route']")).isDisplayed());
    }

}
