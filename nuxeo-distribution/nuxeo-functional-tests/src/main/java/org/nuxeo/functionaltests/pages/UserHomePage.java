/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Mariana Cedica
 */
package org.nuxeo.functionaltests.pages;

import org.nuxeo.functionaltests.fragment.GadgetsContainerFragment;
import org.nuxeo.functionaltests.pages.profile.ProfilePage;
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
