/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.fragment;

import java.util.List;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.google.common.base.Function;

/**
 * Multiple version of the "add to collection" popup, triggered from content views.
 *
 * @since 8.1
 */
public class AddAllToCollectionForm extends AddToCollectionForm {

    private final static String ADD_BUTTON_ID = "document_content_buttons:nxw_addSelectedToCollectionAction_after_view_fancy_subview:nxw_addSelectedToCollectionAction_after_view_fancyform:addAll";

    private final static String S2_CHOOSE_COLLECTION_ID = "s2id_document_content_buttons:nxw_addSelectedToCollectionAction_after_view_fancy_subview:nxw_addSelectedToCollectionAction_after_view_fancyform:nxw_singleDocumentSuggestion_2_select2";

    private final static String NEW_COLLECTION_DESCRIPTION_ID = "document_content_buttons:nxw_addSelectedToCollectionAction_after_view_fancy_subview:nxw_addSelectedToCollectionAction_after_view_fancyform:description";

    private static final String EXISTING_COLLECTION_DESCRIPTION_ID = "document_content_buttons:nxw_addSelectedToCollectionAction_after_view_fancy_subview:nxw_addSelectedToCollectionAction_after_view_fancyform:scd";

    public AddAllToCollectionForm(WebDriver driver, WebElement element) {
        super(driver, element);
        multiple = true;
    }

    public DocumentBasePage addAll() {
        return addAll(DocumentBasePage.class);
    }

    public <T> T addAll(final Class<T> pageClassProxy) {
        Locator.findElementWaitUntilEnabledAndClick(By.id(ADD_BUTTON_ID));
        return AbstractTest.asPage(pageClassProxy);
    }

    @Override
    protected String getAddButtonId() {
        return ADD_BUTTON_ID;
    }

    @Override
    protected String getChooseCollectionId() {
        return S2_CHOOSE_COLLECTION_ID;
    }

    @Override
    protected String getNewCollectionDescriptionId() {
        return NEW_COLLECTION_DESCRIPTION_ID;
    }

    @Override
    protected String getCollectionDescriptionId() {
        return EXISTING_COLLECTION_DESCRIPTION_ID;
    }

    public void removeDocumentToBeAddedToCollection(int index) {
        if (!isMultiple()) {
            throw new UnsupportedOperationException("You are not adding many documents to the collection");
        }

        List<WebElement> docsToBeAdded = getElement().findElements(By.xpath("//div[@class='simpleBox']"));

        final int docsToBeAddedSize = docsToBeAdded.size();

        docsToBeAdded.get(index).findElement(By.xpath("a")).click();

        Locator.waitUntilGivenFunctionIgnoring(new Function<WebDriver, Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                return getElement().findElements(By.xpath("//div[@class='simpleBox']")).size() == docsToBeAddedSize - 1;
            }
        }, StaleElementReferenceException.class);

    }

}
