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
package org.nuxeo.functionaltests.formsLayoutDemo.page;

import static org.junit.Assert.assertEquals;

import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.forms.JSListWidgetElement;
import org.nuxeo.functionaltests.forms.LayoutElement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Represents the page used for testing validation.
 *
 * @since 7.2
 */
public class ValidationPage {

    /**
     * @since 7.4
     */
    public final static String VALUE_REQUIRED = "Value is required.";

    public static String PAGE_PATH = HomePage.URL + "testDocumentValidation";

    protected WebDriver driver;

    @Required
    @FindBy(id = "validateForm:validateButton")
    protected WebElement submitButton;

    @Required
    @FindBy(id = "validateForm:resetButton")
    protected WebElement resetButton;

    public ValidationPage(WebDriver driver) {
        super();
        this.driver = driver;
    }

    public void resetDemoDocument() {
        resetButton.click();
    }

    public void submit() {
        AjaxRequestManager am = new AjaxRequestManager(driver);
        am.watchAjaxRequests();
        submitButton.click();
        am.waitForAjaxRequests();
    }

    protected String getBody() {
        WebElement body = driver.findElement(By.tagName("body"));
        return body.getText();
    }

    public boolean hasGlobalError() {
        String body = getBody();
        return body != null && body.contains("Please correct errors");
    }

    public boolean hasValidated() {
        String body = getBody();
        return body != null && body.contains("Validation done");
    }

    public LayoutElement getLayout() {
        return new LayoutElement(driver, "validateForm:nxl_layout_demo_validation_sample");
    }

    protected void submitWhileTyping() {
        // submit to force immediate validation of fields launching validation on submit
        submit();
    }

    public void fillLayoutInvalid() {
        LayoutElement l = getLayout();
        l.getWidget("nxw_groupCode").setInputValue("-25");
        submitWhileTyping();
        l.getWidget("nxw_manager:nxw_firstname").setInputValue("  ");
        submitWhileTyping();
        l.getWidget("nxw_manager:nxw_lastname").setInputValue("AA");
        JSListWidgetElement slist = l.getWidget("nxw_roles", JSListWidgetElement.class);
        slist.addNewElement();
        slist.getSubWidget("nxw_role", 0).setInputValue("  ");
        JSListWidgetElement list = l.getWidget("nxw_users", JSListWidgetElement.class);
        list.addNewElement();
        list.getSubWidget("nxw_fn", 0).setInputValue("  ");
        list.getSubWidget("nxw_ln", 0).setInputValue("AA");
        JSListWidgetElement llist = l.getWidget("nxw_listOfListsWidget", JSListWidgetElement.class);
        llist.addNewElement();
        JSListWidgetElement subList = llist.getSubWidget("nxw_stringListItem", 0, JSListWidgetElement.class, false);
        subList.addNewElement();
        subList.getSubWidget("nxw_stringListSubItem", 0).setInputValue("aaa");
    }

    /**
     * @since 10.1
     */
    public void fillLayoutInvalidWithMissingRequiredField() {
        LayoutElement l = getLayout();
        l.getWidget("nxw_title").setInputValue(" ");
        l.getWidget("nxw_groupCode").setInputValue("123");
        submitWhileTyping();
        l.getWidget("nxw_manager:nxw_firstname").setInputValue("John");
        submitWhileTyping();
        l.getWidget("nxw_manager:nxw_lastname").setInputValue("Doe");
        JSListWidgetElement slist = l.getWidget("nxw_roles", JSListWidgetElement.class);
        slist.addNewElement();
        slist.getSubWidget("nxw_role", 0).setInputValue("myRole");
        JSListWidgetElement list = l.getWidget("nxw_users", JSListWidgetElement.class);
        list.addNewElement();
        list.getSubWidget("nxw_fn", 0).setInputValue("test");
        list.getSubWidget("nxw_ln", 0).setInputValue("user");
        JSListWidgetElement llist = l.getWidget("nxw_listOfListsWidget", JSListWidgetElement.class);
        llist.addNewElement();
        llist.addNewElement();
        JSListWidgetElement subList1 = llist.getSubWidget("nxw_stringListItem", 0, JSListWidgetElement.class, false);
        subList1.addNewElement();
        subList1.getSubWidget("nxw_stringListSubItem", 0).setInputValue("Aaa");
        JSListWidgetElement subList2 = llist.getSubWidget("nxw_stringListItem", 1, JSListWidgetElement.class, false);
        subList2.addNewElement();
        subList2.getSubWidget("nxw_stringListSubItem", 0).setInputValue("Bbb");
    }


    public void fillLayoutValid() {
        LayoutElement l = getLayout();
        l.getWidget("nxw_groupCode").setInputValue("2");
        submitWhileTyping();
        l.getWidget("nxw_manager:nxw_firstname").setInputValue("AA");
        submitWhileTyping();
        l.getWidget("nxw_manager:nxw_lastname").setInputValue("Aa");
        JSListWidgetElement slist = l.getWidget("nxw_roles", JSListWidgetElement.class);
        slist.addNewElement();
        slist.getSubWidget("nxw_role", 0).setInputValue("AA");
        JSListWidgetElement list = l.getWidget("nxw_users", JSListWidgetElement.class);
        list.addNewElement();
        list.getSubWidget("nxw_fn", 0).setInputValue("AA");
        list.getSubWidget("nxw_ln", 0).setInputValue("Aa");
        JSListWidgetElement llist = l.getWidget("nxw_listOfListsWidget", JSListWidgetElement.class);
        llist.addNewElement();
        JSListWidgetElement subList = llist.getSubWidget("nxw_stringListItem", 0, JSListWidgetElement.class, false);
        subList.addNewElement();
        subList.getSubWidget("nxw_stringListSubItem", 0).setInputValue("Aaa");
    }

    public void checkLayoutEmpty() {
        LayoutElement l = getLayout();
        assertEquals("", l.getWidget("nxw_title").getMessageValue());
        assertEquals(VALUE_REQUIRED, l.getWidget("nxw_groupCode").getMessageValue());
        assertEquals(VALUE_REQUIRED, l.getWidget("nxw_manager:nxw_firstname").getMessageValue());
        assertEquals(VALUE_REQUIRED, l.getWidget("nxw_manager:nxw_lastname").getMessageValue());
        JSListWidgetElement slist = l.getWidget("nxw_roles", JSListWidgetElement.class);
        assertEquals("", slist.getMessageValue());
        JSListWidgetElement list = l.getWidget("nxw_users", JSListWidgetElement.class);
        assertEquals("", list.getMessageValue());
    }

    public void checkLayoutInvalid() {
        LayoutElement l = getLayout();
        assertEquals("", l.getWidget("nxw_title").getMessageValue());
        assertEquals("The group code must be a positive integer.", l.getWidget("nxw_groupCode").getMessageValue());
        assertEquals("This value must match the format \".*\\S.*\".",
                l.getWidget("nxw_manager:nxw_firstname").getMessageValue());
        assertEquals("The manager's lastname must start with an uppercase character.",
                l.getWidget("nxw_manager:nxw_lastname").getMessageValue());
        JSListWidgetElement slist = l.getWidget("nxw_roles", JSListWidgetElement.class);
        assertEquals("", slist.getMessageValue());
        assertEquals("This value must match the format \"[a-zA-Z0-9]+\".",
                slist.getSubWidget("nxw_role", 0).getMessageValue());
        JSListWidgetElement list = l.getWidget("nxw_users", JSListWidgetElement.class);
        assertEquals("", slist.getMessageValue());
        assertEquals("A user's firstname must contain at least one character.",
                list.getSubWidget("nxw_fn", 0).getMessageValue());
        assertEquals("This value must match the format \"[A-Z][a-z '-]+\".",
                list.getSubWidget("nxw_ln", 0).getMessageValue());
        JSListWidgetElement llist = l.getWidget("nxw_listOfListsWidget", JSListWidgetElement.class);
        JSListWidgetElement subList = llist.getSubWidget("nxw_stringListItem", 0, JSListWidgetElement.class, false);
        assertEquals("This value must match the format \"[A-Z][a-z '-]+\".",
                subList.getSubWidget("nxw_stringListSubItem", 0).getMessageValue());
    }

    public void checkLayoutValid() {
        LayoutElement l = getLayout();
        assertEquals("", l.getWidget("nxw_title").getMessageValue());
        assertEquals("", l.getWidget("nxw_groupCode").getMessageValue());
        assertEquals("", l.getWidget("nxw_manager:nxw_firstname").getMessageValue());
        assertEquals("", l.getWidget("nxw_manager:nxw_lastname").getMessageValue());
        JSListWidgetElement slist = l.getWidget("nxw_roles", JSListWidgetElement.class);
        assertEquals("", slist.getMessageValue());
        assertEquals("", slist.getSubWidget("nxw_role", 0).getMessageValue());
        JSListWidgetElement list = l.getWidget("nxw_users", JSListWidgetElement.class);
        assertEquals("", list.getMessageValue());
        assertEquals("", list.getSubWidget("nxw_fn", 0).getMessageValue());
        assertEquals("", list.getSubWidget("nxw_ln", 0).getMessageValue());
        JSListWidgetElement llist = l.getWidget("nxw_listOfListsWidget", JSListWidgetElement.class);
        JSListWidgetElement subList = llist.getSubWidget("nxw_stringListItem", 0, JSListWidgetElement.class, false);
        assertEquals("", subList.getSubWidget("nxw_stringListSubItem", 0).getMessageValue());
    }

}
