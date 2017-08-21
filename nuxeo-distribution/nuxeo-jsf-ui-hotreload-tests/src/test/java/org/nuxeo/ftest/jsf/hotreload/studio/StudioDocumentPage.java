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

import java.io.IOException;

import org.junit.Assert;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.forms.DateWidgetElement;
import org.nuxeo.functionaltests.forms.FileWidgetElement;
import org.nuxeo.functionaltests.forms.JSListWidgetElement;
import org.nuxeo.functionaltests.forms.LayoutElement;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.tabs.SummaryTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.WorkflowTabSubPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.junit.Assert.assertTrue;

/**
 * Page holding common methods to Studio test document pages.
 *
 * @since 9.3.
 */
public class StudioDocumentPage extends DocumentBasePage {

    @Required
    @FindBy(className = "content")
    WebElement content;

    // helper to generate screenshots with meaningful names
    protected String testId;

    public StudioDocumentPage(WebDriver driver) {
        super(driver);
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    protected StudioDocumentPage reload() {
        StudioDocumentPage reloaded = asPage(StudioDocumentPage.class);
        reloaded.setTestId(testId);
        return reloaded;
    }

    protected void fillForm(String prefix, String title, boolean fillAllMetadata) throws IOException {
        LayoutElement form = new LayoutElement(driver, prefix);
        form.getWidget("nxw_title").setInputValue(title);
        if (fillAllMetadata) {
            form.getWidget("nxw_simpleString").setInputValue("test string");
            form.getWidget("nxw_simpleDate", DateWidgetElement.class).setInputValue("Oct 11, 2012");

            FileWidgetElement file = form.getWidget("nxw_simpleBlob", FileWidgetElement.class);
            file.uploadTestFile("Studio test", ".txt", "blah blah");

            JSListWidgetElement list = form.getWidget("nxw_multiString", JSListWidgetElement.class);
            list.addNewElement();
            list.getSubWidget("nxw_sub0", 0, true).setInputValue("hihi");
            list.addNewElement();
            list.getSubWidget("nxw_sub0", 1, true).setInputValue("hoho");
        }
    }

    protected void checkForm(String prefix, String title, String description, boolean checkAllMetadata,
            boolean isEdit) {
        LayoutElement form = new LayoutElement(driver, prefix);
        Assert.assertEquals(title, form.getWidget("nxw_title").getValue(isEdit));
        if (description != null) {
            Assert.assertEquals(description, form.getWidget("nxw_description").getValue(isEdit));
        }
        if (checkAllMetadata) {
            Assert.assertEquals("test string", form.getWidget("nxw_simpleString").getValue(isEdit));
            Assert.assertEquals("Oct 11, 2012",
                    form.getWidget("nxw_simpleDate", DateWidgetElement.class).getValue(isEdit));

            FileWidgetElement file = form.getWidget("nxw_simpleBlob", FileWidgetElement.class);
            String filename = file.getFilename(isEdit);
            Assert.assertTrue(filename != null);
            Assert.assertTrue(filename.startsWith("Studio test"));
            Assert.assertTrue(filename.endsWith(".txt"));

            JSListWidgetElement list = form.getWidget("nxw_multiString", JSListWidgetElement.class);

            String subId = "nxw_sub0";
            Assert.assertEquals("hihi", list.getSubWidget(subId, 0, false).getValue(isEdit));
            Assert.assertEquals("hoho", list.getSubWidget(subId, 1, false).getValue(isEdit));
        }
    }

    public StudioDocumentPage checkDocumentView(String title, boolean otherMetadataFilled) {
        checkDocumentView(title, null, otherMetadataFilled);
        return reload();
    }

    public StudioDocumentPage checkDocumentView(String title, String description, boolean otherMetadataFilled) {
        String formId = "nxl_grid_summary_layout:nxw_summary_current_document_view_form:nxl_layout_TestDocument_view";
        checkForm(formId, title, description, otherMetadataFilled, false);
        return reload();
    }

    public StudioDocumentPage checkDocumentEditForm(String title, boolean otherMetadataFilled) {
        String formId = "document_edit:nxl_layout_TestDocument_edit";
        checkForm(formId, title, null, otherMetadataFilled, true);
        return reload();
    }

    public StudioDocumentPage createDocument(String title, boolean fillAllMetadata) throws IOException {
        String formId = "document_create:nxl_layout_TestDocument_create";
        fillForm(formId, title, fillAllMetadata);
        String buttonId = "document_create:nxw_documentCreateButtons_CREATE_DOCUMENT";
        WebElement createButton = driver.findElement(By.id(buttonId));
        Locator.waitUntilEnabledAndClick(createButton);
        return reload();
    }

    public StudioDocumentPage editDocument(String title, boolean fillAllMetadata) throws IOException {
        // create only with title first
        String formId = "document_edit:nxl_layout_TestDocument_edit";
        fillForm(formId, title, fillAllMetadata);
        String buttonId = "document_edit:nxw_documentEditButtons_EDIT_CURRENT_DOCUMENT";
        WebElement editButton = driver.findElement(By.id(buttonId));
        Locator.waitUntilEnabledAndClick(editButton);
        return reload();
    }

    public StudioDocumentPage executeWorkflow(String status) throws Exception {
        // start workflow
        SummaryTabSubPage summaryTabPage = getSummaryTab();
        summaryTabPage.selectItemInDropDownMenu(summaryTabPage.workflowSelector, "simpleWorkflow");
        summaryTabPage = asPage(SummaryTabSubPage.class);
        Locator.waitUntilEnabledAndClick(summaryTabPage.startWorkflowBtn);
        summaryTabPage = asPage(SummaryTabSubPage.class);
        String taskText = summaryTabPage.workflowTasksForm.getText();
        String expectedTaskText = "simpleWorkflow - Simple task";
        assertTrue(String.format("Task text is '%s'", taskText), taskText.contains(expectedTaskText));
        // click on the workflow tab
        WorkflowTabSubPage workflowTab = getWorkflow();
        workflowTab.endTask(status);
        return reload();
    }

}
