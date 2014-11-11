/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.functionaltests.pages;

import org.nuxeo.functionaltests.pages.tabs.FileSummaryTabSubPage;
import org.openqa.selenium.WebDriver;

/**
 * @author Sun Seng David TAN <stan@nuxeo.com>
 *
 */
public class FileDocumentBasePage extends DocumentBasePage {

    public FileDocumentBasePage(WebDriver driver) {
        super(driver);
    }

    public FileSummaryTabSubPage getFileSummaryTab() {
        clickOnLinkIfNotSelected(summaryTabLink);
        return asPage(FileSummaryTabSubPage.class);
    }

}
