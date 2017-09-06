/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ftest.cap;

import java.util.Calendar;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.contentView.ContentViewElement;
import org.nuxeo.functionaltests.fragment.EditResultColumnsForm;
import org.nuxeo.functionaltests.pages.DocumentBasePage;
import org.nuxeo.functionaltests.pages.DocumentBasePage.UserNotConnectedException;
import org.nuxeo.functionaltests.pages.search.DefaultSearchSubPage;
import org.nuxeo.functionaltests.pages.search.SearchPage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Non-regression tests for spurious fanxybox reopening regressions.
 *
 * @since 9.3
 */
public class ITFancyboxTest extends AbstractTest {

    /**
     * Non-regression tests for NXP-19722.
     */
    @Test
    public void testContentViewRefreshEditColumns() throws UserNotConnectedException {
        DocumentBasePage page = login().goToWorkspaces();
        ContentViewElement contentView = page.getContentTab().getContentView();
        contentView.refresh();

        assertFalse(page.isFancyBoxOpen());

        EditResultColumnsForm form = contentView.openEditColumnsFancybox();
        assertTrue(page.isFancyBoxOpen());
        form.cancel();
        assertFalse(page.isFancyBoxOpen());

        page = asPage(DocumentBasePage.class);
        contentView = page.getContentTab().getContentView();
        contentView.refresh();

        page = asPage(DocumentBasePage.class);
        assertFalse(page.isFancyBoxOpen());

        logout();
    }

    /**
     * Non-regression tests for NXP-22009.
     */
    @Test
    public void testSearchContentViewEditColumns() throws UserNotConnectedException {
        SearchPage page = login().goToSearchPage();
        ContentViewElement contentView = page.getContentView();

        assertFalse(page.isFancyBoxOpen());

        EditResultColumnsForm form = contentView.openEditRowsFancybox();
        assertTrue(page.isFancyBoxOpen());
        form.save();
        assertFalse(page.isFancyBoxOpen());

        page = asPage(SearchPage.class);

        // trigger an ajax re-rendering
        DefaultSearchSubPage searchLayoutSubPage = page.getDefaultSearch();
        int year = Calendar.getInstance().get(Calendar.YEAR);
        searchLayoutSubPage.selectCreatedAggregate(String.valueOf(year));

        page = asPage(SearchPage.class);
        assertFalse(page.isFancyBoxOpen());

        logout();
    }

}
