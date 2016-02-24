/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Gabriel Barata
 */

package org.nuxeo.functionaltests.pages.tabs;

import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * @since 8.2
 */
public class LocalConfigSubPage extends DocumentBasePage {

    public LocalConfigSubPage(WebDriver driver) {
        super(driver);
    }

    public LocalConfigSubPage enableDocumentContentConfig() {
        findElementAndWaitUntilEnabled(By.xpath("//a[contains(@id,'nxl_content_view_configuration_enable')]")).click();
        return asPage(LocalConfigSubPage.class);
    }

    public LocalConfigSubPage disableDocumentContentConfig() {
        findElementAndWaitUntilEnabled(By.xpath("//a[contains(@id,'nxl_content_view_configuration_disable')]")).click();
        return asPage(LocalConfigSubPage.class);
    }

    public LocalConfigSubPage addDocumentContentConfig(String docType, String contentView) {
        findElementAndWaitUntilEnabled(
                By.xpath("//a[contains(@id,'nxl_content_view_configuration:nxw_content_view_selection_add')]")).click();

        selectItemInDropDownMenu(
                driver.findElement(By.xpath("//select[contains(@id, 'nxw_complexListItem:nxw_docType')]")), docType);

        selectItemInDropDownMenu(
                driver.findElement(By.xpath("//select[contains(@id, 'nxw_complexListItem:nxw_contentView')]")),
                contentView);

        driver.findElement(By.xpath("//input[contains(@id,'nxl_content_view_configuration_save')]")).click();

        return asPage(LocalConfigSubPage.class);
    }
}
