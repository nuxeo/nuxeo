/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 *     Thomas Roger
 */

package org.nuxeo.functionaltests.dam;

import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * The Nuxeo DAM base page
 *
 * @since 5.7.3
 */
public class DAMPage extends DocumentBasePage {

    public DAMPage(WebDriver driver) {
        super(driver);
    }

    public SearchFormFragment getSearchFormFragment() {
        return getWebFragment(By.className("nxDamSearchForm"),
                SearchFormFragment.class);
    }

    public SearchResultsFragment getSearchResultsFragment() {
        return getWebFragment(By.className("nxDamSearchResults"),
                SearchResultsFragment.class);
    }

    public AssetViewFragment getAssetViewFragment() {
        return getWebFragment(By.className("nxDamAssetView"),
                AssetViewFragment.class);
    }

    public DAMPage createAsset(String type, String title, String description,
            String originalAuthor, String authoringDate) {
        return getSearchResultsFragment().createAsset(this, type, title,
                description, originalAuthor, authoringDate);
    }

}
