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

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.fragment.WebFragmentImpl;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * @since 5.7.3
 */
public class AssetCreationFancyBoxFragment extends WebFragmentImpl {

    @FindBy(xpath = "//div[@id='fancybox-content']//input[@value='Cancel']")
    public WebElement cancelButton;

    public AssetCreationFancyBoxFragment(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    public void cancel() {
        cancelButton.click();
        // make sure the fancybox content is not loaded anymore
        WebDriverWait wait = new WebDriverWait(driver,
                AbstractTest.LOAD_TIMEOUT_SECONDS);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("fancybox-content")));
    }
}
