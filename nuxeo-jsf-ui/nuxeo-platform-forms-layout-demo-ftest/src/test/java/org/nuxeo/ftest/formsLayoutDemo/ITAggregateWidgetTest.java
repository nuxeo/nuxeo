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
package org.nuxeo.ftest.formsLayoutDemo;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.formsLayoutDemo.page.HomePage;
import org.nuxeo.functionaltests.formsLayoutDemo.page.Page;
import org.openqa.selenium.By;

/**
 * @since 7.4
 */
public class ITAggregateWidgetTest extends AbstractTest {

    @Before
    public void before() throws Exception {
        RestHelper.createUser(TEST_USERNAME, TEST_USERNAME, "First Name", "Last Name", null, null, "members");
    }

    @After
    public void after() {
        RestHelper.cleanup();
    }

    protected void navigateTo(String pageId) {
        driver.get(HomePage.URL);
        get(HomePage.URL + pageId, Page.class);
    }

    protected void checkNoError() {
        Locator.waitForTextNotPresent(driver.findElement(By.xpath("//html")), "ERROR");
    }

    protected void checkLabel(String forValue, String label) {
        assertEquals(label, driver.findElement(By.xpath("//label[@for='" + forValue + "']")).getText());
    }

    @Test
    public void testSelectManyCheckboxDirectoryAggregateWidget() {
        navigateTo("selectManyCheckboxDirectoryAggregateWidget");
        checkNoError();
        String prefix1 = "selectManyCheckboxDirectoryAggregate_edit_form:nxl_selectManyCheckboxDirectoryAggregate:nxw_widget:";
        checkLabel(prefix1 + "0", "Eric Cartman (10)");
        checkLabel(prefix1 + "1", "Stan Marsh (5)");
        checkLabel(prefix1 + "2", "Kyle Broflovski (2)");
        String prefix2 = "selectManyCheckboxDirectoryAggregateLocalized_edit_form:nxl_selectManyCheckboxDirectoryAggregateLocalized:nxw_widget_1:";
        checkLabel(prefix2 + "0", "Australia/Oceania (10)");
        checkLabel(prefix2 + "1", "Antarctica (5)");
        checkLabel(prefix2 + "2", "Europe (2)");
        String prefix3 = "selectManyCheckboxDirectoryAggregateL10N_edit_form:nxl_selectManyCheckboxDirectoryAggregateL10N:nxw_widget_2:";
        checkLabel(prefix3 + "0", "Oceania (10)");
        checkLabel(prefix3 + "1", "Antarctica (5)");
        checkLabel(prefix3 + "2", "Europe (2)");
    }

    @Test
    public void testSelectManyListboxDirectoryAggregateWidget() {
        navigateTo("selectManyListboxDirectoryAggregateWidget");
        checkNoError();
        assertEquals(
                "Eric Cartman (10)\nStan Marsh (5)\nKyle Broflovski (2)",
                driver.findElement(
                        By.xpath("//form[@id='selectManyListboxDirectoryAggregate_edit_form']//table//tr/td[2]")).getText());
        assertEquals(
                "Australia/Oceania (10)\nAntarctica (5)\nEurope (2)",
                driver.findElement(
                        By.xpath("//form[@id='selectManyListboxDirectoryAggregateLocalized_edit_form']//table//tr/td[2]")).getText());
        assertEquals(
                "Oceania/Australia (10)\nAntarctica (5)\nEurope/France (2)",
                driver.findElement(
                        By.xpath("//form[@id='selectManyListboxDirectoryAggregateL10N_edit_form']//table//tr/td[2]")).getText());
    }

    @Test
    public void testSelectManyCheckboxAggregateWidget() {
        navigateTo("selectManyCheckboxAggregateWidget");
        checkNoError();
        String prefix1 = "selectManyCheckboxAggregate_edit_form:nxl_selectManyCheckboxAggregate:nxw_widget:";
        checkLabel(prefix1 + "0", "eric (10)");
        checkLabel(prefix1 + "1", "stan (5)");
        checkLabel(prefix1 + "2", "kyle (2)");
        String prefix2 = "selectManyCheckboxAggregateOptions_edit_form:nxl_selectManyCheckboxAggregateOptions:nxw_widget_1:";
        checkLabel(prefix2 + "0", "Eric Cartman (10)");
        checkLabel(prefix2 + "1", "Stan Marsh (5)");
        checkLabel(prefix2 + "2", "Kyle Broflovski (2)");
    }

    @Test
    public void testSelectManyListboxAggregateWidget() {
        navigateTo("selectManyListboxAggregateWidget");
        checkNoError();
        assertEquals(
                "eric (10)\nstan (5)\nkyle (2)",
                driver.findElement(By.xpath("//form[@id='selectManyListboxAggregate_edit_form']//table//tr/td[2]")).getText());
    }

    @Test
    public void testSelectManyCheckboxUserAggregateWidget() {
        navigateTo("selectManyCheckboxUserAggregateWidget");
        checkNoError();
        String prefix1 = "selectManyCheckboxUserAggregate_edit_form:nxl_selectManyCheckboxUserAggregate:nxw_widget:";
        checkLabel(prefix1 + "0", "Administrator (10)");
        checkLabel(prefix1 + "1", "First Name Last Name (3)");
        checkLabel(prefix1 + "2", "Members group (5)");
    }

    @Test
    public void testSelectManyListboxUserAggregateWidget() {
        navigateTo("selectManyListboxUserAggregateWidget");
        checkNoError();
        assertEquals("Administrator (10)\n" + "First Name Last Name (3)\n" + "Members group (5)",
                driver.findElement(By.xpath("//form[@id='selectManyListboxUserAggregate_edit_form']//table//tr/td[2]"))
                      .getText());
    }

}
