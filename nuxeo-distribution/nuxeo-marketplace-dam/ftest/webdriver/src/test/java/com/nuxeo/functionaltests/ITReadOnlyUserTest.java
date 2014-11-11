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

import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.nuxeo.functionaltests.dam.AssetCreationFancyBoxFragment;
import org.nuxeo.functionaltests.dam.DAMPage;
import org.nuxeo.functionaltests.dam.SearchResultsFragment;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * @since 5.7.3
 */
public class ITReadOnlyUserTest extends AbstractDAMTest {

    @Test
    public void testReadOnlyUser() throws Exception {
        login("leela", "test");
        DAMPage damPage = getDAMPage();
        damPage = damPage.createAsset("File", "One Document",
                "One File description", "Leela", "1/1/2012");
        damPage = damPage.createAsset("File", "Another Document",
                "Another File description", "Fry", "1/1/2012");
        damPage.createAsset("File", "Sample picture", "This is a sample doc",
                "Bender", "1/2/2012");
        logout();

        login("bender", "test");
        // make sure navigation to a given domain is done, see NXP-13436
        findElementWithTimeout(By.linkText("DOCUMENT MANAGEMENT")).click();
        findElementWithTimeout(By.linkText("Default domain")).click();
        damPage = getDAMPage();
        SearchResultsFragment searchResultsFragment = damPage.getSearchResultsFragment();
        // Asset Library is not selected as the user does not have Write right
        // on it
        AssetCreationFancyBoxFragment assetCreation = searchResultsFragment.showAssetCreation(damPage);
        assetCreation.checkTextToBeNotPresent("Asset Library");
        assetCreation.cancel();

        // cannot bulk edit, but can add to worklist
        String bEditButtonId = "nxl_gridDamLayout:dam_search_nxw_searchResults_buttons:nxw_damBulkEdit_form:nxw_cvButton_damBulkEdit_subview:nxw_cvButton_damBulkEdit_link";
        String addToWLButtonId = "nxl_gridDamLayout:dam_search_nxw_searchResults_buttons:nxw_CURRENT_SELECTION_ADDTOLIST_form:nxw_CURRENT_SELECTION_ADDTOLIST";

        searchResultsFragment.selectAll();
        WebElement bulkEditButton = driver.findElement(By.id(bEditButtonId));
        assertFalse(bulkEditButton.isEnabled());

        // make sure ajax request is done, otherwise we might get stack traces
        findElementAndWaitUntilEnabled(By.id(addToWLButtonId));

        // cannot edit any metadata on the asset view
        assertFalse(damPage.hasElement(By.xpath("//div[contains(@class, 'foldableBox')]//a[text()='Edit']")));
        logout();
    }
}
