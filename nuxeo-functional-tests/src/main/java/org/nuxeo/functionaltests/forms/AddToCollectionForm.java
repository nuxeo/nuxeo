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
package org.nuxeo.functionaltests.forms;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.fragment.WebFragmentImpl;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.google.common.base.Function;

/**
 * @since 5.9.3
 */
public class AddToCollectionForm extends WebFragmentImpl {

    private final static String ADD_ALL_BUTTON_ID = "document_content_buttons:nxw_cvButton_addSelectedToCollectionAction_fancy_subview:nxw_cvButton_addSelectedToCollectionAction_fancyform:addAll";

    private final static String ADD_BUTTON_ID = "nxw_documentActionsUpperButtons_addToCollectionAction_fancy_subview:nxw_documentActionsUpperButtons_addToCollectionAction_fancyform:add";

    private final static String S2_CHOOSE_COLLECTION_ID = "s2id_nxw_documentActionsUpperButtons_addToCollectionAction_fancy_subview:nxw_documentActionsUpperButtons_addToCollectionAction_fancyform:nxw_singleDocumentSuggestion_select2";

    private final static String S2_CHOOSE_COLLECTION_MULTIPLE_ID = "s2id_document_content_buttons:nxw_cvButton_addSelectedToCollectionAction_fancy_subview:nxw_cvButton_addSelectedToCollectionAction_fancyform:nxw_singleDocumentSuggestion_1_select2";

    private final static String NEW_COLLECTION_DESCRIPTION_ID = "nxw_documentActionsUpperButtons_addToCollectionAction_fancy_subview:nxw_documentActionsUpperButtons_addToCollectionAction_fancyform:description";

    private final static String NEW_COLLECTION_DESCRIPTION_MULTIPLE_ID = "document_content_buttons:nxw_cvButton_addSelectedToCollectionAction_fancy_subview:nxw_cvButton_addSelectedToCollectionAction_fancyform:nxw_cvButton_addSelectedToCollectionAction_fancyform_collectionDescriptionsPanel";

    private static final String EXISTING_COLLECTION_DESCRIPTION_ID = "nxw_documentActionsUpperButtons_addToCollectionAction_fancy_subview:nxw_documentActionsUpperButtons_addToCollectionAction_fancyform:scd";

    private static final String EXISTING_COLLECTION_DESCRIPTION_MULTIPLE_ID = "document_content_buttons:nxw_cvButton_addSelectedToCollectionAction_fancy_subview:nxw_cvButton_addSelectedToCollectionAction_fancyform:scd";

    private boolean multiple = false;

    public AddToCollectionForm(WebDriver driver, WebElement element) {
        super(driver, element);
        Locator.waitUntilGivenFunctionIgnoring(
                new Function<WebDriver, Boolean>() {
                    public Boolean apply(WebDriver driver) {
                        try {
                            driver.findElement(By.id(ADD_BUTTON_ID));
                            multiple = false;
                            return true;
                        } catch (NoSuchElementException e) {
                            driver.findElement(By.id(ADD_ALL_BUTTON_ID));
                            multiple = true;
                            return true;
                        }
                    }
                }, NoSuchElementException.class);
    }

    public DocumentBasePage add() {
        return add(DocumentBasePage.class);
    }

    public <T> T add(final Class<T> pageClassProxy) {
        Locator.findElementWaitUntilEnabledAndClick(By.id(ADD_BUTTON_ID));
        return AbstractTest.asPage(pageClassProxy);
    }

    public DocumentBasePage addAll() {
        return addAll(DocumentBasePage.class);
    }

    public <T> T addAll(final Class<T> pageClassProxy) {
        Locator.findElementWaitUntilEnabledAndClick(By.id(ADD_ALL_BUTTON_ID));
        return AbstractTest.asPage(pageClassProxy);
    }

    public void setCollection(final String collectionName) {
        Select2WidgetElement s2Collection = new Select2WidgetElement(driver,
                Locator.findElementWithTimeout(
                        By.id(multiple ? S2_CHOOSE_COLLECTION_MULTIPLE_ID
                                : S2_CHOOSE_COLLECTION_ID), getElement()),
                false);
        s2Collection.selectValue(collectionName);
        Locator.waitUntilGivenFunctionIgnoring(
                new Function<WebDriver, Boolean>() {
                    public Boolean apply(WebDriver driver) {
                        return StringUtils.isBlank(driver.findElement(
                                By.id(multiple ? ADD_ALL_BUTTON_ID
                                        : ADD_BUTTON_ID)).getAttribute(
                                "disabled"));
                    }
                }, StaleElementReferenceException.class);
    }

    public void setNewDescription(final String collectionDescription) {
        // TODO sort this sleep out. See NXP-14030.
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        Locator.findElementWithTimeout(By.id(multiple ? NEW_COLLECTION_DESCRIPTION_MULTIPLE_ID : NEW_COLLECTION_DESCRIPTION_ID)).sendKeys(
                collectionDescription);
    }

    public boolean isNewDescriptionVisible() {
        try {
            driver.findElement(By.id(multiple ? NEW_COLLECTION_DESCRIPTION_MULTIPLE_ID : NEW_COLLECTION_DESCRIPTION_ID));
            return true;
        } catch (final NoSuchElementException e) {
            return false;
        }
    }

    public boolean isExistingDescriptionVisible() {
        try {
            driver.findElement(By.id(multiple ? EXISTING_COLLECTION_DESCRIPTION_MULTIPLE_ID : EXISTING_COLLECTION_DESCRIPTION_ID));
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public String getExistingDescription() {
        return driver.findElement(By.id(multiple ? EXISTING_COLLECTION_DESCRIPTION_MULTIPLE_ID : EXISTING_COLLECTION_DESCRIPTION_ID)).getText();
    }

    public void removeDocumentToBeAddedToCollection(int index) {
        if (!multiple) {
            throw new UnsupportedOperationException(
                    "You are not adding many documents to the collection");
        }

        List<WebElement> docsToBeAdded = getElement().findElements(
                By.xpath("//div[@class='simpleBox']"));

        final int docsToBeAddedSize = docsToBeAdded.size();

        docsToBeAdded.get(index).findElement(By.xpath("a")).click();

        Locator.waitUntilGivenFunctionIgnoring(
                new Function<WebDriver, Boolean>() {
                    public Boolean apply(WebDriver driver) {
                        return getElement().findElements(
                                By.xpath("//div[@class='simpleBox']")).size() == docsToBeAddedSize - 1;
                    }
                }, StaleElementReferenceException.class);

    }
}
