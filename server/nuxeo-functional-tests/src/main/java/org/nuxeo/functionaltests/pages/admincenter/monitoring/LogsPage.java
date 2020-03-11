/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.functionaltests.pages.admincenter.monitoring;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * @since 6.0
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
                break;
            }
        }
        return asPage(LogsPage.class);
    }

    public WebElement getDownloadButton(String serverLogFileName) {
        String downloadButtonValue = "Download " + serverLogFileName;
        return findElementWithTimeout(By.xpath("//input[@value='" + downloadButtonValue + "']"));
    }

}
