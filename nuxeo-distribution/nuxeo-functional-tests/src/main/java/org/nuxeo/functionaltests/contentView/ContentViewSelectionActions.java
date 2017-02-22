/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.functionaltests.contentView;

import static org.junit.Assert.assertEquals;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.fragment.WebFragmentImpl;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * @since 9.1
 */
public class ContentViewSelectionActions extends WebFragmentImpl {

    public static final String DELETE = "Delete";

    public ContentViewSelectionActions(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    public DocumentBasePage delete() {
        return delete(DocumentBasePage.class);
    }

    public <T extends AbstractPage> T delete(Class<T> pageClassToProxy) {
        clickOnActionByTitle(DELETE);
        Alert alert = driver.switchTo().alert();
        assertEquals("Delete selected document(s)?", alert.getText());
        alert.accept();
        return AbstractTest.asPage(pageClassToProxy);
    }

    public void clickOnActionByTitle(String title) {
        Locator.waitUntilEnabledAndClick(getActionByTitle(title));
    }

    public <T extends AbstractPage> T clickOnActionByTitle(String title, Class<T> pageClassToProxy) {
        clickOnActionByTitle(title);
        return AbstractTest.asPage(pageClassToProxy);
    }

    public WebElement getActionByTitle(String title) {
        return getElement().findElement(By.xpath("//input[@value=\"" + title + "\"]"));
    }

}
