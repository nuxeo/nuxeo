/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
public abstract class Page {

    protected final static String SELECTED_TAB_CSS_CLASS = "selected";

    @Required
    @FindBy(linkText = "Overview")
    protected WebElement overviewTabLink;

    @Required
    @FindBy(linkText = "Reference")
    protected WebElement referenceTabLink;

    @Required
    @FindBy(linkText = "Preview")
    protected WebElement previewTabLink;

    public OverviewTab goToOverviewTab() {
        if (!overviewTabLink.findElement(By.xpath("ancestor::li")).getAttribute(
                "class").equals(SELECTED_TAB_CSS_CLASS)) {
            overviewTabLink.click();
        }
        return AbstractTest.asPage(OverviewTab.class);
    }

    public PreviewTab goToPreviewTab() {
        if (!previewTabLink.findElement(By.xpath("ancestor::li")).getAttribute(
                "class").equals(SELECTED_TAB_CSS_CLASS)) {
            previewTabLink.click();
        }
        return AbstractTest.asPage(PreviewTab.class);
    }

    public ReferenceTab goToReferenceTab() {
        if (!referenceTabLink.findElement(By.xpath("ancestor::li")).getAttribute(
                "class").equals(SELECTED_TAB_CSS_CLASS)) {
            referenceTabLink.click();
        }
        return AbstractTest.asPage(ReferenceTab.class);
    }

}
