/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
