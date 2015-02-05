/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.formsLayoutDemo.page;

import static org.junit.Assert.assertEquals;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.forms.LayoutElement;
import org.nuxeo.functionaltests.forms.ListWidgetElement;
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

    public static String PAGE_PATH = AbstractTest.NUXEO_URL + "/layoutDemo/testDocumentValidation";

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

    protected LayoutElement getLayout() {
        return new LayoutElement(driver, "validateForm:nxl_layout_demo_validation_sample");
    }

    protected void submitWhileTyping() {
        // submit to force immediate validation of fields launching validation on submit
        submit();
    }

    public void fillLayoutInvalid() {
        LayoutElement l = getLayout();
        l.getWidget("nxw_groupCode").setInputValue("invalid string");
        submitWhileTyping();
        l.getWidget("nxw_manager:nxw_firstname").setInputValue("");
        submitWhileTyping();
        l.getWidget("nxw_manager:nxw_lastname").setInputValue("AA");
        ListWidgetElement slist = l.getWidget("nxw_roles", ListWidgetElement.class);
        slist.addNewElement();
        ListWidgetElement list = l.getWidget("nxw_users", ListWidgetElement.class);
        list.addNewElement();
        list.getSubWidget("nxw_ln", 0, false).setInputValue("AA");
    }

    public void fillLayoutValid() {
        LayoutElement l = getLayout();
        l.getWidget("nxw_groupCode").setInputValue("2");
        submitWhileTyping();
        l.getWidget("nxw_manager:nxw_firstname").setInputValue("AA");
        submitWhileTyping();
        l.getWidget("nxw_manager:nxw_lastname").setInputValue("Aa");
        ListWidgetElement slist = l.getWidget("nxw_roles", ListWidgetElement.class);
        slist.addNewElement();
        slist.getSubWidget("nxw_role", 0, false).setInputValue("AA");
        ListWidgetElement list = l.getWidget("nxw_users", ListWidgetElement.class);
        list.addNewElement();
        list.getSubWidget("nxw_fn", 0, false).setInputValue("AA");
        list.getSubWidget("nxw_ln", 0, false).setInputValue("Aa");
    }

    public void checkLayoutEmpty() {
        LayoutElement l = getLayout();
        assertEquals("", l.getWidget("nxw_title").getMessageValue());
        assertEquals("Value is required", l.getWidget("nxw_groupCode").getMessageValue());
        assertEquals("This value must match the format \".*\\S.*\".",
                l.getWidget("nxw_manager:nxw_firstname").getMessageValue());
        assertEquals("This value must match the format \"[A-Z][a-z '-]+\".",
                l.getWidget("nxw_manager:nxw_lastname").getMessageValue());
        ListWidgetElement slist = l.getWidget("nxw_roles", ListWidgetElement.class);
        assertEquals("", slist.getMessageValue());
        ListWidgetElement list = l.getWidget("nxw_users", ListWidgetElement.class);
        assertEquals("", list.getMessageValue());
    }

    public void checkLayoutInvalid() {
        LayoutElement l = getLayout();
        assertEquals("", l.getWidget("nxw_title").getMessageValue());
        assertEquals("'invalid string' is not a number. Example: 99", l.getWidget("nxw_groupCode").getMessageValue());
        assertEquals("This value must match the format \".*\\S.*\".",
                l.getWidget("nxw_manager:nxw_firstname").getMessageValue());
        assertEquals("This value must match the format \"[A-Z][a-z '-]+\".",
                l.getWidget("nxw_manager:nxw_lastname").getMessageValue());
        ListWidgetElement slist = l.getWidget("nxw_roles", ListWidgetElement.class);
        assertEquals("", slist.getMessageValue());
        assertEquals("This value must match the format \"[a-zA-Z0-9]+\".",
                slist.getSubWidget("nxw_role", 0, false).getMessageValue());
        ListWidgetElement list = l.getWidget("nxw_users", ListWidgetElement.class);
        assertEquals("", slist.getMessageValue());
        assertEquals("This value must match the format \".*\\S.*\".",
                list.getSubWidget("nxw_fn", 0, false).getMessageValue());
        assertEquals("This value must match the format \"[A-Z][a-z '-]+\".",
                list.getSubWidget("nxw_ln", 0, false).getMessageValue());
    }

    public void checkLayoutValid() {
        LayoutElement l = getLayout();
        assertEquals("", l.getWidget("nxw_title").getMessageValue());
        assertEquals("", l.getWidget("nxw_groupCode").getMessageValue());
        assertEquals("", l.getWidget("nxw_manager:nxw_firstname").getMessageValue());
        assertEquals("", l.getWidget("nxw_manager:nxw_lastname").getMessageValue());
        ListWidgetElement slist = l.getWidget("nxw_roles", ListWidgetElement.class);
        assertEquals("", slist.getMessageValue());
        assertEquals("", slist.getSubWidget("nxw_role", 0, false).getMessageValue());
        ListWidgetElement list = l.getWidget("nxw_users", ListWidgetElement.class);
        assertEquals("", list.getMessageValue());
        assertEquals("", list.getSubWidget("nxw_fn", 0, false).getMessageValue());
        assertEquals("", list.getSubWidget("nxw_ln", 0, false).getMessageValue());
    }

}
