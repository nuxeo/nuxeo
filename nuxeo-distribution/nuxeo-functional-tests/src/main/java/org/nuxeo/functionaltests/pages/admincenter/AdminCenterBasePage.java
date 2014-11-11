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
import org.nuxeo.functionaltests.pages.admincenter.monitoring.MonitoringPage;
import org.nuxeo.functionaltests.pages.admincenter.usermanagement.UsersGroupsBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class AdminCenterBasePage extends AbstractPage {

    // for ajax refresh wait until by adding required element
    @Required
    @FindBy(linkText = "System Information")
    public WebElement systemInformationLink;

    @FindBy(linkText = "Users & Groups")
    public WebElement userAndGroupsLink;

    @FindBy(linkText = "WORKSPACE")
    public WebElement documentManagementLink;

    @FindBy(linkText = "Update Center")
    public WebElement updateCenterLink;

    @FindBy(linkText = "Monitoring")
    public WebElement monitoringLink;

    @FindBy(linkText = "Nuxeo Connect")
    public WebElement nuxeoConnectLink;

    @FindBy(linkText = "Vocabularies")
    public WebElement vocabulariesLink;

    public AdminCenterBasePage(WebDriver driver) {
        super(driver);
    }

    public UsersGroupsBasePage getUsersGroupsHomePage() {
        userAndGroupsLink.click();
        return asPage(UsersGroupsBasePage.class);
    }

    public ConnectHomePage getConnectHomePage() {
        nuxeoConnectLink.click();
        return asPage(ConnectHomePage.class);
    }

    public UpdateCenterPage getUpdateCenterHomePage() {
        updateCenterLink.click();
        return asPage(UpdateCenterPage.class);
    }

    public MonitoringPage getMonitoringPage() {
        monitoringLink.click();
        return asPage(MonitoringPage.class);
    }

    public SystemHomePage getSystemHomePage() {
        systemInformationLink.click();
        return asPage(SystemHomePage.class);
    }

    public VocabulariesPage getVocabulariesPage() {
        vocabulariesLink.click();
        return asPage(VocabulariesPage.class);
    }

    public String getSelectedSubTab() {
        WebElement tab = findElementWithTimeout(By.xpath("//div[@id='nxw_adminCenterSubTabs_panel']//li[@class='selected']//a"));
        if (tab != null) {
            return tab.getText();
        }
        return null;
    }

    public AdminCenterBasePage selectSubTab(String text) {
        WebElement tab = findElementWithTimeout(By.xpath("//div[@id='nxw_adminCenterSubTabs_panel']//li/form/a/span[text()='"
                + text + "']"));
        if (tab != null) {
            tab.click();
            return asPage(AdminCenterBasePage.class);

        }
        return null;
    }

    public List<String> getAvailableSubTabs() {
        List<WebElement> elements = driver.findElements(By.xpath("//div[@id='nxw_adminCenterSubTabs_panel']//li/form/a/span"));
        List<String> tabs = new ArrayList<String>();

        for (WebElement el : elements) {
            tabs.add(el.getText());
        }
        return tabs;
    }

    public DocumentBasePage exitAdminCenter() {
        documentManagementLink.click();
        return asPage(DocumentBasePage.class);
    }
}
