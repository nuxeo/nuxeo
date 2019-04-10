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
 *     Guillaume Renard
 *
 */

package org.nuxeo.functionaltests.formsLayoutDemo.page;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.formsLayoutDemo.page.standardWidgets.ListStandardWidgetPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 7.1
 */
public class HomePage {

    /**
     * @since 7.4
     */
    public static final String URL = AbstractTest.NUXEO_URL + "/layoutDemo/";

    @Required
    @FindBy(linkText = "Standard Widgets")
    public WebElement standardWidgetsPanelHeaderLink;

    @Required
    @FindBy(linkText = "Listing Widgets")
    public WebElement listingWidgetsPanelHeaderLink;

    @Required
    @FindBy(linkText = "Aggregate Widgets")
    public WebElement aggregateWidgetsPanelHeaderLink;

    @Required
    @FindBy(linkText = "Action Types")
    public WebElement actionTypesPanelHeaderLink;

    @Required
    @FindBy(linkText = "Advanced Widgets")
    public WebElement advancedWidgetsPanelHeaderLink;

    public ListStandardWidgetPage goToListStandardWidget() {
        return AbstractTest.asPage(ListStandardWidgetPage.class);
    }

    public Map<String, List<String>> getMenuItems() {
        Map<String, List<String>> items = new LinkedHashMap<String, List<String>>();
        List<WebElement> boxes = Locator.findElementsWithTimeout(By.xpath("//div[@class='layoutDemoLeftMenu']/div"));
        for (WebElement box : boxes) {
            WebElement boxHead = box.findElement(By.xpath(".//h3"));
            List<String> titles = new ArrayList<String>();
            for (WebElement item : box.findElements(By.xpath(".//li"))) {
                titles.add(item.getText());
            }
            items.put(boxHead.getText(), titles);
        }
        return items;
    }
}
