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
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.admincenter.VocabulariesPage;
import org.nuxeo.functionaltests.pages.tabs.EditTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.google.common.base.Function;

/**
 * @since 5.9.3
 */
public class ITVocabularyTest extends AbstractTest {

    public final static String L10N_SUBJECTS = "l10nsubjects";

    public final static String SAMPLE_SUBJECT_ENTRY_ID = "comics";

    public final static String SAMPLE_SUBJECT_ENTRY_LABEL = "Comics";

    public final static String TEST_FILE_NAME = "test1";

    private final static String WORKSPACE_TITLE = "WorkspaceTitle_"
            + new Date().getTime();

    /**
     * Edit a document and add a directory entry. Remove the entry from the
     * vocabulary in Admin Center. Check that we have a warning message and that
     * we can remove the entry.
     */
    @Test
    public void testEntryNotFountAndRemove() throws UserNotConnectedException,
            IOException {
        DocumentBasePage documentBasePage = login();

        // Create test File
        DocumentBasePage workspacePage = createWorkspace(documentBasePage,
                WORKSPACE_TITLE, null);
        FileDocumentBasePage filePage = createFile(workspacePage,
                TEST_FILE_NAME, "Test File description", false, null, null,
                null);
        EditTabSubPage editTabSubPage = filePage.getEditTab();

        // add a vocabulary entry
        Select2WidgetElement subjectsWidget = new Select2WidgetElement(
                driver,
                driver.findElement(By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_subjects_select2']")),
                true);
        subjectsWidget.selectValue(SAMPLE_SUBJECT_ENTRY_LABEL);
        documentBasePage = editTabSubPage.save();

        // delete the selected entry in admin center
        VocabulariesPage vocabulariesPage = documentBasePage.getAdminCenter().getVocabulariesPage();
        vocabulariesPage = vocabulariesPage.select(L10N_SUBJECTS);
        vocabulariesPage = vocabulariesPage.deleteEntry(SAMPLE_SUBJECT_ENTRY_ID);
        final VocabulariesPage temp = vocabulariesPage;
        try {
            Locator.waitUntilGivenFunction(new Function<WebDriver, Boolean>() {
                public Boolean apply(WebDriver driver) {
                    return !temp.hasEntry(SAMPLE_SUBJECT_ENTRY_ID);
                }
            });
            vocabulariesPage.getDirectoryEntryRow(SAMPLE_SUBJECT_ENTRY_ID);
            fail(String.format(
                    "The entry %s is supposed to be deleted but is still present in entry list",
                    SAMPLE_SUBJECT_ENTRY_ID));
        } catch (NoSuchElementException e) {
            // Expected behavior
        }

        // The entry should still be attached to the document but with a warning
        documentBasePage = vocabulariesPage.exitAdminCenter().getHeaderLinks().getNavigationSubPage().goToDocument(
                "Workspaces");

        documentBasePage = documentBasePage.getContentTab().goToDocument(
                WORKSPACE_TITLE);

        editTabSubPage = documentBasePage.getContentTab().goToDocument(
                TEST_FILE_NAME).getEditTab();
        subjectsWidget = new Select2WidgetElement(
                driver,
                driver.findElement(By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_subjects_select2']")),
                true);
        List<WebElement> selectedEntries = subjectsWidget.getSelectedValues();
        assertEquals(1, selectedEntries.size());
        // because the entry is deleted, the id should be displayed
        assertTrue(selectedEntries.get(0).getText().endsWith(
                SAMPLE_SUBJECT_ENTRY_ID));
        // we should also have a warning
        WebElement warnImage = selectedEntries.get(0).findElement(
                By.xpath("div/img"));
        assertEquals("entry not found", warnImage.getAttribute("title"));

        // Check that if we remove this deleted entry and save, it is indeed
        // deleted
        subjectsWidget.removeFromSelection("art/" + SAMPLE_SUBJECT_ENTRY_ID);
        documentBasePage = editTabSubPage.save();
        editTabSubPage = documentBasePage.getEditTab();
        subjectsWidget = new Select2WidgetElement(
                driver,
                driver.findElement(By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_subjects_select2']")),
                true);
        assertEquals(0, subjectsWidget.getSelectedValues().size());

    }

    @After
    public void tearDown() throws UserNotConnectedException {
        logout();
        // Let's recreate the entry to leave state as it was
        DocumentBasePage documentBasePage = login();
        VocabulariesPage newVocabulariesPage = documentBasePage.getAdminCenter().getVocabulariesPage();
        newVocabulariesPage = newVocabulariesPage.select(L10N_SUBJECTS);
        newVocabulariesPage = newVocabulariesPage.addEntry(
                SAMPLE_SUBJECT_ENTRY_ID, "Art", SAMPLE_SUBJECT_ENTRY_LABEL,
                "Bande dessinée", false, 10000000);
        assertTrue(newVocabulariesPage.hasEntry(SAMPLE_SUBJECT_ENTRY_ID));
    }

}
