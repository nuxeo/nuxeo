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

package org.nuxeo.functionaltests.dam;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.forms.LayoutElement;
import org.nuxeo.functionaltests.fragment.WebFragment;
import org.nuxeo.functionaltests.fragment.WebFragmentImpl;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import com.google.common.base.Function;

/**
 * @since 5.7.3
 */
public class SearchResultsFragment extends WebFragmentImpl {

    protected static final Log log = LogFactory.getLog(SearchResultsFragment.class);

    @FindBy(id = "nxl_gridDamLayout:nxw_damNewAsset_form:nxw_damSearchResultsActions_damNewAsset_subview:nxw_damSearchResultsActions_damNewAsset_link")
    public WebElement newAssetButton;

    @FindBy(id = "nxl_gridDamLayout:dam_search_nxw_searchResults:nxl_dam_box_listing_ajax:damPanelLeft_header")
    public WebElement selectAllCheckbox;

    public SearchResultsFragment(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    public DAMPage createAsset(DAMPage damPage, String type, String title,
            String description, String originalAuthor, String authoringDate) {
        newAssetButton.click();
        AssetCreationFancyBoxFragment fancyBoxFragment = showAssetCreation(damPage);
        LayoutElement layout = new LayoutElement(driver,
                "nxl_gridDamLayout:nxw_damSearchResultsActions_damNewAsset_fancyform");
        Select select = new Select(
                layout.getSubElement("nxw_damNewAsset_after_view_select"));
        select.selectByValue(type);

        Locator.waitForTextPresent(By.id("fancybox-content"),
                "You can upload files of any format");

        layout.getWidget("nxl_heading:nxw_title").setInputValue(title);
        layout.getWidget("nxl_heading:nxw_description").setInputValue(
                description);
        layout.getWidget("nxl_dam_common:nxw_damc_author_1").setInputValue(
                originalAuthor);
        layout.getWidget("nxl_dam_common:nxw_damc_authoringDate_1InputDate").setInputValue(
                authoringDate);

        fancyBoxFragment.findElement(
                By.xpath("//div[@id='fancybox-content']//input[@value='Create']")).click();
        return damPage.asPage(DAMPage.class);
    }

    public AssetCreationFancyBoxFragment showAssetCreation(DAMPage currentPage) {
        newAssetButton.click();
        WebElement element = currentPage.getFancyBoxContent();
        return getWebFragment(element, AssetCreationFancyBoxFragment.class);
    }

    /**
     * Returns the bubble box element containing the given {@code text}.
     */
    public WebFragment getBubbleBox(String text) {
        List<WebElement> elements = element.findElements(By.className("jsDamItem"));
        for (WebElement ele : elements) {
            if (ele.getText().contains(text)) {
                return new WebFragmentImpl(driver, ele);
            }
        }
        throw new NoSuchElementException(String.format(
                "No bubble box found with text '%s'", text));
    }

    /**
     * Click on the bubble box element containing the given {@code text} to
     * select an asset.
     */
    public void selectAsset(final String text) {
        getBubbleBox(text).click();
        Locator.waitUntilGivenFunction(new Function<WebDriver, Boolean>() {
            public Boolean apply(WebDriver driver) {
                log.trace("Waiting for " + text + " to be selected");
                return getBubbleBox(text).getAttribute("class").contains(
                        "selectedItem");
            }
        });
    }

    /**
     * Returns the bubble box element of the selected asset.
     */
    public WebFragment getSelectedAsset() {
        return new WebFragmentImpl(driver,
                element.findElement(By.cssSelector(".jsDamItem.selectedItem")));
    }

    public void selectAll() {
        selectAllCheckbox.click();
    }

    public BulkEditFancyBoxFragment showBulkEdit(DAMPage currentPage) {
        WebElement bulkEditButton = Locator.findElementAndWaitUntilEnabled(By.id("nxl_gridDamLayout:dam_search_nxw_searchResults_buttons:nxw_damBulkEdit_form:nxw_cvButton_damBulkEdit_subview:nxw_cvButton_damBulkEdit_link"));
        bulkEditButton.click();
        WebElement element = currentPage.getFancyBoxContent();
        return getWebFragment(element, BulkEditFancyBoxFragment.class);
    }

}
