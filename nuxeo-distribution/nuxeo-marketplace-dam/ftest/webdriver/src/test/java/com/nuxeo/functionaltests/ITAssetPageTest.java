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
 *     Thomas Roger
 */

package com.nuxeo.functionaltests;

import org.junit.Test;
import org.nuxeo.functionaltests.dam.AssetViewFragment;
import org.nuxeo.functionaltests.dam.DAMAssetPage;
import org.nuxeo.functionaltests.dam.DAMPage;
import org.nuxeo.functionaltests.dam.SearchResultsFragment;
import org.nuxeo.functionaltests.fragment.WebFragment;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;

/**
 * @since 5.7.3
 */
public class ITAssetPageTest extends AbstractDAMTest {

    @Test
    public void navigateToDM() throws Exception {
        login("leela", "test");

        DAMPage damPage = getDAMPage();
        damPage = damPage.createAsset("File", "One File",
                "One File description", "Leela", "1/1/2012");
        damPage = damPage.createAsset("File", "Another File",
                "Another File description", "Leela", "1/1/2012");

        SearchResultsFragment searchResultsFragment = damPage.getSearchResultsFragment();
        searchResultsFragment.selectAsset("One File");

        AssetViewFragment assetViewFragment = damPage.getAssetViewFragment();
        assetViewFragment.clickOnAction("Document Management view");

        DocumentBasePage page = asPage(DocumentBasePage.class);
        page.checkDocTitle("One File");

        damPage = getDAMPage();
        searchResultsFragment = damPage.getSearchResultsFragment();
        searchResultsFragment.selectAsset("Another File");

        assetViewFragment = damPage.getAssetViewFragment();
        assetViewFragment.clickOnAction("Document Management view");

        page = asPage(DocumentBasePage.class);
        page.checkDocTitle("Another File");
    }

    @Test
    public void testAssetPage() throws Exception {
        login("leela", "test");

        DAMPage damPage = getDAMPage();
        damPage = damPage.createAsset("File", "One File",
                "One File description", "Leela", "1/1/2012");

        AssetViewFragment assetViewFragment = damPage.getAssetViewFragment();
        assetViewFragment.clickOnAction("Document Management view");

        DocumentBasePage page = asPage(DocumentBasePage.class);
        page.checkDocTitle("One File");

        page.getContextualActions().moreButton.click();
        driver.findElement(By.xpath("//img[@alt=\"DAM view\"]")).click();

        DAMAssetPage assetPage = getDAMassetPage();
        assetPage.checkAssetTitle("One File");
        assetPage.getBackToDAMLink().click();

        damPage = getDAMPage();
        SearchResultsFragment searchResultsFragment = damPage.getSearchResultsFragment();
        WebFragment selectedAsset = searchResultsFragment.getSelectedAsset();
        selectedAsset.checkTextToBePresent("One File");
    }
}
