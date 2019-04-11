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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.functionaltests.pages;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.tabs.CollectionContentTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 5.9.3
 */
public class CollectionsPage extends HomePage {

    @Required
    @FindBy(id = "user_collection_contentview")
    protected WebElement collectionContentView;

    public CollectionsPage(final WebDriver driver) {
        super(driver);
    }

    public List<WebElement> getCollectionRows() {
        return driver.findElements(By.xpath("//form[@id='user_collection_contentview']/table/tbody/tr"));
    }

    public List<String> getCollectionNames() {
        List<String> result = new ArrayList<>();
        for (WebElement collectionRow : getCollectionRows()) {
            result.add(collectionRow.findElement(By.xpath("td[3]")).getText());
        }
        return result;
    }

    public CollectionContentTabSubPage goToCollection(final String collectionName) {
        Locator.findElementWithTimeout(By.linkText(collectionName), collectionContentView).click();
        return asPage(CollectionContentTabSubPage.class);
    }

}
