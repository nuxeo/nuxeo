/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN <stan@nuxeo.com>
 *     Antoine Taillefer
 */
package org.nuxeo.functionaltests.pages;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.actions.ContextualActions;
import org.nuxeo.functionaltests.pages.admincenter.AdminCenterBasePage;
import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.EditTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.HistoryTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.ManageTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.RelationTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.SummaryTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.WorkflowTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.WorkspacesContentTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * The nuxeo main document base page
 *
 * @author Sun Seng David TAN <stan@nuxeo.com>
 */
public class DocumentBasePage extends AbstractPage {

    /**
     * Exception occurred a user is expected to be connected but it isn't.
     *
     */
    public class UserNotConnectedException extends Exception {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public UserNotConnectedException(String username) {
            super("The user " + username
                    + " is expected to be connected but isn't");
        }
    }

    @FindBy(xpath = "//form[@id='breadcrumbForm']")
    public WebElement breadcrumbForm;

    @FindBy(xpath = "//div[@id='nxw_documentTabs_panel']//a/span[text()='Content']")
    public WebElement contentTabLink;

    public ContextualActions contextualActions;

    @FindBy(className = "creator")
    public WebElement currentDocumentContributor;

    @FindBy(className = "documentDescription")
    public WebElement currentDocumentDescription;

    @FindBy(xpath = "/html/body/table[2]/tbody/tr/td[2]/div[2]//h1")
    public WebElement currentDocumentTitle;

    @FindBy(className = "currentDocumentDescription")
    public WebElement currentFolderishDescription;

    @FindBy(linkText = "DOCUMENT MANAGEMENT")
    public WebElement documentManagementLink;

    @FindBy(xpath = "//div[@id='nxw_documentTabs_panel']//a/span[text()='Edit']")
    public WebElement editTabLink;

    @FindBy(xpath = "//div[@id='nxw_documentTabs_panel']//a/span[text()='History']")
    public WebElement historyTabLink;

    @FindBy(xpath = "//div[@id='nxw_documentTabs_panel']//a/span[text()='Manage']")
    public WebElement manageTabLink;

    @FindBy(xpath = "//div[@id='nxw_documentTabs_panel']//a/span[text()='Relations']")
    public WebElement relationTabLink;

    @FindBy(xpath = "//div[@id='nxw_documentTabs_panel']//a/span[text()='Summary']")
    public WebElement summaryTabLink;

    @Required
    @FindBy(xpath = "//div[@id='nxw_documentTabs_panel']")
    public WebElement tabsBar;

    @FindBy(linkText = "Workflow")
    public WebElement workflowLink;

    public DocumentBasePage(WebDriver driver) {
        super(driver);
    }

    /**
     * Check if the title of the current document page is equal to the
     * {@code expectedTitle}.
     *
     * @param expectedTitle the expected title
     */
    public void checkDocTitle(String expectedTitle) {
        assertEquals(expectedTitle, getCurrentDocumentTitle());
    }

    /**
     * Check if the user is connected by looking for the text: You are logged as
     * Username
     *
     * @param username
     * @throws UserNotConnectedException
     */
    public void checkUserConnected(String username)
            throws UserNotConnectedException {
        if (!(getHeaderLinks().getText().contains(username))) {
            throw new UserNotConnectedException(username);
        }
    }

    protected void clickOnLinkIfNotSelected(WebElement tabLink) {
        WebElement selectedTab = findElementWithTimeout(By.xpath("//div[@id='nxw_documentTabs_panel']//li[@class='selected']/a/span"));
        if (!selectedTab.equals(tabLink)) {
            tabLink.click();
        }
    }

    public AdminCenterBasePage getAdminCenter() {
        findElementWithTimeout(By.linkText("ADMIN CENTER")).click();
        return asPage(AdminCenterBasePage.class);
    }

    /**
     * Click on the content tab and return the subpage of this page.
     *
     */
    public ContentTabSubPage getContentTab() {
        clickOnLinkIfNotSelected(contentTabLink);
        return asPage(ContentTabSubPage.class);
    }

    public ContextualActions getContextualActions() {
        return asPage(ContextualActions.class);
    }

    public String getCurrentContributors() {
        return currentDocumentContributor.getText();
    }

    public String getCurrentDocumentDescription() {
        return currentDocumentDescription.getText();
    }

    public String getCurrentDocumentTitle() {
        return currentDocumentTitle.getText();
    }

    public String getCurrentFolderishDescription() {
        return currentFolderishDescription.getText();
    }

    public List<String> getCurrentStates() {
        List<WebElement> states = findElementsWithTimeout(By.className("sticker"));
        List<String> stateLabels = new ArrayList<String>();
        for (WebElement state : states) {
            stateLabels.add(state.getText());
        }
        return stateLabels;
    }

    public DocumentBasePage getDocumentManagement() {
        documentManagementLink.click();
        return asPage(DocumentBasePage.class);
    }

    public EditTabSubPage getEditTab() {
        clickOnLinkIfNotSelected(editTabLink);
        return asPage(EditTabSubPage.class);
    }

    public HistoryTabSubPage getHistoryTab() {
        clickOnLinkIfNotSelected(historyTabLink);
        return asPage(HistoryTabSubPage.class);
    }

    public ManageTabSubPage getManageTab() {
        clickOnLinkIfNotSelected(manageTabLink);
        return asPage(ManageTabSubPage.class);
    }

    public NavigationSubPage getNavigationSubPage() {
        return asPage(NavigationSubPage.class);
    }

    public RelationTabSubPage getRelationTab() {
        clickOnLinkIfNotSelected(relationTabLink);
        return asPage(RelationTabSubPage.class);
    }

    public SummaryTabSubPage getSummaryTab() {
        clickOnLinkIfNotSelected(summaryTabLink);
        return asPage(SummaryTabSubPage.class);
    }

    /**
     * @since 5.7
     */
    public UserHomePage getUserHome() {
        findElementWithTimeout(By.linkText("HOME")).click();
        UserHomePage page = asPage(UserHomePage.class);
        // make sure we're back on the dashboard tab
        return page.goToDashboard();
    }

    public WorkflowTabSubPage getWorkflow() {
        workflowLink.click();
        return asPage(WorkflowTabSubPage.class);
    }

    /**
     * For workspace type, the content tab is a bit different.
     *
     */
    public WorkspacesContentTabSubPage getWorkspacesContentTab() {
        clickOnLinkIfNotSelected(contentTabLink);
        return asPage(WorkspacesContentTabSubPage.class);
    }

    public DocumentBasePage goToDocumentByBreadcrumb(String documentTitle) {
        breadcrumbForm.findElement(By.linkText(documentTitle)).click();
        return asPage(DocumentBasePage.class);
    }
}
