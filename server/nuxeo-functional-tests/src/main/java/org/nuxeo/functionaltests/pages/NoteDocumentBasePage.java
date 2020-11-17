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
 *     <a href="mailto:glefevre@nuxeo.com">Gildas</a>
 */
package org.nuxeo.functionaltests.pages;

import org.nuxeo.functionaltests.pages.tabs.NoteSummaryTabSubPage;
import org.openqa.selenium.WebDriver;

/**
 * The Nuxeo note page.
 *
 * @since 5.9.4
 */
public class NoteDocumentBasePage extends DocumentBasePage {

    public NoteDocumentBasePage(WebDriver driver) {
        super(driver);
    }

    public NoteSummaryTabSubPage getNoteSummaryTab() {
        clickOnDocumentTabLink(summaryTabLink);
        return asPage(NoteSummaryTabSubPage.class);
    }
}
