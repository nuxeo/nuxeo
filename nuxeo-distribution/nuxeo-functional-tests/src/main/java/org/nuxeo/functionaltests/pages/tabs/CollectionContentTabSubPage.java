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
package org.nuxeo.functionaltests.pages.tabs;

import java.util.List;

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 5.9.3
 */
public class CollectionContentTabSubPage extends ContentTabSubPage {

    @Required
    @FindBy(id = "collection_content_contentview")
    WebElement documentContentForm;

    @FindBy(xpath = "//form[@id=\"collection_content_contentview\"]//tbody//tr")
    List<WebElement> childDocumentRows;

    public CollectionContentTabSubPage(final WebDriver driver) {
        super(driver);
    }

    @Override
    public List<WebElement> getChildDocumentRows() {
        return childDocumentRows;
    }

}
