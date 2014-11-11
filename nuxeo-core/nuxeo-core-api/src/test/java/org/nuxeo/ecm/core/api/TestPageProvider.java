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
package org.nuxeo.ecm.core.api;

import java.util.List;

import junit.framework.TestCase;

/**
 * @author Anahide Tchertchian
 */
public class TestPageProvider extends TestCase {

    public void testPageProvider() {
        PageProvider<MockPagedListItem> provider = new MockPageProvider(5, 13);

        checkFirstPage(provider);
        provider.nextPage();
        checkSecondPage(provider);
        provider.nextPage();
        checkThirdPage(provider);

        // go back to first page and test again
        provider.firstPage();
        checkFirstPage(provider);

        // go to last page and test again
        provider.lastPage();
        checkThirdPage(provider);
        provider.previousPage();
        checkSecondPage(provider);
        provider.previousPage();
        checkFirstPage(provider);

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
        assertTrue(provider.isPreviousEntryAvailable());
        assertTrue(provider.isNextEntryAvailable());
        provider.nextEntry();
        assertTrue(provider.isPreviousEntryAvailable());
        assertTrue(provider.isNextEntryAvailable());
        provider.nextEntry();
        assertTrue(provider.isPreviousEntryAvailable());
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

    protected void checkFirstPage(PageProvider<MockPagedListItem> provider) {
        assertEquals(5, provider.getPageSize());
        assertEquals(13, provider.getResultsCount());
        assertEquals(3, provider.getNumberOfPages());
        List<MockPagedListItem> currentItems = provider.getCurrentPage();
        assertEquals(5, currentItems.size());
        assertEquals(0, currentItems.get(0).getPosition());
        assertEquals(4, currentItems.get(4).getPosition());
        assertEquals(0, provider.getCurrentPageIndex());
        assertEquals(0, provider.getCurrentPageOffset());
        assertEquals(5, provider.getCurrentPageSize());
        assertEquals("1/3", provider.getCurrentPageStatus());
        assertFalse(provider.isPreviousPageAvailable());
        assertTrue(provider.isNextPageAvailable());
    }

    protected void checkSecondPage(PageProvider<MockPagedListItem> provider) {
        assertEquals(5, provider.getPageSize());
        assertEquals(13, provider.getResultsCount());
        assertEquals(3, provider.getNumberOfPages());
        List<MockPagedListItem> currentItems = provider.getCurrentPage();
        assertEquals(5, currentItems.size());
        assertEquals(5, currentItems.get(0).getPosition());
        assertEquals(9, currentItems.get(4).getPosition());
        assertEquals(1, provider.getCurrentPageIndex());
        assertEquals(5, provider.getCurrentPageOffset());
        assertEquals(5, provider.getCurrentPageSize());
        assertEquals("2/3", provider.getCurrentPageStatus());
        assertTrue(provider.isPreviousPageAvailable());
        assertTrue(provider.isNextPageAvailable());
    }

    protected void checkThirdPage(PageProvider<MockPagedListItem> provider) {
        assertEquals(5, provider.getPageSize());
        assertEquals(13, provider.getResultsCount());
        assertEquals(3, provider.getNumberOfPages());
        List<MockPagedListItem> currentItems = provider.getCurrentPage();
        assertEquals(3, currentItems.size());
        assertEquals(10, currentItems.get(0).getPosition());
        assertEquals(12, currentItems.get(2).getPosition());
        assertEquals(2, provider.getCurrentPageIndex());
        assertEquals(10, provider.getCurrentPageOffset());
        assertEquals(3, provider.getCurrentPageSize());
        assertEquals("3/3", provider.getCurrentPageStatus());
        assertTrue(provider.isPreviousPageAvailable());
        assertFalse(provider.isNextPageAvailable());
    }

    public void testPageProviderWithPageSizeSameThanResultSize() {
        PageProvider<MockPagedListItem> provider = new MockPageProvider(5, 10);
        assertEquals(5, provider.getPageSize());
        assertEquals(10, provider.getResultsCount());
        assertEquals(2, provider.getNumberOfPages());
        assertEquals(0, provider.getCurrentPageIndex());
        List<MockPagedListItem> currentItems = provider.getCurrentPage();
        assertEquals(5, currentItems.size());
        assertEquals(0, currentItems.get(0).getPosition());
        assertEquals(4, currentItems.get(4).getPosition());
        assertEquals(0, provider.getCurrentPageIndex());
        assertEquals(0, provider.getCurrentPageOffset());
        assertEquals(5, provider.getCurrentPageSize());
        assertEquals("1/2", provider.getCurrentPageStatus());
        assertFalse(provider.isPreviousPageAvailable());
        assertTrue(provider.isNextPageAvailable());
        assertFalse(provider.isPreviousEntryAvailable());
        assertTrue(provider.isNextEntryAvailable());
        provider.lastPage();
        assertEquals(1, provider.getCurrentPageIndex());
        provider.firstPage();
        assertEquals(0, provider.getCurrentPageIndex());
    }

    public void testPageProviderWithoutPagination() {
        PageProvider<MockPagedListItem> provider = new MockPageProvider(0, 13);
        assertEquals(0, provider.getPageSize());
        assertEquals(13, provider.getResultsCount());
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
        provider.lastPage();
        assertEquals(0, provider.getCurrentPageIndex());
        provider.firstPage();
        assertEquals(0, provider.getCurrentPageIndex());
    }

}
