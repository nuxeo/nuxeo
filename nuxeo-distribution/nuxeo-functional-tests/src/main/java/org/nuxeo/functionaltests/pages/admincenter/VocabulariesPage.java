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
 *     Sun Seng David TAN <stan@nuxeo.com>
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 *     Yannis JULIENNE
 */
package org.nuxeo.functionaltests.pages.admincenter;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.fragment.NewVocabularyEntryForm;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

public class VocabulariesPage extends AdminCenterBasePage {

    @FindBy(linkText = "Add a New Vocabulary Entry")
    @Required
    WebElement addNewEntryLink;

    @FindBy(id = "selectDirectoryForm:directoriesList")
    @Required
    WebElement directoriesListSelectElement;

    @FindBy(id = "viewDirectoryEntries")
    @Required
    WebElement directoryEntriesForm;

    @FindBy(xpath = "//form[@id='viewDirectoryEntries']//thead")
    @Required
    WebElement directoryEntriesHeader;

    public VocabulariesPage(WebDriver driver) {
        super(driver);
    }

    /**
     * @return
     * @since 5.9.3
     */
    public VocabulariesPage addEntry(final String entryId, final String parentId, final String entryEnglishLabel,
            final String entryFrenchLabel, final boolean obsolete, final int order) {
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        Locator.waitUntilEnabledAndClick(addNewEntryLink);
        arm.end();
        NewVocabularyEntryForm newVocabularyEntryForm = getWebFragment(By.id("addEntryView:addEntryForm"),
                NewVocabularyEntryForm.class);
        newVocabularyEntryForm.setNewVocabularyId(entryId);
        newVocabularyEntryForm.setNewVocabularyEnglishLabel(entryEnglishLabel);
        newVocabularyEntryForm.setNewVocabularyFrenchLabel(entryFrenchLabel);
        newVocabularyEntryForm.setNewVocabularyObsolete(obsolete);
        newVocabularyEntryForm.setNewVocabularyOrder(order);
        if (!StringUtils.isBlank(parentId)) {
            newVocabularyEntryForm.setNewVocabularyParentId(parentId);
        }
        arm.begin();
        newVocabularyEntryForm.save();
        Locator.waitForTextPresent(By.id("ambiance-notification"), "Vocabulary entry added");
        arm.end();
        return asPage(VocabulariesPage.class);
    }

    /**
     * @since 5.9.3
     */
    public VocabulariesPage deleteEntry(final String entryId) {
        WebElement entryToBeDeleted = getDirectoryEntryRow(entryId);
        WebElement entryDeleteButton = entryToBeDeleted.findElement(By.xpath("td/input[@value='Delete']"));
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        Locator.waitUntilEnabledAndClick(entryDeleteButton);
        Alert confirmRemove = driver.switchTo().alert();
        confirmRemove.accept();
        Locator.waitForTextPresent(By.id("ambiance-notification"), "Vocabulary entry deleted");
        arm.end();
        return asPage(VocabulariesPage.class);
    }

    /**
     * Return the list of directories in the select box
     */
    public List<String> getDirectoriesList() {
        Select directoriesListSelect = new Select(directoriesListSelectElement);
        ArrayList<String> directoryList = new ArrayList<String>();
        List<WebElement> list = directoriesListSelect.getOptions();
        for (WebElement webElement : list) {
            directoryList.add(webElement.getText());
        }
        return directoryList;
    }

    /**
     * @since 5.9.3
     */
    public WebElement getDirectoryEntryRow(final String entryId) {
        List<WebElement> entryElementList = directoryEntriesForm.findElements(By.xpath("table/tbody/tr"));
        for (WebElement entryElement : entryElementList) {
            WebElement entryIdElement = entryElement.findElement(By.xpath("td[2]/span"));
            if (entryId.equals(entryIdElement.getText())) {
                return entryElement;
            }
        }
        throw new NoSuchElementException(String.format("Vocabulary entry with id %s not found", entryId));
    }

    /**
     * @since 5.9.3
     */
    public boolean hasEntry(final String entryId) {
        try {
            getDirectoryEntryRow(entryId);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Select one of the directory in the select box
     *
     * @param directoryName
     */
    public VocabulariesPage select(String directoryName) {
        Select directoriesListSelect = new Select(directoriesListSelectElement);
        List<WebElement> list = directoriesListSelect.getOptions();
        for (WebElement webElement : list) {
            if (directoryName.trim().equals(webElement.getText().trim())) {
                AjaxRequestManager a = new AjaxRequestManager(driver);
                a.watchAjaxRequests();
                directoriesListSelect.selectByVisibleText(webElement.getText());
                a.waitForAjaxRequests();
                return asPage(VocabulariesPage.class);
            }
        }
        throw new NoSuchElementException(String.format("directoryName %s not available", directoryName));
    }

    /**
     * Returns true if the directory entries table contains given string in its header.
     *
     * @since 7.1
     */
    public boolean hasHeaderLabel(String headerLabel) {
        return directoryEntriesHeader.getText().contains(headerLabel);
    }

}
