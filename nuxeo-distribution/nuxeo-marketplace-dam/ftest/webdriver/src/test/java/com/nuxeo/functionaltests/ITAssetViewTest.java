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

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.nuxeo.functionaltests.dam.AssetViewFragment;
import org.nuxeo.functionaltests.dam.DAMPage;
import org.nuxeo.functionaltests.dam.FoldableBoxFragment;
import org.nuxeo.functionaltests.dam.SearchResultsFragment;
import org.nuxeo.functionaltests.forms.LayoutElement;
import org.nuxeo.functionaltests.fragment.WebFragment;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import com.google.common.base.Function;

/**
 * @since 5.7.3
 */
public class ITAssetViewTest extends AbstractDAMTest {

    private static final Log log = LogFactory.getLog(ITAssetViewTest.class);

    protected static final String FILE_MODIFIED_NOTIFICATION_LABEL = "File modified";

    @Test
    public void testAssetSelection() throws Exception {
        login("leela", "test");

        DAMPage damPage = getDAMPage();
        damPage = damPage.createAsset("File", "One File",
                "One File description", "Leela", "1/1/2012");
        damPage = damPage.createAsset("File", "Another File",
                "Another File description", "Leela", "1/1/2012");

        SearchResultsFragment searchResultsFragment = damPage.getSearchResultsFragment();
        searchResultsFragment.checkTextToBePresent("One File");
        searchResultsFragment.checkTextToBePresent("Another File");

        AssetViewFragment assetViewFragment = damPage.getAssetViewFragment();
        assetViewFragment.checkTextToBePresent("One File");
        assetViewFragment.checkTextToBeNotPresent("Another File");
        assetViewFragment.checkAssetTitle("One File");

        searchResultsFragment.selectAsset("Another File");
        assetViewFragment.waitForTextToBePresent("Another File");

        assetViewFragment.checkTextToBeNotPresent("One File");
        assetViewFragment.checkTextToBePresent("Another File");
        assetViewFragment.checkAssetTitle("Another File");
        logout();
    }

    @Test
    public void testMetadataEdition() throws Exception {
        login("leela", "test");

        DAMPage damPage = getDAMPage();
        damPage = damPage.createAsset("File", "One File",
                "One File description", "Leela", "1/1/2012");
        damPage = damPage.createAsset("File", "Another File",
                "Another File description", "Leela", "1/1/2012");

        AssetViewFragment assetViewFragment = damPage.getAssetViewFragment();
        FoldableBoxFragment metadataBox = assetViewFragment.getFoldableBox(
                "Metadata", false);
        metadataBox.open();
        metadataBox.checkTextToBeNotPresent("New title");
        metadataBox.checkTextToBeNotPresent("New description");
        metadataBox.edit();

        LayoutElement layout = new LayoutElement(driver,
                "nxl_gridDamLayout:nxw_damAssetViewMetadata_toggledForm");
        layout.getWidget("nxl_heading:nxw_title").setInputValue("New title");
        layout.getWidget("nxl_heading:nxw_description").setInputValue(
                "New description");
        layout.getWidget("nxl_dam_common_2:nxw_damc_author_3").setInputValue(
                "New author");
        layout.getWidget("nxl_dam_common_2:nxw_damc_authoringDate_3InputDate").setInputValue(
                "10/10/2010");
        metadataBox.save();

        damPage = asPage(DAMPage.class);
        assetViewFragment = damPage.getAssetViewFragment();
        metadataBox = assetViewFragment.getFoldableBox("Metadata", false);
        metadataBox.open();
        metadataBox.checkTextToBePresent("New title");
        metadataBox.checkTextToBePresent("New description");

        SearchResultsFragment searchResultsFragment = damPage.getSearchResultsFragment();
        WebFragment bubbleBox = searchResultsFragment.getBubbleBox("New title");
        bubbleBox.checkTextToBePresent("New title");
        bubbleBox.checkTextToBePresent("New author");
        bubbleBox.checkTextToBePresent("10/10/2010");

        logout();
    }

    @Test
    public void testIntellectualProperty() throws Exception {
        login("leela", "test");

        DAMPage damPage = getDAMPage();
        damPage = damPage.createAsset("File", "One File",
                "One File description", "Leela", "1/1/2012");

        AssetViewFragment assetViewFragment = damPage.getAssetViewFragment();
        FoldableBoxFragment ipBox = assetViewFragment.getFoldableBox(
                "Intellectual Property", true);
        ipBox.checkTextToBeNotPresent("New holder");
        ipBox.edit();

        WebElement copyrightHolderInput = driver.findElement(By.id("nxl_gridDamLayout:nxw_damAssetViewIpRights_toggledForm:nxl_ip_rights:nxw_copyright_holder"));
        copyrightHolderInput.clear();
        copyrightHolderInput.sendKeys("New holder");
        ipBox.save();

        Wait<WebDriver> wait = new FluentWait<WebDriver>(driver).withTimeout(5,
                TimeUnit.SECONDS).pollingEvery(100, TimeUnit.MILLISECONDS).ignoring(
                NoSuchElementException.class);

        try {
            wait.until(new Function<WebDriver, WebElement>() {
                public WebElement apply(WebDriver driver) {
                    return driver.findElement(By.xpath("//div[contains(.,'"
                            + FILE_MODIFIED_NOTIFICATION_LABEL + "')]"));
                }
            });
        } catch (TimeoutException e) {
            log.warn("Could not see saved message, maybe I was too slow and it "
                    + "has already disappeared. Let's see if I can restore.");
        }

        ipBox.open();
        ipBox.waitForTextToBePresent("New holder");

        logout();
    }
}
