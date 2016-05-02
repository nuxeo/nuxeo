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

import org.nuxeo.functionaltests.pages.profile.ProfilePage;
import org.nuxeo.functionaltests.pages.tabs.SummaryTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 5.7
 */
public class UserHomePage extends AbstractPage {

    private static final String ITEM_XPATH_BASE = ".//table[@class='dataOutput']/tbody/tr[td//text()[contains(.,'%s')]]";

    @FindBy(id = "nxw_dashboard_user_tasks")
    WebElement userTasks;

    @FindBy(id = "nxw_dashboard_user_documents")
    WebElement userDocuments;

    @FindBy(id = "nxw_dashboard_domain_documents")
    WebElement domainDocuments;

    public UserHomePage(WebDriver driver) {
        super(driver);
    }

    /**
     * @since 8.3
     */
    public boolean taskExistsOnUserTasks(String taskName) {
        try {
            return getTask(taskName).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public SummaryTabSubPage redirectToTask(String taskTitle) {
        userTasks.findElement(By.linkText(taskTitle)).click();
        return asPage(SummaryTabSubPage.class);
    }

    /**
     * @since 8.3
     */
    public boolean isUserTasksEmpty() {
        return userTasks.getText().contains("Your dashboard is empty");
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

    /**
     * @since 8.3
     */
    public boolean hasUserDocument(String docName) {
        try {
            return getUserDocument(docName) != null;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * @since 8.3
     */
    public boolean hasDomainDocument(String docName) {
        try {
            return getDomainDocument(docName) != null;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * @since 8.3
     */
    public DocumentBasePage goToDomainDocument(String docName) {
        getDomainDocument(docName).findElement(By.className("documentTitle")).click();
        return asPage(DocumentBasePage.class);
    }

    protected void goToTab(String id) {
        clickOnTabIfNotSelected("nxw_homeTabs_panel", id);
    }

    protected WebElement getTask(String taskName) {
        String xpath = String.format(ITEM_XPATH_BASE, taskName);
        return userTasks.findElement(By.xpath(xpath));
    }

    protected WebElement getUserDocument(String docName) {
        String xpath = String.format(ITEM_XPATH_BASE, docName);
        return userDocuments.findElement(By.xpath(xpath));
    }

    protected WebElement getDomainDocument(String docName) {
        String xpath = String.format(ITEM_XPATH_BASE, docName);
        return domainDocuments.findElement(By.xpath(xpath));
    }

}
