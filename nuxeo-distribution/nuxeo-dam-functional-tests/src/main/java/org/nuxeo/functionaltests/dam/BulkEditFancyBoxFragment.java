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

import org.nuxeo.functionaltests.forms.WidgetElement;
import org.nuxeo.functionaltests.fragment.WebFragmentImpl;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 5.7.3
 */
public class BulkEditFancyBoxFragment extends WebFragmentImpl {

    @FindBy(xpath = "//div[@id='fancybox-content']//input[@value='Update']")
    public WebElement updateButton;

    @FindBy(xpath = "//div[@id='fancybox-content']//input[@value='Cancel']")
    public WebElement cancelButton;

    @FindBy(xpath = "//div[@id='fancybox-content']//input[contains(@id, 'nxl_damBulkEdit_edit:nxw_damc_author')]")
    public WebElement originalAuthorInput;

    @FindBy(xpath = "//div[@id='fancybox-content']//input[contains(@id, 'nxl_damBulkEdit_edit:nxw_damc_authoringDate')]")
    public WebElement authoringDateInput;

    public BulkEditFancyBoxFragment(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    public void update() {
        updateButton.click();
    }

    public void cancel() {
        cancelButton.click();
    }

    public void fillOriginalAuthor(String author) {
        new WidgetElement(driver, originalAuthorInput).setInputValue(author);
    }

    public void fillAuthoringDate(String date) {
        new WidgetElement(driver, authoringDateInput).setInputValue(date);
    }

}
