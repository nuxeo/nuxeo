/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
