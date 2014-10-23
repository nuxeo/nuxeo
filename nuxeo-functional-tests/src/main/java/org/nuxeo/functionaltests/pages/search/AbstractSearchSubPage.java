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
package org.nuxeo.functionaltests.pages.search;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 5.9.6
 */
public abstract class AbstractSearchSubPage extends AbstractPage {

    @Required
    @FindBy(id = "nxl_gridSearchLayout:nxw_searchLayout_form:nxw_searchActions_clearSearch")
    public WebElement clearButton;

    @Required
    @FindBy(id = "nxl_gridSearchLayout:nxw_searchLayout_form:nxw_searchActions_submitSearch")
    public WebElement filterButton;

    public AbstractSearchSubPage(WebDriver driver) {
        super(driver);
    }

    public SearchPage filter() {
        filterButton.click();
        return asPage(SearchPage.class);
    }

}
