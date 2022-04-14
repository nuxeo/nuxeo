/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.directory.test;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2021.20
 */
@SuppressWarnings("unchecked")
@RunWith(FeaturesRunner.class)
@Features(DirectoryFeature.class)
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.directory.tests:test-directory-continent-config.xml")
public class DirectoryEntryPageProviderTest {

    protected static final String PP_NAME = "nuxeo_directory_entry_listing";

    @Inject
    protected DirectoryService directoryService;

    @Inject
    protected PageProviderService pageProviderService;

    protected Directory continent;

    @Before
    public void before() {
        continent = directoryService.getDirectory("continent");
    }

    @Test
    public void testGetPage() {
        PageProvider<?> pp = pageProviderService.getPageProvider(PP_NAME, singletonList(new SortInfo("id", true)), null,
                0L, null, continent);

        List<DirectoryEntry> entriesPage = (List<DirectoryEntry>) pp.getCurrentPage();
        assertEquals(7, entriesPage.size());
        assertEquals(7, pp.getResultsCount());
        assertEquals(7, pp.getCurrentPageSize());
        assertEquals(0, pp.getCurrentPageIndex());
        assertEquals("africa", entriesPage.get(0).getDocumentModel().getId());
        assertEquals("antarctica", entriesPage.get(1).getDocumentModel().getId());
        assertEquals("asia", entriesPage.get(2).getDocumentModel().getId());
        assertEquals("europe", entriesPage.get(3).getDocumentModel().getId());
        assertEquals("north-america", entriesPage.get(4).getDocumentModel().getId());
        assertEquals("oceania", entriesPage.get(5).getDocumentModel().getId());
        assertEquals("south-america", entriesPage.get(6).getDocumentModel().getId());
        assertFalse(pp.isPreviousPageAvailable());
        assertFalse(pp.isNextPageAvailable());
        assertFalse(pp.isLastPageAvailable());
    }

    // NXP-30520
    @Test
    public void testIsNextPageAvailable() {
        PageProvider<?> pp = pageProviderService.getPageProvider(PP_NAME, singletonList(new SortInfo("id", true)), 1L,
                0L, null, continent);

        List<DirectoryEntry> entriesPage = (List<DirectoryEntry>) pp.getCurrentPage();
        assertEquals(1, entriesPage.size());
        assertEquals(7, pp.getResultsCount());
        assertEquals(1, pp.getCurrentPageSize());
        assertEquals(0, pp.getCurrentPageIndex());
        assertFalse(pp.isPreviousPageAvailable());
        assertTrue(pp.isNextPageAvailable());
        assertTrue(pp.isLastPageAvailable());

        pp.lastPage();

        // needed to execute the query against DB
        entriesPage = (List<DirectoryEntry>) pp.getCurrentPage();
        assertEquals(1, entriesPage.size());
        assertEquals(7, pp.getResultsCount());
        assertEquals(1, pp.getCurrentPageSize());
        assertEquals(6, pp.getCurrentPageIndex());
        assertTrue(pp.isPreviousPageAvailable());
        assertFalse(pp.isNextPageAvailable());
        assertFalse(pp.isLastPageAvailable());
    }
}
