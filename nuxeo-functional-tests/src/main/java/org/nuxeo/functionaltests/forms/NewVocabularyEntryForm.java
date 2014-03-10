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

import java.util.Arrays;

import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.fragment.WebFragmentImpl;
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

    @FindBy(id = "nxw_parent_openPopup")
    private WebElement newParentPopup;

    @FindBy(id = "addEntryView:addEntryForm:nxl_l10nsubjects_vocabulary:nxw_l10nxvocabulary_obsolete_checkbox")
    @Required
    private WebElement notObsoleteInput;

    @FindBy(id = "addEntryView:addEntryForm:nxl_l10nsubjects_vocabulary:nxw_l10nxvocabulary_obsolete_checkbox")
    @Required
    private WebElement obsoleteInput;

    @FindBy(id = "addEntryView:addEntryForm:nxl_l10nsubjects_vocabulary:nxw_l10nxvocabulary_order")
    @Required
    private WebElement orderInput;

    public NewVocabularyEntryForm(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    private void findParentNodeAndSelect(final WebElement selectParentFancyBox,
            final String[] path) {
        if (path.length == 1) {
            selectParentFancyBox.findElement(By.linkText(path[0])).click();
        } else {
            WebElement node = selectParentFancyBox.findElement(By.xpath("//table[contains(text(),'"
                    + path[0] + "')]"));
            node.findElement(By.xpath("tbody/tr[1]/td[1]/div/a")).click();
            findParentNodeAndSelect(selectParentFancyBox,
                    Arrays.copyOfRange(path, 1, path.length));
        }
    }

    public void save() {
        getElement().findElement(By.xpath("//input[@value='Create']")).click();
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
            obsoleteInput.click();
        } else {
            notObsoleteInput.click();
        }
    }

    public void setNewVocabularyOrder(final int vocabularyOrder) {
        orderInput.clear();
        orderInput.sendKeys(vocabularyOrder + "");
    }

    public void setNewVocabularyParentId(final String parentLabelPath) {
        newParentPopup.click();
        WebElement selectParentFancyBox = Locator.findElementWithTimeout(By.id("fancybox-content"));
        String[] split = parentLabelPath.split("/");
        findParentNodeAndSelect(selectParentFancyBox, split);
    }

}
