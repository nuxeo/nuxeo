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

import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.impl.DocumentsPageProvider;

/**
 * @author Anahide Tchertchian
 */
public class TestDocumentPageProvider extends TestCase {

    public class MockDocumentModelIterator implements DocumentModelIterator {

        private static final long serialVersionUID = 1L;

        protected final int size;

        protected final Iterator<DocumentModel> iterator;

        public MockDocumentModelIterator(DocumentModelList list) {
            super();
            this.size = list.size();
            this.iterator = list.iterator();
        }

        public long size() {
            return size;
        }

        public Iterator<DocumentModel> iterator() {
            return iterator;
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public DocumentModel next() {
            return iterator.next();
        }

        public void remove() {
            iterator.remove();
        }

    }

    protected DocumentModelIterator getDocumentIterator(int size) {
        DocumentModelList list = new DocumentModelListImpl();
        for (int i = 0; i < size; i++) {
            list.add(new DocumentModelImpl("/", String.format("Mock_%s",
                    Integer.valueOf(i)), "mockDocumentType"));
        }
        return new MockDocumentModelIterator(list);
    }

    public void testPageProvider() {
        PageProvider<DocumentModel> provider = new DocumentsPageProvider(
                getDocumentIterator(13), 5);

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
        DocumentModel currentEntry = provider.getCurrentEntry();
        currentEntry = provider.getCurrentEntry();
        assertNotNull(currentEntry);
        assertEquals("Mock_0", currentEntry.getName());
        assertFalse(provider.isPreviousEntryAvailable());
        assertTrue(provider.isNextEntryAvailable());
        provider.nextEntry();
        currentEntry = provider.getCurrentEntry();
        assertNotNull(currentEntry);
        assertEquals("Mock_1", currentEntry.getName());
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
        assertEquals("Mock_5", currentEntry.getName());

        // switch to previous page
        assertTrue(provider.isPreviousEntryAvailable());
        assertTrue(provider.isNextEntryAvailable());
        provider.previousEntry();
        currentEntry = provider.getCurrentEntry();
        assertNotNull(currentEntry);
        assertEquals(0, provider.getCurrentPageIndex());
        assertEquals("Mock_4", currentEntry.getName());

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
        assertEquals("Mock_12", currentEntry.getName());
        provider.nextEntry();
        currentEntry = provider.getCurrentEntry();
        assertNotNull(currentEntry);
        assertEquals(2, provider.getCurrentPageIndex());
        assertEquals("Mock_12", currentEntry.getName());

    }

    protected void checkFirstPage(PageProvider<DocumentModel> provider) {
        assertEquals(5, provider.getPageSize());
        assertEquals(13, provider.getResultsCount());
        assertEquals(3, provider.getNumberOfPages());
        List<DocumentModel> currentItems = provider.getCurrentPage();
        assertEquals(5, currentItems.size());
        assertEquals("Mock_0", currentItems.get(0).getName());
        assertEquals("Mock_4", currentItems.get(4).getName());
        assertEquals(0, provider.getCurrentPageIndex());
        assertEquals(0, provider.getCurrentPageOffset());
        assertEquals(5, provider.getCurrentPageSize());
        assertEquals("1/3", provider.getCurrentPageStatus());
        assertFalse(provider.isPreviousPageAvailable());
        assertTrue(provider.isNextPageAvailable());
    }

    protected void checkSecondPage(PageProvider<DocumentModel> provider) {
        assertEquals(5, provider.getPageSize());
        assertEquals(13, provider.getResultsCount());
        assertEquals(3, provider.getNumberOfPages());
        List<DocumentModel> currentItems = provider.getCurrentPage();
        assertEquals(5, currentItems.size());
        assertEquals("Mock_5", currentItems.get(0).getName());
        assertEquals("Mock_9", currentItems.get(4).getName());
        assertEquals(1, provider.getCurrentPageIndex());
        assertEquals(5, provider.getCurrentPageOffset());
        assertEquals(5, provider.getCurrentPageSize());
        assertEquals("2/3", provider.getCurrentPageStatus());
        assertTrue(provider.isPreviousPageAvailable());
        assertTrue(provider.isNextPageAvailable());
    }

    protected void checkThirdPage(PageProvider<DocumentModel> provider) {
        assertEquals(5, provider.getPageSize());
        assertEquals(13, provider.getResultsCount());
        assertEquals(3, provider.getNumberOfPages());
        List<DocumentModel> currentItems = provider.getCurrentPage();
        assertEquals(3, currentItems.size());
        assertEquals("Mock_10", currentItems.get(0).getName());
        assertEquals("Mock_12", currentItems.get(2).getName());
        assertEquals(2, provider.getCurrentPageIndex());
        assertEquals(10, provider.getCurrentPageOffset());
        assertEquals(3, provider.getCurrentPageSize());
        assertEquals("3/3", provider.getCurrentPageStatus());
        assertTrue(provider.isPreviousPageAvailable());
        assertFalse(provider.isNextPageAvailable());
    }

    public void testPageProviderWithPageSizeSameThanResultSize() {
        PageProvider<DocumentModel> provider = new DocumentsPageProvider(
                getDocumentIterator(10), 5);
        assertEquals(5, provider.getPageSize());
        assertEquals(10, provider.getResultsCount());
        assertEquals(2, provider.getNumberOfPages());
        List<DocumentModel> currentItems = provider.getCurrentPage();
        assertEquals(5, currentItems.size());
        assertEquals("Mock_0", currentItems.get(0).getName());
        assertEquals("Mock_4", currentItems.get(4).getName());
        assertEquals(0, provider.getCurrentPageIndex());
        assertEquals(0, provider.getCurrentPageOffset());
        assertEquals(5, provider.getCurrentPageSize());
        assertEquals("1/2", provider.getCurrentPageStatus());
        assertFalse(provider.isPreviousPageAvailable());
        assertTrue(provider.isNextPageAvailable());
        assertFalse(provider.isPreviousEntryAvailable());
        assertTrue(provider.isNextEntryAvailable());
    }

    public void testPageProviderWithoutPagination() {
        PageProvider<DocumentModel> provider = new DocumentsPageProvider(
                getDocumentIterator(13), 0);
        assertEquals(0, provider.getPageSize());
        assertEquals(13, provider.getResultsCount());
        assertEquals(1, provider.getNumberOfPages());
        List<DocumentModel> currentItems = provider.getCurrentPage();
        assertEquals(13, currentItems.size());
        assertEquals("Mock_0", currentItems.get(0).getName());
        assertEquals("Mock_12", currentItems.get(12).getName());
        assertEquals(0, provider.getCurrentPageIndex());
        assertEquals(0, provider.getCurrentPageOffset());
        assertEquals(13, provider.getCurrentPageSize());
        assertEquals("1/1", provider.getCurrentPageStatus());
        assertFalse(provider.isPreviousPageAvailable());
        assertFalse(provider.isNextPageAvailable());
        assertFalse(provider.isPreviousEntryAvailable());
        assertTrue(provider.isNextEntryAvailable());
    }
}