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
 *     dmetzler
 *     akervern
 */
package org.nuxeo.functionaltests.pages.wizard;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.5
 */
public class ConnectRegistrationPage extends ConnectWizardPage {

    public ConnectRegistrationPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public String getTitle() {
        WebElement title = findElementWithTimeout(By.xpath("//h1"));
        return title.getText().trim();
    }

    public void submit() {
        WebElement input = findElementWithTimeout(By.xpath("//input[@value=\"Continue\"]"));
        input.submit();
    }
}
