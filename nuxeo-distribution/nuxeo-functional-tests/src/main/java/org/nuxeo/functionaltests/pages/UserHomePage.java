/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Mariana Cedica
 */
package org.nuxeo.functionaltests.pages;

import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.fragment.GadgetsContainerFragment;
import org.nuxeo.functionaltests.pages.tabs.SummaryTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 5.7
 */
public class UserHomePage extends AbstractPage {

    // not required: when navigating to home, we could be on another tab than
    // dashboard
    @FindBy(id = GadgetsContainerFragment.GADGETS_CONTAINER_ID)
    public WebElement gadgetsContainer;

    protected GadgetsContainerFragment gadgetsFragment;

    public UserHomePage(WebDriver driver) {
        super(driver);
    }

    protected GadgetsContainerFragment getGadgetsFragment() {
        if (gadgetsFragment == null) {
            gadgetsFragment = getWebFragment(gadgetsContainer, GadgetsContainerFragment.class);
        }
        return gadgetsFragment;
    }

    public boolean isTaskGadgetLoaded() {
        return getGadgetsFragment().isGadgetLoaded("My Tasks");
    }

    public SummaryTabSubPage redirectToTask(String taskTitle) {
        WebDriver driver = getGadgetsFragment().switchToFrame("My Tasks");
        driver.findElement(By.linkText(taskTitle)).click();
        return new SummaryTabSubPage(driver);
    }

    public boolean isTaskGadgetEmpty() {
        return getGadgetsFragment().isTaskGadgetEmpty("My Tasks");
    }

    /**
     * @since 5.8
     */
    public WorkflowHomePage getWorkflowHomePage() {
        goToTab("nxw_WorkflowHome");
        return asPage(WorkflowHomePage.class);
    }

    public UserHomePage goToDashboard() {
        goToTab("nxw_Dashboard");
        return this;
    }

    public ProfilePage goToProfile() {
        goToTab("nxw_Profile");
        return asPage(ProfilePage.class);
    }

    protected void goToTab(String id) {
        clickOnTabIfNotSelected("nxw_homeTabs_panel", id);
    }

}