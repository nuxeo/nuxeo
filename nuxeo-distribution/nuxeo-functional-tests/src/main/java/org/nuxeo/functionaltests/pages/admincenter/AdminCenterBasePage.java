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
 *     Thierry Delprat
 */
package org.nuxeo.functionaltests.pages.admincenter;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersGroupsBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class AdminCenterBasePage extends AbstractPage {

    @Required
    @FindBy(linkText = "Users & groups")
    public WebElement userAndGroupsLink;

    public static final String DOCUMENT_MANAGEMENT = "Document Management";

    public static final String SYSTEM_INFORMATION = "System information";

    public static final String UPDATE_CENTER = "Update Center";

    public static final String NUXEO_CONNECT = "Nuxeo Connect";

    public AdminCenterBasePage(WebDriver driver) {
        super(driver);
    }

    public AdminCenterBasePage nav(String linkText) {
        return nav(AdminCenterBasePage.class, linkText);
    }

    public <T extends AbstractPage> T nav(Class<T> pageClass, String linkText) {
        WebElement link = findElementWithTimeout(By.linkText(linkText));
        if (link == null) {
            return null;
        }
        link.click();
        return asPage(pageClass);
    }

    public UsersGroupsBasePage getUsersGroupsHomePage() {
        userAndGroupsLink.click();
        return asPage(UsersGroupsBasePage.class);
    }

    public ConnectHomePage getConnectHomePage() {
        return nav(ConnectHomePage.class, NUXEO_CONNECT);
    }

    public UpdateCenterPage getUpdateCenterHomePage() {
        return nav(UpdateCenterPage.class, UPDATE_CENTER);
    }

    public SystemHomePage getSystemHomePage() {
        return nav(SystemHomePage.class, SYSTEM_INFORMATION);
    }

    public String getSelectedSubTab() {
        WebElement tab = findElementWithTimeout(By.xpath("//div[@class='tabsBar']//li[@class='selected']/a"));
        if (tab != null) {
            return tab.getText();
        }
        return null;
    }

    public AdminCenterBasePage selectSubTab(String text) {
        WebElement tab = findElementWithTimeout(By.xpath("//div[@class='tabsBar']//li/a[text()='"
                + text + "']"));
        if (tab != null) {
            tab.click();
            return asPage(AdminCenterBasePage.class);

        }
        return null;
    }

    public List<String> getAvailableSubTabs() {
        List<WebElement> elements = driver.findElements(By.xpath("//div[@class='tabsBar']//li/a"));
        List<String> tabs = new ArrayList<String>();

        for (WebElement el : elements) {
            tabs.add(el.getText());
        }
        return tabs;
    }

    public DocumentBasePage exitAdminCenter() {
        findElementWithTimeout(By.linkText(DOCUMENT_MANAGEMENT)).click();
        return asPage(DocumentBasePage.class);
    }
}
