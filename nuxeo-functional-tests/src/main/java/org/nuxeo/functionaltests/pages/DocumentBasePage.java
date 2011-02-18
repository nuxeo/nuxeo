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

import org.nuxeo.functionaltests.pages.tabs.ContentTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.HeaderLinksSubPage;
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

    @FindBy(xpath = "/html/body/table[2]/tbody/tr/td[2]/div[2]//div[@class=\"tabsBar\"]/form/ul/li[@class=\"selected\"]/a")
    public WebElement selectedTab;

    @FindBy(name = "userServicesForm")
    public WebElement userServicesForm;

    public DocumentBasePage(WebDriver driver) {
        super(driver);
    }

    /**
     * Click on the content tab and return the subpage of this page.
     *
     * @return
     */
    public ContentTabSubPage getContentTab() {
        assertNotNull(contentTabLink);
        assertNotNull(selectedTab);

        if (!selectedTab.equals(contentTabLink)) {
            contentTabLink.click();
        }
        return asPage(ContentTabSubPage.class);
    }

    /**
     * For workspace type, the content tab is a bit different.
     *
     * @return
     */
    public WorkspaceContentTabSubPage getWorkspaceContentTab() {
        assertNotNull(contentTabLink);
        assertNotNull(selectedTab);

        if (!selectedTab.equals(contentTabLink)) {
            contentTabLink.click();
        }

        return asPage(WorkspaceContentTabSubPage.class);
    }

    /**
     * Get the top bar navigation sub page.
     *
     * @return
     */
    public HeaderLinksSubPage getHeaderLinks() {
        assertNotNull(userServicesForm);

        return asPage(HeaderLinksSubPage.class);
    }
}
