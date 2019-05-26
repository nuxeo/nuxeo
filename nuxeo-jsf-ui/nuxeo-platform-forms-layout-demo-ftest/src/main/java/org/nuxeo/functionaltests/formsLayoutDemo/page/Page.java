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

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 7.1
 */
public class Page {

    protected final static String SELECTED_TAB_CSS_CLASS = "selected";

    @Required
    @FindBy(linkText = "Overview")
    protected WebElement overviewTabLink;

    @Required
    @FindBy(linkText = "Reference")
    protected WebElement referenceTabLink;

    @FindBy(linkText = "Preview")
    protected WebElement previewTabLink;

    public OverviewTab goToOverviewTab() {
        if (!overviewTabLink.findElement(By.xpath("ancestor::li")).getAttribute("class").equals(SELECTED_TAB_CSS_CLASS)) {
            overviewTabLink.click();
        }
        return AbstractTest.asPage(OverviewTab.class);
    }

    /**
     * @since 7.4
     */
    public boolean hasPreviewTab() {
        return previewTabLink != null;
    }

    public PreviewTab goToPreviewTab() {
        if (!hasPreviewTab()) {
            throw new AssertionError("No preview tab");
        }
        if (!previewTabLink.findElement(By.xpath("ancestor::li")).getAttribute("class").equals(SELECTED_TAB_CSS_CLASS)) {
            previewTabLink.click();
        }
        return AbstractTest.asPage(PreviewTab.class);
    }

    public ReferenceTab goToReferenceTab() {
        if (!referenceTabLink.findElement(By.xpath("ancestor::li")).getAttribute("class").equals(SELECTED_TAB_CSS_CLASS)) {
            referenceTabLink.click();
        }
        return AbstractTest.asPage(ReferenceTab.class);
    }

}
