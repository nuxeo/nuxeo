/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.functionaltests.pages;

import org.nuxeo.functionaltests.pages.tabs.FilePublishTabSubPage;
import org.nuxeo.functionaltests.pages.tabs.FileSummaryTabSubPage;
import org.openqa.selenium.WebDriver;

/**
 * @author Sun Seng David TAN <stan@nuxeo.com>
 */
public class FileDocumentBasePage extends DocumentBasePage {

    public FileDocumentBasePage(WebDriver driver) {
        super(driver);
    }

    public FileSummaryTabSubPage getFileSummaryTab() {
        clickOnDocumentTabLink(summaryTabLink);
        return asPage(FileSummaryTabSubPage.class);
    }

    public FilePublishTabSubPage getFilePublishTab() {
        clickOnDocumentTabLink(publishTabLink);
        return asPage(FilePublishTabSubPage.class);
    }

}
