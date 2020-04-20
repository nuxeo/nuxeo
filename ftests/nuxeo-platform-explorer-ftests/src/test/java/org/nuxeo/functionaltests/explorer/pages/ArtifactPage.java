/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.functionaltests.explorer.pages;

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Page representing a selected artifact.
 *
 * @since 11.1
 */
public class ArtifactPage extends ArtifactHomePage {

    @Required
    @FindBy(xpath = "//section/article[@role='contentinfo']/h1")
    public WebElement header;

    @FindBy(xpath = "//section/article[@role='contentinfo']/div[contains(@class, 'include-in')]")
    public WebElement description;

    public ArtifactPage(WebDriver driver) {
        super(driver);
    }

}
