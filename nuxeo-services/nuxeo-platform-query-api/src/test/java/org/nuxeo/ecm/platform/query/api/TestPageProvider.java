/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.query.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/**
 * @author Anahide Tchertchian
 */
public class TestPageProvider {

    @Test
    public void testPageProvider() {
        checkStandardPageProvider(new MockPageProvider(5, 13, true), true);
    }

    @Test
    public void testPageProviderNoResultsCount() {
        checkStandardPageProvider(new MockPageProvider(5, 13, false), false);
    }

    protected void checkStandardPageProvider(
            PageProvider<MockPagedListItem> provider, boolean knowsResultCount) {

        checkFirstPage(provider, knowsResultCount);
        provider.nextPage();
        checkSecondPage(provider, knowsResultCount);
        provider.nextPage();
        checkThirdPage(provider, knowsResultCount);

        // go back to first page and test again
        provider.firstPage();
        checkFirstPage(provider, knowsResultCount);

        // go to last page and test again
        if (knowsResultCount) {
            assertTrue(provider.isLastPageAvailable());
            provider.lastPage();
        } else {
            assertFalse(provider.isLastPageAvailable());
            // last page has no effect on result providers with no result count
            provider.setCurrentPage(2);
        }
        checkThirdPage(provider, knowsResultCount);
        provider.previousPage();
        checkSecondPage(provider, knowsResultCount);
        provider.previousPage();
        checkFirstPage(provider, knowsResultCount);

        // check current entry
        MockPagedListItem currentEntry = provider.getCurrentEntry();
        currentEntry = provider.getCurrentEntry();
        assertNotNull(currentEntry);
        assertEquals(0, currentEntry.getPosition());
        assertFalse(provider.isPreviousEntryAvailable());
        assertTrue(provider.isNextEntryAvailable());
        provider.nextEntry();
        currentEntry = provider.getCurrentEntry();
        assertNotNull(currentEntry);
        assertEquals(1, currentEntry.getPosition());
        assertTrue(provider.isPreviousEntryAvailable());
        assertTrue(provider.isNextEntryAvailable());

        // next till the end of the page
        provider.nextEntry();
        currentEntry = provider.getCurrentEntry();
        assertNotNull(currentEntry);
        assertEquals(2, currentEntry.getPosition());
        assertTrue(provider.isPreviousEntryAvailable());
        assertTrue(provider.isNextEntryAvailable());
        provider.nextEntry();
        currentEntry = provider.getCurrentEntry();
        assertNotNull(currentEntry);
        assertEquals(3, currentEntry.getPosition());
        assertTrue(provider.isPreviousEntryAvailable());
        assertTrue(provider.isNextEntryAvailable());
        provider.nextEntry();
        currentEntry = provider.getCurrentEntry();
        assertNotNull(currentEntry);
        assertEquals(4, currentEntry.getPosition());
        assertTrue(provider.isPreviousEntryAvailable());

        if (knowsResultCount) {
            assertTrue(provider.isNextEntryAvailable());

            // switch to next page
            assertEquals(0, provider.getCurrentPageIndex());
            provider.nextEntry();
            currentEntry = provider.getCurrentEntry();
            assertNotNull(currentEntry);
            assertEquals(1, provider.getCurrentPageIndex());
            assertEquals(5, currentEntry.getPosition());

            // switch to previous page
            assertTrue(provider.isPreviousEntryAvailable());
            assertTrue(provider.isNextEntryAvailable());
            provider.previousEntry();
            currentEntry = provider.getCurrentEntry();
            assertNotNull(currentEntry);
            assertEquals(0, provider.getCurrentPageIndex());
            assertEquals(4, currentEntry.getPosition());

            // check isNextEntryAvailable returning false
            assertTrue(provider.isLastPageAvailable());
            provider.lastPage();
            assertTrue(provider.isNextEntryAvailable());
            provider.nextEntry();
            assertTrue(provider.isNextEntryAvailable());
            provider.nextEntry();
            assertFalse(provider.isNextEntryAvailable());
            currentEntry = provider.getCurrentEntry();
            assertNotNull(currentEntry);
            assertEquals(2, provider.getCurrentPageIndex());
            assertEquals(12, currentEntry.getPosition());
            provider.nextEntry();
            currentEntry = provider.getCurrentEntry();
            assertNotNull(currentEntry);
            assertEquals(2, provider.getCurrentPageIndex());
            assertEquals(12, currentEntry.getPosition());
        }

    }

    protected void checkFirstPage(PageProvider<MockPagedListItem> provider,
            boolean knowsResultCount) {
        assertEquals(5, provider.getPageSize());
        if (knowsResultCount) {
            assertEquals(13, provider.getResultsCount());
            assertEquals(3, provider.getNumberOfPages());
        } else {
            assertEquals(PageProvider.UNKNOWN_SIZE_AFTER_QUERY,
                    provider.getResultsCount());
            assertEquals(0, provider.getNumberOfPages());
        }
        List<MockPagedListItem> currentItems = provider.getCurrentPage();
        assertEquals(5, currentItems.size());
        assertEquals(0, currentItems.get(0).getPosition());
        assertEquals(4, currentItems.get(4).getPosition());
        assertEquals(0, provider.getCurrentPageIndex());
        assertEquals(0, provider.getCurrentPageOffset());
        assertEquals(5, provider.getCurrentPageSize());
        if (knowsResultCount) {
            assertEquals("1/3", provider.getCurrentPageStatus());
        } else {
            assertEquals("1", provider.getCurrentPageStatus());
        }
        assertFalse(provider.isPreviousPageAvailable());
        assertTrue(provider.isNextPageAvailable());
    }

    protected void checkSecondPage(PageProvider<MockPagedListItem> provider,
            boolean knowsResultCount) {
        assertEquals(5, provider.getPageSize());
        if (knowsResultCount) {
            assertEquals(13, provider.getResultsCount());
            assertEquals(3, provider.getNumberOfPages());
        } else {
            assertEquals(PageProvider.UNKNOWN_SIZE_AFTER_QUERY,
                    provider.getResultsCount());
            assertEquals(0, provider.getNumberOfPages());
        }
        List<MockPagedListItem> currentItems = provider.getCurrentPage();
        assertEquals(5, currentItems.size());
        assertEquals(5, currentItems.get(0).getPosition());
        assertEquals(9, currentItems.get(4).getPosition());
        assertEquals(1, provider.getCurrentPageIndex());
        assertEquals(5, provider.getCurrentPageOffset());
        assertEquals(5, provider.getCurrentPageSize());
        if (knowsResultCount) {
            assertEquals("2/3", provider.getCurrentPageStatus());
        } else {
            assertEquals("2", provider.getCurrentPageStatus());
        }
        assertTrue(provider.isPreviousPageAvailable());
        assertTrue(provider.isNextPageAvailable());
    }

    protected void checkThirdPage(PageProvider<MockPagedListItem> provider,
            boolean knowsResultCount) {
        assertEquals(5, provider.getPageSize());
        if (knowsResultCount) {
            assertEquals(13, provider.getResultsCount());
            assertEquals(3, provider.getNumberOfPages());
        } else {
            assertEquals(PageProvider.UNKNOWN_SIZE_AFTER_QUERY,
                    provider.getResultsCount());
            assertEquals(0, provider.getNumberOfPages());
        }
        List<MockPagedListItem> currentItems = provider.getCurrentPage();
        assertEquals(3, currentItems.size());
        assertEquals(10, currentItems.get(0).getPosition());
        assertEquals(12, currentItems.get(2).getPosition());
        assertEquals(2, provider.getCurrentPageIndex());
        assertEquals(10, provider.getCurrentPageOffset());
        assertEquals(3, provider.getCurrentPageSize());
        if (knowsResultCount) {
            assertEquals("3/3", provider.getCurrentPageStatus());
        } else {
            assertEquals("3", provider.getCurrentPageStatus());
        }
        assertTrue(provider.isPreviousPageAvailable());
        if (knowsResultCount) {
            assertFalse(provider.isNextPageAvailable());
        } else {
            // keep one more page before saying it's not available when there
            // is no result count
            assertTrue(provider.isNextPageAvailable());
            provider.nextPage();
            assertFalse(provider.isNextPageAvailable());
            // go back one page for consistency
            provider.previousPage();
        }
    }

    @Test
    public void testPageProviderWithPageSizeSameThanResultSize() {
        checkPageProviderWithPageSizeSameThanResultSize(new MockPageProvider(5,
                10, true), true);
    }

    @Test
    public void testPageProviderWithPageSizeSameThanResultSizeNoResultsCount() {
        checkPageProviderWithPageSizeSameThanResultSize(new MockPageProvider(5,
                10, false), false);
    }

    protected void checkPageProviderWithPageSizeSameThanResultSize(
            PageProvider<MockPagedListItem> provider, boolean knowsResultCount) {
        assertEquals(5, provider.getPageSize());
        if (knowsResultCount) {
            assertEquals(10, provider.getResultsCount());
            assertEquals(2, provider.getNumberOfPages());
        } else {
            assertEquals(PageProvider.UNKNOWN_SIZE_AFTER_QUERY,
                    provider.getResultsCount());
            assertEquals(0, provider.getNumberOfPages());
        }
        assertEquals(0, provider.getCurrentPageIndex());
        List<MockPagedListItem> currentItems = provider.getCurrentPage();
        assertEquals(5, currentItems.size());
        assertEquals(0, currentItems.get(0).getPosition());
        assertEquals(4, currentItems.get(4).getPosition());
        assertEquals(0, provider.getCurrentPageIndex());
        assertEquals(0, provider.getCurrentPageOffset());
        assertEquals(5, provider.getCurrentPageSize());
        if (knowsResultCount) {
            assertEquals("1/2", provider.getCurrentPageStatus());
        } else {
            assertEquals("1", provider.getCurrentPageStatus());
        }
        assertFalse(provider.isPreviousPageAvailable());
        assertTrue(provider.isNextPageAvailable());
        assertFalse(provider.isPreviousEntryAvailable());
        assertTrue(provider.isNextEntryAvailable());
        if (knowsResultCount) {
            assertTrue(provider.isLastPageAvailable());
            provider.lastPage();
            assertEquals(1, provider.getCurrentPageIndex());
        } else {
            assertFalse(provider.isLastPageAvailable());
        }
        provider.firstPage();
        assertEquals(0, provider.getCurrentPageIndex());
    }

    @Test
    public void testPageProviderWithoutPagination() {
        checkPageProviderWithoutPagination(new MockPageProvider(0, 13, true),
                true);
    }

    @Test
    public void testPageProviderWithoutPaginationNoResultsCount() {
        checkPageProviderWithoutPagination(new MockPageProvider(0, 13, false),
                false);
    }

    public void checkPageProviderWithoutPagination(
            PageProvider<MockPagedListItem> provider, boolean knowsResultCount) {
        assertEquals(0, provider.getPageSize());
        if (knowsResultCount) {
            assertEquals(13, provider.getResultsCount());
        } else {
            assertEquals(PageProvider.UNKNOWN_SIZE_AFTER_QUERY,
                    provider.getResultsCount());
        }
        assertEquals(1, provider.getNumberOfPages());
        assertEquals(0, provider.getCurrentPageIndex());
        List<MockPagedListItem> currentItems = provider.getCurrentPage();
        assertEquals(13, currentItems.size());
        assertEquals(0, currentItems.get(0).getPosition());
        assertEquals(12, currentItems.get(12).getPosition());
        assertEquals(0, provider.getCurrentPageIndex());
        assertEquals(0, provider.getCurrentPageOffset());
        assertEquals(13, provider.getCurrentPageSize());
        assertEquals("1/1", provider.getCurrentPageStatus());
        assertFalse(provider.isPreviousPageAvailable());
        assertFalse(provider.isNextPageAvailable());
        assertFalse(provider.isPreviousEntryAvailable());
        assertTrue(provider.isNextEntryAvailable());
        // no last page when no pagination
        assertFalse(provider.isLastPageAvailable());
        provider.lastPage();
        assertEquals(0, provider.getCurrentPageIndex());
        provider.firstPage();
        assertEquals(0, provider.getCurrentPageIndex());
    }

    @Test
    public void testMinMaxPageSize() {
        // only set page size => should fallback on default max page size
        assertEquals(20, getMinMaxPageSize(Long.valueOf(20), null));
        assertEquals(PageProvider.DEFAULT_MAX_PAGE_SIZE,
                getMinMaxPageSize(Long.valueOf(2000), null));
        assertEquals(PageProvider.DEFAULT_MAX_PAGE_SIZE,
                getMinMaxPageSize(Long.valueOf(3000), null));
        assertEquals(PageProvider.DEFAULT_MAX_PAGE_SIZE,
                getMinMaxPageSize(null, null));
        assertEquals(PageProvider.DEFAULT_MAX_PAGE_SIZE,
                getMinMaxPageSize(Long.valueOf(0), null));
        assertEquals(PageProvider.DEFAULT_MAX_PAGE_SIZE,
                getMinMaxPageSize(Long.valueOf(-1), null));
        // set max page size to 200
        assertEquals(20, getMinMaxPageSize(Long.valueOf(20), Long.valueOf(200)));
        assertEquals(200,
                getMinMaxPageSize(Long.valueOf(200), Long.valueOf(200)));
        assertEquals(200,
                getMinMaxPageSize(Long.valueOf(500), Long.valueOf(200)));
        assertEquals(200, getMinMaxPageSize(null, Long.valueOf(200)));
        assertEquals(200, getMinMaxPageSize(Long.valueOf(0), Long.valueOf(200)));
        assertEquals(200,
                getMinMaxPageSize(Long.valueOf(-1), Long.valueOf(200)));
        // set max page size to 0 (unlimited)
        assertEquals(20, getMinMaxPageSize(Long.valueOf(20), Long.valueOf(0)));
        assertEquals(200, getMinMaxPageSize(Long.valueOf(200), Long.valueOf(0)));
        assertEquals(500, getMinMaxPageSize(Long.valueOf(500), Long.valueOf(0)));
        assertEquals(0, getMinMaxPageSize(null, Long.valueOf(0)));
        assertEquals(0, getMinMaxPageSize(Long.valueOf(0), Long.valueOf(0)));
        assertEquals(0, getMinMaxPageSize(Long.valueOf(-1), Long.valueOf(0)));
        // extreme case that should never happen (set max page size to negative
        // number)
        assertEquals(20, getMinMaxPageSize(Long.valueOf(20), Long.valueOf(-1)));
        assertEquals(PageProvider.DEFAULT_MAX_PAGE_SIZE,
                getMinMaxPageSize(Long.valueOf(2000), Long.valueOf(-1)));
        assertEquals(PageProvider.DEFAULT_MAX_PAGE_SIZE,
                getMinMaxPageSize(Long.valueOf(1000), Long.valueOf(-1)));
        assertEquals(PageProvider.DEFAULT_MAX_PAGE_SIZE,
                getMinMaxPageSize(null, Long.valueOf(-1)));
        assertEquals(PageProvider.DEFAULT_MAX_PAGE_SIZE,
                getMinMaxPageSize(Long.valueOf(0), Long.valueOf(-1)));
        assertEquals(PageProvider.DEFAULT_MAX_PAGE_SIZE,
                getMinMaxPageSize(Long.valueOf(-1), Long.valueOf(-1)));
    }

    protected long getMinMaxPageSize(Long pageSize, Long maxPageSize) {
        MockPageProvider pp = new MockPageProvider();
        if (pageSize != null) {
            pp.setPageSize(pageSize.longValue());
        }
        if (maxPageSize != null) {
            pp.setMaxPageSize(maxPageSize.longValue());
        }
        return pp.getMinMaxPageSize();
    }

    @Test
    public void testPageProviderChangedListener() {
        MockPageProvider mockPageProvider = new MockPageProvider(5, 13, true);
        DummyPageProviderChangedListener listener = new DummyPageProviderChangedListener();
        assertFalse(listener.hasPageChanged);

        mockPageProvider.lastPage();
        assertFalse(listener.hasPageChanged);
        mockPageProvider.firstPage();
        assertFalse(listener.hasPageChanged);

        mockPageProvider.setPageProviderChangedListener(listener);
        mockPageProvider.lastPage();
        assertTrue(listener.hasPageChanged);
    }

    public static class DummyPageProviderChangedListener implements
            PageProviderChangedListener {

        public boolean hasPageChanged = false;

        @Override
        public void pageChanged(PageProvider<?> pageProvider) {
            hasPageChanged = true;
        }

        @Override
        public void refreshed(PageProvider<?> pageProvider) {
        }
    }

}
