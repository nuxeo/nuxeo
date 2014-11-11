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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.nuxeo.functionaltests.fragment.WebFragmentImpl;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 5.7.3
 */
public class AssetViewFragment extends WebFragmentImpl {

    @FindBy(xpath = "//div[@id='nxl_gridDamLayout:nxw_damAssetView_panel']//div[@class='documentTitle']")
    public WebElement assetTitle;

    public AssetViewFragment(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    public String getAssetTitle() {
        return assetTitle.getText();
    }

    public void checkAssetTitle(String expectedTitle) {
        assertEquals(expectedTitle, getAssetTitle());
    }

    public void clickOnAction(String actionTitle) {
        String xpath = String.format(
                "//div[@id='nxl_gridDamLayout:nxw_damAssetView_panel']//img[@title='%s']",
                actionTitle);
        element.findElement(By.xpath(xpath)).click();
    }

    /**
     * Returns the foldable box element with the given title.
     */
    public FoldableBoxFragment getFoldableBox(String title, boolean isAjax) {
        List<WebElement> elements = element.findElements(By.className("foldableBox"));
        for (WebElement ele : elements) {
            if (ele.findElement(By.tagName("h3")).getText().contains(title)) {
                FoldableBoxFragment foldableBox = getWebFragment(
                        ele.findElement(By.xpath("..")),
                        FoldableBoxFragment.class);
                foldableBox.setAjax(isAjax);
                return foldableBox;
            }
        }
        throw new NoSuchElementException(String.format(
                "No foldable box found with text '%s'", title));
    }

}
