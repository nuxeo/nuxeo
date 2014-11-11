/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
    @FindBy(id="user_collection_contentview")
    protected WebElement collectionContentView;

    public CollectionsPage(final WebDriver driver) {
        super(driver);
    }

    public List<WebElement> getCollectionRows() {
        return driver.findElements(By.xpath("//form[@id='user_collection_contentview']/table/tbody/tr"));
    }

    public List<String> getCollectionNames() {
        List<String> result = new ArrayList<String>();
        for (WebElement collectionRow : getCollectionRows()) {
            result.add(collectionRow.findElement(By.xpath("td[3]")).getText());
        }
        return result;
    }

    public CollectionContentTabSubPage gotToCollection(final String collectionName) {
        Locator.findElementWithTimeout(By.linkText(collectionName), collectionContentView).click();
        return asPage(CollectionContentTabSubPage.class);
    }



}
