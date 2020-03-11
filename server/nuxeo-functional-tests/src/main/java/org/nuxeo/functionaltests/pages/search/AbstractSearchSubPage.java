/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Yannis JULIENNE
 */
package org.nuxeo.functionaltests.pages.search;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 6.0
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
        waitUntilEnabledAndClick(filterButton);
        return asPage(SearchPage.class);
    }

}
