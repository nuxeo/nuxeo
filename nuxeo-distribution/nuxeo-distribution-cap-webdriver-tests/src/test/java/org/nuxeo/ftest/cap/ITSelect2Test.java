/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
package org.nuxeo.ftest.cap;

import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.FileDocumentBasePage;
import org.nuxeo.functionaltests.pages.tabs.EditTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.nuxeo.ftest.cap.TestConstants.TEST_FILE_TITLE;

import static org.nuxeo.functionaltests.Constants.FILE_TYPE;
import static org.nuxeo.functionaltests.Constants.NXDOC_URL_FORMAT;
import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;
import static org.nuxeo.functionaltests.Constants.WORKSPACE_TYPE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Select2 feature test.
 *
 * @since 5.7.3
 */
public class ITSelect2Test extends AbstractTest {

    private final static String WORKSPACE_TITLE = ITSelect2Test.class.getSimpleName() + "_WorkspaceTitle_" + new Date().getTime();

    public final static String[] SUBJECTS = { "Comics", "Religion", "Education" };

    public final static String COVERAGE = "France";

    public static final String S2_COVERAGE_FIELD_XPATH = "//*[@id='s2id_document_edit:nxl_dublincore:nxw_coverage_1_select2']/a/span";

    private static String fileId;

    @Before
    public void before() {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD, TEST_USERNAME, "lastname1", "company1", "email1", "members");
        String wsId = RestHelper.createDocument(WORKSPACES_PATH, WORKSPACE_TYPE, WORKSPACE_TITLE, null);
        fileId = RestHelper.createDocument(wsId, FILE_TYPE, TEST_FILE_TITLE, null);
        RestHelper.addPermission(wsId, TEST_USERNAME, "Everything");
    }

    @After
    public void after() {
        RestHelper.cleanup();
        fileId = null;
    }

    /**
     * Create a file document and manipulate coverage and subjects fields based on select2 attributes.
     *
     * @throws Exception
     * @since 5.7.3
     */
    @Test
    public void testSelect2Edit() throws Exception {
        login(TEST_USERNAME, TEST_PASSWORD);
        open(String.format(NXDOC_URL_FORMAT, fileId));

        FileDocumentBasePage filePage = asPage(FileDocumentBasePage.class);
        EditTabSubPage editTabSubPage = filePage.getEditTab();

        Select2WidgetElement subjectsWidget = new Select2WidgetElement(driver,
                driver.findElement(By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_subjects_1_select2']")),
                true);
        subjectsWidget.selectValues(SUBJECTS);

        Select2WidgetElement coverageWidget = new Select2WidgetElement(driver,
                driver.findElement(By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_coverage_1_select2']")),
                false);
        coverageWidget.selectValue(COVERAGE);

        editTabSubPage.save();

        editTabSubPage = filePage.getEditTab();

        WebElement savedCoverage = driver.findElement(By.xpath(S2_COVERAGE_FIELD_XPATH));
        final String text = savedCoverage.getText();
        assertNotNull(text);
        assertTrue(text.endsWith(COVERAGE));

        List<WebElement> savedSubjects = driver.findElements(
                By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_subjects_1_select2']/ul/li/div"));
        assertEquals(savedSubjects.size(), SUBJECTS.length);

        // Remove the second subject
        WebElement deleteSecondSubjectAction = driver.findElement(
                By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_subjects_1_select2']/ul/li[2]/a"));
        deleteSecondSubjectAction.click();

        // We need to do this because select2 take a little while to write in
        // the form that an entry has been deleted
        Thread.sleep(250);

        editTabSubPage.save();

        filePage.getEditTab();

        // Make sure we have one subject removed
        savedSubjects = driver.findElements(
                By.xpath("//*[@id='s2id_document_edit:nxl_dublincore:nxw_subjects_1_select2']/ul/li/div"));
        assertEquals(savedSubjects.size(), SUBJECTS.length - 1);

        logout();
    }

}
