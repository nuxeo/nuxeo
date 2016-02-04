/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.functionaltests.fragment;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * @since 5.9.3
 */
public class AddToCollectionForm extends WebFragmentImpl {

    private final static String ADD_BUTTON_ID = "nxw_addToCollectionAction_after_view_fancy_subview:nxw_addToCollectionAction_after_view_fancyform:add";

    private final static String S2_CHOOSE_COLLECTION_ID = "s2id_nxw_addToCollectionAction_after_view_fancy_subview:nxw_addToCollectionAction_after_view_fancyform:nxw_singleDocumentSuggestion_1_select2";

    private final static String NEW_COLLECTION_DESCRIPTION_ID = "nxw_addToCollectionAction_after_view_fancy_subview:nxw_addToCollectionAction_after_view_fancyform:description";

    private static final String EXISTING_COLLECTION_DESCRIPTION_ID = "nxw_addToCollectionAction_after_view_fancy_subview:nxw_addToCollectionAction_after_view_fancyform:scd";

    protected boolean multiple = false;

    public AddToCollectionForm(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    protected boolean isMultiple() {
        return multiple;
    }

    public DocumentBasePage add() {
        return add(DocumentBasePage.class);
    }

    public <T> T add(final Class<T> pageClassProxy) {
        Locator.findElementWaitUntilEnabledAndClick(By.id(ADD_BUTTON_ID));
        return AbstractTest.asPage(pageClassProxy);
    }

    protected String getAddButtonId() {
        return ADD_BUTTON_ID;
    }

    protected String getChooseCollectionId() {
        return S2_CHOOSE_COLLECTION_ID;
    }

    protected String getNewCollectionDescriptionId() {
        return NEW_COLLECTION_DESCRIPTION_ID;
    }

    protected String getCollectionDescriptionId() {
        return EXISTING_COLLECTION_DESCRIPTION_ID;
    }

    public void setCollection(final String collectionName) {
        Locator.findElementAndWaitUntilEnabled(By.id(getChooseCollectionId()));
        Select2WidgetElement s2Collection = new Select2WidgetElement(driver, getChooseCollectionId());
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        s2Collection.selectValue(collectionName, false, true);
        arm.end();
        Locator.findElementAndWaitUntilEnabled(By.id(getAddButtonId()));
    }

    public void setNewDescription(final String collectionDescription) {
        Locator.findElementAndWaitUntilEnabled(By.id(getNewCollectionDescriptionId())).sendKeys(collectionDescription);
    }

    public boolean isNewDescriptionVisible() {
        try {
            driver.findElement(By.id(getNewCollectionDescriptionId()));
            return true;
        } catch (final NoSuchElementException e) {
            return false;
        }
    }

    public boolean isExistingDescriptionVisible() {
        try {
            driver.findElement(By.id(getCollectionDescriptionId()));
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public String getExistingDescription() {
        return driver.findElement(By.id(getCollectionDescriptionId())).getText();
    }

}
