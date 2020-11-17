/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.functionaltests.pages;

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author Sun Seng David TAN
 */
public class NavigationSubPage extends AbstractPage {

    @Required
    @FindBy(xpath = "//dd[@class=\"menuForm\"]")
    WebElement navigationTree;

    public NavigationSubPage(WebDriver driver) {
        super(driver);
    }

    public DocumentBasePage goToDocument(String docTitle) {
        navigationTree.findElement(By.linkText(docTitle)).click();
        // wait for page load after click
        findElementWithTimeout(By.className("userMenuActions"));
        return asPage(DocumentBasePage.class);
    }

    /**
     * @since 5.9.4
     */
    public boolean canNavigateToDocument(final String docTitle) {
        try {
            navigationTree.findElement(By.linkText(docTitle));
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

}
