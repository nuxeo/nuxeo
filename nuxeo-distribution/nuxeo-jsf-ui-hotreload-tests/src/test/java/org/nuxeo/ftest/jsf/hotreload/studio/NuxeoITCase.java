/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ftest.jsf.hotreload.studio;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.forms.LayoutElement;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.forms.FileCreationFormPage;
import org.nuxeo.functionaltests.pages.search.SearchPage;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import static org.nuxeo.functionaltests.Constants.WORKSPACES_PATH;

import static org.junit.Assert.assertEquals;

/**
 * Test case for testing Studio generated jars on Nuxeo, holds all tests that should be run on most of target platforms.
 *
 * @since 9.3
 */
public abstract class NuxeoITCase extends NuxeoITCaseHelper {

    private static final String USERNAME = "gudule";

    private static final String PASSWD = "gudule1";

    @Before()
    public void before() {
        RestHelper.createUser(USERNAME, PASSWD, "firstname1", "lastname1", "company1", "email1", "members");
        RestHelper.addPermission(WORKSPACES_PATH, USERNAME, "Write");
    }

    @After
    public void after() {
        logout();
        RestHelper.deleteDocument(WORKSPACES_PATH + TEST_WS_TITLE);
        RestHelper.removePermissions(WORKSPACES_PATH, USERNAME);
        RestHelper.cleanup();
    }

    protected String getTestCaseId() {
        return this.getClass().getSimpleName();
    }

    protected StudioDocumentPage getTestStudioDocument() {
        StudioDocumentPage d = asPage(StudioDocumentPage.class);
        d.setTestId(getTestCaseId());
        return d;
    }

    protected void doLog() throws UserNotConnectedException {
        login(USERNAME, PASSWD);
    }

    @Test
    public void testCreateStudioDocument() throws Exception {
        doLog();
        getOrCreateTestWorkspace().getContentTab().getDocumentCreatePage("My test document", StudioDocumentPage.class);

        StudioDocumentPage page = getTestStudioDocument();
        page = page.createDocument("My creation title", true).checkDocumentView("My creation title", true);

        // navigate to edit tab and just change the title
        page.getEditTab();
        getTestStudioDocument().checkDocumentEditForm("My creation title", true)
                               .editDocument("My edited creation title", false)
                               .checkDocumentView("My edited creation title", true);

        // check state is project
        WebElement form = driver.findElement(By.id("nxl_grid_summary_layout:nxw_summary_current_document_states_form"));
        Assert.assertEquals("State\nProject", form.getText());
    }

    @Test
    public void testEditStudioDocument() throws Exception {
        doLog();
        getOrCreateTestWorkspace().getContentTab().getDocumentCreatePage("My test document", StudioDocumentPage.class);

        StudioDocumentPage page = getTestStudioDocument();
        page = page.createDocument("My edition title", false).checkDocumentView("My edition title", false);

        // navigate to edit tab, change the title and fill other metadata
        page.getEditTab();
        page.checkDocumentEditForm("My edition title", false)
            .editDocument("My edited edition title", true)
            .checkDocumentView("My edited edition title", true);

        // check state is project
        WebElement form = driver.findElement(By.id("nxl_grid_summary_layout:nxw_summary_current_document_states_form"));
        Assert.assertEquals("State\nProject", form.getText());
    }

    @Test
    public void testStudioTab() throws Exception {
        doLog();

        // create an invoice doc
        getOrCreateTestWorkspace().getContentTab().getDocumentCreatePage("Invoice", FileCreationFormPage.class);

        String iformId = "document_create:nxl_layout_Invoice_create";
        LayoutElement iform = new LayoutElement(driver, iformId);
        iform.getWidget("nxw_title").setInputValue("My invoice");
        iform.getWidget("nxw_amount").setInputValue("3");
        Locator.waitUntilEnabledAndClick(getCreateButton());

        // check the invoice data tab
        WebElement invoiceTab = AbstractPage.findElementWithTimeout(By.linkText("Invoice Data"));
        Locator.waitUntilEnabledAndClick(invoiceTab);
        // check content
        WebElement stateAndVersion = AbstractPage.findElementWithTimeout(By.className("lcAndVersion"));
        assertEquals("State received Version 0.0", stateAndVersion.getText());
        String tabFormId = "nxl_InvoiceData_tabLayout:nxw_sub0_initialForm:nxl_InvoiceDataLayout_view";
        LayoutElement itabform = new LayoutElement(driver, tabFormId);
        assertEquals("3", itabform.getWidget("nxw_amount").getValue(false));

        // go back on workspace, create a File doc
        getOrCreateTestWorkspace().getContentTab().getDocumentCreatePage("File", FileCreationFormPage.class);
        String fformId = "document_create:nxl_heading";
        LayoutElement fform = new LayoutElement(driver, fformId);
        fform.getWidget("nxw_title").setInputValue("My file");
        WebElement fcreateButton = getCreateButton();
        Locator.waitUntilEnabledAndClick(fcreateButton);
        // check the invoice data tab is not there
        try {
            driver.findElementByLinkText("Invoice Data");
            Assert.fail("Invoice Data tab should not be displayed");
        } catch (NoSuchElementException e) {
            // ok
        }
    }

    protected WebElement getCreateButton() {
        String buttonId = "document_create:nxw_documentCreateButtons_CREATE_DOCUMENT";
        return driver.findElement(By.id(buttonId));
    }

    @Test
    public void testWorkflow() throws Exception {
        doLog();
        getOrCreateTestWorkspace().getContentTab().getDocumentCreatePage("My test document", StudioDocumentPage.class);
        StudioDocumentPage page = getTestStudioDocument();
        page.createDocument("My creation title", true)
            .checkDocumentView("My creation title", true)
            .executeWorkflow("updateTitle")
            .checkDocumentView("Updated Title", false)
            .executeWorkflow("updateDescription")
            .checkDocumentView("Updated Title", "Updated Description", false);
    }

    @Test
    public void testCustomFilterSearch() throws Exception {
        doLog();

        SearchPage searchPage = asPage(DocumentBasePage.class).goToSearchPage();
        String selectedCV = searchPage.getSelectedSearch();
        Assert.assertEquals(SearchPage.DEFAULT_SEARCH, selectedCV);
        AjaxRequestManager arm = new AjaxRequestManager(driver);
        arm.begin();
        searchPage.selectSearch("testSearch");
        arm.end();
        Assert.assertEquals("testSearch", asPage(SearchPage.class).getSelectedSearch());

        logout();
    }

}