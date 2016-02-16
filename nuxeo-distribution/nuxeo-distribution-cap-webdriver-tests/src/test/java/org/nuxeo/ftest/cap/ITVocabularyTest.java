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
package org.nuxeo.ftest.cap;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.admincenter.VocabulariesPage;
import org.nuxeo.functionaltests.pages.tabs.EditTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.google.common.base.Function;

import static org.nuxeo.ftest.cap.TestConstants.TEST_FILE_TITLE;

import static org.nuxeo.functionaltests.Constants.FILE_TYPE;
import static org.nuxeo.functionaltests.Constants.NXDOC_URL_FORMAT;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @since 5.9.3
 */
public class ITVocabularyTest extends AbstractTest {

    public final static String L10N_SUBJECTS = "l10nsubjects";

    public final static String SAMPLE_SUBJECT_ENTRY_ID = "comics";

    public final static String SAMPLE_SUBJECT_ENTRY_LABEL = "Comics";

    private final static String WORKSPACE_TITLE = ITVocabularyTest.class.getSimpleName() + "_WorkspaceTitle_"
            + new Date().getTime();

    private static String fileId;

    @Before
    public void before() {
        String wsId = RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, WORKSPACE_TITLE, null);
        fileId = RestHelper.createDocument(wsId, FILE_TYPE, TEST_FILE_TITLE, null);
    }

    @After
    public void after() {
        RestHelper.cleanup();
        fileId = null;
    }

    /**
     * Edit a document and add a directory entry. Remove the entry from the vocabulary in Admin Center. Check that we
     * have a warning message and that we can remove the entry.
     */
    @Test
    public void testEntryNotFountAndRemove() throws UserNotConnectedException, IOException {
        try {
            login();
            open(String.format(NXDOC_URL_FORMAT, fileId));
            EditTabSubPage editTabSubPage = asPage(DocumentBasePage.class).getEditTab();

            // add a vocabulary entry
            Select2WidgetElement subjectsWidget = new Select2WidgetElement(driver,
                    driver.findElement(By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_subjects_1_select2']")),
                    true);
            subjectsWidget.selectValue(SAMPLE_SUBJECT_ENTRY_LABEL);
            DocumentBasePage documentBasePage = editTabSubPage.save();

            // delete the selected entry in admin center
            VocabulariesPage vocabulariesPage = documentBasePage.getAdminCenter().getVocabulariesPage();
            vocabulariesPage = vocabulariesPage.select(L10N_SUBJECTS);
            vocabulariesPage = vocabulariesPage.deleteEntry(SAMPLE_SUBJECT_ENTRY_ID);
            final VocabulariesPage temp = vocabulariesPage;
            try {
                Locator.waitUntilGivenFunction(new Function<WebDriver, Boolean>() {
                    @Override
                    public Boolean apply(WebDriver driver) {
                        return !temp.hasEntry(SAMPLE_SUBJECT_ENTRY_ID);
                    }
                });
                vocabulariesPage.getDirectoryEntryRow(SAMPLE_SUBJECT_ENTRY_ID);
                fail(String.format("The entry %s is supposed to be deleted but is still present in entry list",
                        SAMPLE_SUBJECT_ENTRY_ID));
            } catch (NoSuchElementException e) {
                // Expected behavior
            }

            // The entry should still be attached to the document but with a warning
            documentBasePage = vocabulariesPage.exitAdminCenter()
                                               .getHeaderLinks()
                                               .getNavigationSubPage()
                                               .goToDocument("Workspaces");

            documentBasePage = documentBasePage.getContentTab().goToDocument(WORKSPACE_TITLE);

            editTabSubPage = documentBasePage.getContentTab().goToDocument(TEST_FILE_TITLE).getEditTab();
            subjectsWidget = new Select2WidgetElement(driver,
                    driver.findElement(By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_subjects_1_select2']")),
                    true);
            List<WebElement> selectedEntries = subjectsWidget.getSelectedValues();
            assertEquals(1, selectedEntries.size());
            // because the entry is deleted, the id should be displayed
            assertTrue(selectedEntries.get(0).getText().endsWith(SAMPLE_SUBJECT_ENTRY_ID));
            // we should also have a warning
            WebElement warnImage = selectedEntries.get(0).findElement(By.xpath("div/img"));
            assertEquals("entry not found", warnImage.getAttribute("title"));

            // Check that if we remove this deleted entry and save, it is indeed
            // deleted
            subjectsWidget.removeFromSelection("art/" + SAMPLE_SUBJECT_ENTRY_ID);
            documentBasePage = editTabSubPage.save();
            editTabSubPage = documentBasePage.getEditTab();
            subjectsWidget = new Select2WidgetElement(driver,
                    driver.findElement(By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_subjects_1_select2']")),
                    true);
            assertEquals(0, subjectsWidget.getSelectedValues().size());
        } finally {
            logout();
            // Let's recreate the entry to leave state as it was
            DocumentBasePage documentBasePage = login();
            VocabulariesPage newVocabulariesPage = documentBasePage.getAdminCenter().getVocabulariesPage();
            newVocabulariesPage = newVocabulariesPage.select(L10N_SUBJECTS);
            newVocabulariesPage = newVocabulariesPage.addEntry(SAMPLE_SUBJECT_ENTRY_ID, "Art",
                    SAMPLE_SUBJECT_ENTRY_LABEL, "Bande dessinée", false, 10000000);
            assertTrue(newVocabulariesPage.hasEntry(SAMPLE_SUBJECT_ENTRY_ID));
        }
    }

    /**
     * Non regression test for NXP-16205
     *
     * @since 7.1
     */
    @Test
    public void testColumnsOnDirectorySwitch() throws UserNotConnectedException, IOException {
        DocumentBasePage documentBasePage = login();
        VocabulariesPage vocabulariesPage = documentBasePage.getAdminCenter().getVocabulariesPage();
        assertFalse(vocabulariesPage.hasHeaderLabel("English Label"));
        vocabulariesPage = vocabulariesPage.select(L10N_SUBJECTS);
        assertTrue(vocabulariesPage.hasHeaderLabel("English Label"));
    }

}
