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

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 *
 * @since 5.9.6
 */
public class SearchPage extends DocumentBasePage {

    @FindBy(id= "nxl_gridSearchLayout:nxw_searchForm_panel")
    @Required
    protected WebElement searchFormPanel;

    @FindBy(id= "nxl_gridSearchLayout:nxw_searchResults_panel")
    @Required
    protected WebElement searchResultPanel;

    public SearchPage(WebDriver driver) {
        super(driver);
    }

    public SearchLayoutSubPage getSearchLayoutSubPage() {
        return asPage(SearchLayoutSubPage.class);
    }

}
