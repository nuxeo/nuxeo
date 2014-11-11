/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.functionaltests.pages.admincenter.monitoring;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * @since 5.9.6
 */
public class LogsPage extends MonitoringPage {

    public LogsPage(WebDriver driver) {
        super(driver);
    }

    public List<String> getServerLogFileNames() {
        List<WebElement> serverLogTabs = getServerLogTabs();
        List<String> fileNames = new ArrayList<>();
        for (WebElement element : serverLogTabs) {
            fileNames.add(element.getText());
        }
        return fileNames;
    }

    protected List<WebElement> getServerLogTabs() {
        return findElementsWithTimeout(By.xpath("//div[@class='tabsBar subtabsBar']//li"));
    }

    public LogsPage selectServerLogFileTab(String serverLogFileName) {
        List<WebElement> serverLogTabs = getServerLogTabs();
        for (WebElement element : serverLogTabs) {
            if (serverLogFileName.equals(element.getText())) {
                element.click();
            }
        }
        return asPage(LogsPage.class);
    }

    public WebElement getDownloadButton(String serverLogFileName) {
        String downloadButtonValue = "Download " + serverLogFileName;
        return findElementWithTimeout(By.xpath("//input[@value='"
                + downloadButtonValue + "']"));
    }

}
