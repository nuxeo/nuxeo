/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Sun Seng David TAN <stan@nuxeo.com>
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.functionaltests.pages.admincenter;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.forms.NewVocabularyEntryForm;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import com.google.common.base.Function;

public class VocabulariesPage extends AdminCenterBasePage {

    @FindBy(linkText = "Add a new vocabulary entry")
    @Required
    WebElement addNewEntryLink;

    @FindBy(id = "selectDirectoryForm:directoriesList")
    @Required
    WebElement directoriesListSelectElement;

    @FindBy(id = "viewDirectoryEntries")
    @Required
    WebElement directoryEntriesForm;

    public VocabulariesPage(WebDriver driver) {
        super(driver);
    }

    /**
     * @return
     * @since 5.9.3
     */
    public VocabulariesPage addEntry(final String entryId,
            final String parentId, final String entryEnglishLabel,
            final String entryFrenchLabel, final boolean obsolete,
            final int order) {
        addNewEntryLink.click();
        NewVocabularyEntryForm newVocabularyEntryForm = getWebFragment(
                By.id("addEntryView:addEntryForm"),
                NewVocabularyEntryForm.class);
        newVocabularyEntryForm.setNewVocabularyId(entryId);
        newVocabularyEntryForm.setNewVocabularyEnglishLabel(entryEnglishLabel);
        newVocabularyEntryForm.setNewVocabularyFrenchLabel(entryFrenchLabel);
        newVocabularyEntryForm.setNewVocabularyObsolete(obsolete);
        newVocabularyEntryForm.setNewVocabularyOrder(order);
        newVocabularyEntryForm.setNewVocabularyParentId(parentId);
        newVocabularyEntryForm.save();
        Locator.waitForTextPresent(By.id("ambiance-notification"),
                "Vocabulary entry added");
        return asPage(VocabulariesPage.class);
    }

    /**
     * @since 5.9.3
     */
    public VocabulariesPage deleteEntry(final String entryId) {
        WebElement entryToBeDeleted = getDirectoryEntryRow(entryId);
        WebElement entryDeleteButton = entryToBeDeleted.findElement(By.xpath("td/input[@value='Delete']"));
        entryDeleteButton.click();
        Alert confirmRemove = driver.switchTo().alert();
        confirmRemove.accept();
        Locator.waitForTextPresent(By.id("ambiance-notification"),
                "Vocabulary entry deleted");
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
        throw new NoSuchElementException(String.format(
                "Vocabulary entry with id %s not found", entryId));
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
                directoriesListSelect.selectByVisibleText(webElement.getText());
                waitForLoading("selectDirectoryForm:selectDirectoryStatus");
                return asPage(VocabulariesPage.class);
            }
        }
        throw new NoSuchElementException(String.format(
                "directoryName %s not available", directoryName));
    }

    /**
     * @since 5.9.3
     * @since 5.9.6: specify the waiter id
     */
    protected void waitForLoading(String waiterId) {
        String waiterPath = String.format("//span[@id=\"%s\"]", waiterId);
        final String startWaiter = waiterPath
                + "//span[@class=\"rf-st-start\"]";
        final String stopWaiter = waiterPath + "//span[@class=\"rf-st-stop\"]";
        try {
            Locator.waitUntilGivenFunctionIgnoring(
                    new Function<WebDriver, Boolean>() {
                        public Boolean apply(WebDriver driver) {
                            return driver.findElement(By.xpath(stopWaiter)).getAttribute(
                                    "style").equals("display: none;");
                        }
                    }, StaleElementReferenceException.class);
        } catch (TimeoutException e) {
            // maybe this was fast and it is already loaded
            // let's keep going and see
        }
        Locator.waitUntilGivenFunctionIgnoring(
                new Function<WebDriver, Boolean>() {
                    public Boolean apply(WebDriver driver) {
                        return driver.findElement(By.xpath(startWaiter)).getAttribute(
                                "style").equals("display: none;");
                    }
                }, StaleElementReferenceException.class);
    }
}
