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
