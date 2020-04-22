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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Page representing home for a given artifact type.
 *
 * @since 11.1
 */
public class DistributionHeaderFragment extends AbstractExplorerPage {

    @Required
    @FindBy(linkText = "Extension points")
    public WebElement extensionPoints;

    @Required
    @FindBy(linkText = "Contributions")
    public WebElement contributions;

    @Required
    @FindBy(linkText = "Services")
    public WebElement services;

    @Required
    @FindBy(linkText = "Operations")
    public WebElement operations;

    @Required
    @FindBy(linkText = "Components")
    public WebElement components;

    @Required
    @FindBy(linkText = "Bundles")
    public WebElement bundles;

    public DistributionHeaderFragment(WebDriver driver) {
        super(driver);
    }

    protected boolean isSelected(WebElement element) {
        WebElement parent = element.findElement(By.xpath("./.."));
        return "selected".equals(parent.getAttribute("class"));
    }

    public DistributionHeaderFragment navigateTo(WebElement element) {
        clickOn(element);
        return asPage(DistributionHeaderFragment.class);
    }

    @Override
    public void check() {
        checkUnselectedTab(extensionPoints);
        checkUnselectedTab(contributions);
        checkUnselectedTab(services);
        checkUnselectedTab(operations);
        checkUnselectedTab(components);
        checkUnselectedTab(bundles);
    }

    public void checkSelectedTab(WebElement element) {
        assertTrue(isSelected(element));
    }

    public void checkUnselectedTab(WebElement element) {
        assertFalse(isSelected(element));
    }

}