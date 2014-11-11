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

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.forms.WidgetElement;
import org.nuxeo.functionaltests.fragment.WebFragmentImpl;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @since 5.7.3
 */
public class SearchFormFragment extends WebFragmentImpl {

    @Required
    @FindBy(id = "nxl_gridDamLayout:nxw_damSearchesSelector_form:nxw_damSearchesSelector")
    public WebElement searchSelector;

    public static final String FORM_ID = "nxl_gridDamLayout:nxw_damSearchLayout_form:";

    public static final String LAYOUT_ID = FORM_ID + "nxl_dam_search_layout:";

    @Required
    @FindBy(id = FORM_ID + "nxw_doc_damSearchActions_damSubmitSearch")
    public WebElement filterButton;

    @Required
    @FindBy(id = FORM_ID + "nxw_doc_damSearchActions_damClearSearch")
    public WebElement clearButton;

    @Required
    @FindBy(id = LAYOUT_ID + "nxw_ecm_fulltext")
    public WebElement textInput;

    @Required
    @FindBy(id = LAYOUT_ID + "nxw_damc_author")
    public WebElement originalAuthorInput;

    @Required
    @FindBy(id = LAYOUT_ID + "nxw_damc_authoringDate_startInputDate")
    public WebElement authoringDateFromInput;

    @Required
    @FindBy(id = LAYOUT_ID + "nxw_damc_authoringDate_endInputDate")
    public WebElement authoringDateToInput;

    public SearchFormFragment(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    public String getSelectedContentView() {
        return new Select(searchSelector).getFirstSelectedOption().getText();
    }

    public void doSearch() {
        filterButton.click();
    }

    public void clearSearch() {
        clearButton.click();
    }

    public void fillText(String text) {
        textInput.clear();
        textInput.sendKeys(text);
    }

    public void fillOriginalAuthor(String author) {
        new WidgetElement(driver, originalAuthorInput).setInputValue(author);
    }

    public void fillAuthoringDate(String from, String to) {
        new WidgetElement(driver, authoringDateFromInput).setInputValue(from);
        new WidgetElement(driver, authoringDateToInput).setInputValue(to);
    }
}
