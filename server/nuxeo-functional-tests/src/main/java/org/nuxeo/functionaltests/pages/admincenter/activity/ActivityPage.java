/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.functionaltests.pages.admincenter.activity;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.admincenter.AdminCenterBasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 7.10
 */
public class ActivityPage extends AdminCenterBasePage {

    @Required
    @FindBy(linkText = "Repository Analytics")
    WebElement repositoryAnalytics;

    @Required
    @FindBy(linkText = "Search Analytics")
    WebElement searchAnalytics;

    public ActivityPage(WebDriver driver) {
        super(driver);
    }

    public RepositoryAnalyticsPage getRepositoryAnalyticsPage() {
        repositoryAnalytics.click();
        return asPage(RepositoryAnalyticsPage.class);
    }

    public SearchAnalyticsPage getSearchAnalyticsPage() {
        searchAnalytics.click();
        return asPage(SearchAnalyticsPage.class);
    }
}
