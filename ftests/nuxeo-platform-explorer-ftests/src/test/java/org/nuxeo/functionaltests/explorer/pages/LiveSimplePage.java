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

import static org.junit.Assert.assertTrue;

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Page representing home at /site/distribution.
 *
 * @since 11.1
 */
public class LiveSimplePage extends AbstractExplorerPage {

    public static final String URL = "/site/distribution/adm/";

    @Required
    @FindBy(xpath = "//h1")
    public WebElement header;

    public LiveSimplePage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void check() {
        checkTitle("Nuxeo Platform Explorer");
        assertTrue(header.getText(), header.getText().startsWith("Your current Nuxeo Distribution is"));
    }

}
