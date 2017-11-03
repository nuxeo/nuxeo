/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Yannis JULIENNE
 */
package org.nuxeo.functionaltests.fragment;

import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 5.9.3
 */
public class NewVocabularyEntryForm extends WebFragmentImpl {

    @FindBy(id = "addEntryView:addEntryForm:nxl_l10nsubjects_vocabulary:nxw_l10nxvocabulary_label_en")
    @Required
    private WebElement englishLabelInput;

    @FindBy(id = "addEntryView:addEntryForm:nxl_l10nsubjects_vocabulary:nxw_l10nxvocabulary_label_fr")
    @Required
    private WebElement frenchLabelInput;

    @FindBy(id = "addEntryView:addEntryForm:nxl_l10nsubjects_vocabulary:nxw_l10nxvocabulary_id")
    @Required
    private WebElement idInput;

    @FindBy(id = "s2id_addEntryView:addEntryForm:nxl_l10nsubjects_vocabulary:nxw_parent_select2")
    private WebElement newParentSuggestion;

    @FindBy(id = "addEntryView:addEntryForm:nxl_l10nsubjects_vocabulary:nxw_l10nxvocabulary_obsolete_checkbox:0")
    @Required
    private WebElement notObsoleteInput;

    @FindBy(id = "addEntryView:addEntryForm:nxl_l10nsubjects_vocabulary:nxw_l10nxvocabulary_obsolete_checkbox:1")
    @Required
    private WebElement obsoleteInput;

    @FindBy(id = "addEntryView:addEntryForm:nxl_l10nsubjects_vocabulary:nxw_l10nxvocabulary_order")
    @Required
    private WebElement orderInput;

    public NewVocabularyEntryForm(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    public void save() {
        Locator.waitUntilEnabledAndClick(getElement().findElement(By.xpath("//input[@value='Create']")));
    }

    public void setNewVocabularyEnglishLabel(final String vocabularyEnglishLabel) {
        englishLabelInput.sendKeys(vocabularyEnglishLabel);
    }

    public void setNewVocabularyFrenchLabel(final String vocabularyFrenchLabel) {
        frenchLabelInput.sendKeys(vocabularyFrenchLabel);
    }

    public void setNewVocabularyId(final String vocabularyId) {
        idInput.sendKeys(vocabularyId);
    }

    public void setNewVocabularyObsolete(final boolean obsolete) {
        if (obsolete) {
            Locator.waitUntilEnabledAndClick(obsoleteInput);
        } else {
            Locator.waitUntilEnabledAndClick(notObsoleteInput);
        }
    }

    public void setNewVocabularyOrder(final int vocabularyOrder) {
        orderInput.clear();
        orderInput.sendKeys(vocabularyOrder + "");
    }

    public void setNewVocabularyParentId(final String parentLabelPath) {
        Select2WidgetElement select2 = new Select2WidgetElement(driver, newParentSuggestion, false);
        select2.selectValue(parentLabelPath);
    }

}
