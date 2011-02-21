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
 */
package org.nuxeo.functionaltests.pages;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.EditTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.SummaryTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.WorkspaceContentTabSubPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * The nuxeo main document base page
 *
 * @author Sun Seng David TAN <stan@nuxeo.com>
 *
 */
public class DocumentBasePage extends AbstractPage {

    @FindBy(xpath = "//div[@class=\"tabsBar\"]/form/ul/li/a[text()=\"Content\"]")
    public WebElement contentTabLink;

    @FindBy(xpath = "//div[@class=\"tabsBar\"]/form/ul/li/a[text()=\"Summary\"]")
    public WebElement summaryTabLink;

    @FindBy(xpath = "//div[@class=\"tabsBar\"]/form/ul/li/a[text()=\"Edit\"]")
    public WebElement editTabLink;

    @FindBy(xpath = "/html/body/table[2]/tbody/tr/td[2]/div[2]//div[@class=\"tabsBar\"]/form/ul/li[@class=\"selected\"]/a")
    public WebElement selectedTab;

    @FindBy(xpath = "//div[@class=\"userActions\"]")
    public WebElement userActions;

    @FindBy(className = "currentDocumentDescription")
    public WebElement currentDocumentDescription;

    @FindBy(xpath = "/html/body/table[2]/tbody/tr/td[2]/div[2]//h1")
    public WebElement currentDocumentTitle;

    public DocumentBasePage(WebDriver driver) {
        super(driver);
    }

    /**
     * Click on the content tab and return the subpage of this page.
     *
     * @return
     */
    public ContentTabSubPage getContentTab() {
        clickOnLinkIfNotSelected(contentTabLink);
        return asPage(ContentTabSubPage.class);
    }

    public EditTabSubPage getEditTab() {
        clickOnLinkIfNotSelected(editTabLink);
        return asPage(EditTabSubPage.class);
    }

    public SummaryTabSubPage getSummaryTab() {
        clickOnLinkIfNotSelected(editTabLink);
        return asPage(SummaryTabSubPage.class);
    }

    public NavigationSubPage getNavigationSubPage() {
        return asPage(NavigationSubPage.class);
    }

    protected void clickOnLinkIfNotSelected(WebElement tabLink) {
        if (!selectedTab.equals(tabLink)) {
            tabLink.click();
        }
    }

    /**
     * For workspace type, the content tab is a bit different.
     *
     * @return
     */
    public WorkspaceContentTabSubPage getWorkspaceContentTab() {
        clickOnLinkIfNotSelected(contentTabLink);
        return asPage(WorkspaceContentTabSubPage.class);
    }

    /**
     * Check if the user is connected by looking for the text: You are logged as
     * Username
     *
     * @param username
     */
    public void checkUserConnected(String username) {
        String expectedConnectedMessage = "You are logged as " + username;
        assertTrue(userActions.getText().contains(expectedConnectedMessage));
    }

    public String getCurrentDocumentDescription() {
        return currentDocumentDescription.getText();
    }

    public String getCurrentDocumentTitle() {
        return currentDocumentTitle.getText();
    }

}
