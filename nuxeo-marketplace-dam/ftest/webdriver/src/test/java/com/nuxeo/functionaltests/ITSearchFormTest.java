/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package com.nuxeo.functionaltests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.dam.DAMPage;
import org.nuxeo.functionaltests.dam.SearchFormFragment;
import org.nuxeo.functionaltests.dam.SearchResultsFragment;

/**
 * @since 5.7.3
 */
public class ITSearchFormTest extends AbstractDAMTest {

    @Test
    public void testSelectedContentView() throws Exception {
        login("leela", "test");
        DAMPage damPage = getDAMPage();
        SearchFormFragment searchFormFragment = damPage.getSearchFormFragment();
        assertEquals("Default search",
                searchFormFragment.getSelectedContentView());
        logout();
    }

    @Test
    public void testTextSearch() throws Exception {
        login("leela", "test");

        DAMPage damPage = getDAMPage();
        damPage = damPage.createAsset("File", "One File",
                "One File description", "Leela", "1/1/2012");
        damPage = damPage.createAsset("File", "Another File",
                "Another File description", "Fry", "1/1/2012");
        damPage = damPage.createAsset("File", "Sample doc",
                "This is a sample doc", "Bender", "1/2/2012");

        SearchResultsFragment searchResultsFragment = damPage.getSearchResultsFragment();
        searchResultsFragment.getSelectedAsset().checkTextToBePresent(
                "One File");

        searchResultsFragment.checkTextToBePresent("One File");
        searchResultsFragment.checkTextToBePresent("Another File");
        searchResultsFragment.checkTextToBePresent("Sample doc");

        SearchFormFragment searchFormFragment = damPage.getSearchFormFragment();
        searchFormFragment.fillText("Another");
        searchFormFragment.doSearch();
        // reload results after ajax update
        searchResultsFragment = damPage.getSearchResultsFragment();
        Locator.waitForTextNotPresent(searchResultsFragment.getElement(), "One File");
        searchResultsFragment.getSelectedAsset().checkTextToBePresent("Another");
        searchResultsFragment.checkTextToBeNotPresent("One File");
        searchResultsFragment.checkTextToBePresent("Another File");
        searchResultsFragment.checkTextToBeNotPresent("Sample doc");

        searchFormFragment.clearSearch();
        // reload results after ajax update
        searchResultsFragment = damPage.getSearchResultsFragment();
        Locator.waitForTextPresent(searchResultsFragment.getElement(), "One File");
        searchResultsFragment.getSelectedAsset().checkTextToBePresent("Another");
        searchResultsFragment.checkTextToBePresent("One File");
        searchResultsFragment.checkTextToBePresent("Another File");
        searchResultsFragment.checkTextToBePresent("Sample doc");
    }

    @Test
    public void testMultipleCriteriaSearch() throws Exception {
        login("leela", "test");

        DAMPage damPage = getDAMPage();
        damPage = damPage.createAsset("File", "One Document",
                "One File description", "Leela", "1/1/2012");
        damPage = damPage.createAsset("File", "Another Document",
                "Another File description", "Fry", "1/1/2012");
        damPage = damPage.createAsset("File", "Sample picture",
                "This is a sample doc", "Bender", "1/2/2012");

        SearchResultsFragment searchResultsFragment = damPage.getSearchResultsFragment();
        SearchFormFragment searchFormFragment = damPage.getSearchFormFragment();
        searchFormFragment.fillText("Document");
        searchFormFragment.doSearch();
        // reload results after ajax update
        searchResultsFragment = damPage.getSearchResultsFragment();
        Locator.waitForTextNotPresent(searchResultsFragment.getElement(),
                "Sample picture");
        searchResultsFragment.getSelectedAsset().containsText("One");
        searchResultsFragment.checkTextToBePresent("One Document");
        searchResultsFragment.checkTextToBePresent("Another Document");
        searchResultsFragment.checkTextToBeNotPresent("Sample picture");
        searchFormFragment.fillOriginalAuthor("Fry");
        searchFormFragment.doSearch();
        // reload results after ajax update
        searchResultsFragment = damPage.getSearchResultsFragment();
        Locator.waitForTextNotPresent(searchResultsFragment.getElement(),
                "One Document");
        searchResultsFragment.getSelectedAsset().checkTextToBePresent("Another");
        searchResultsFragment.checkTextToBeNotPresent("One Document");
        searchResultsFragment.checkTextToBePresent("Another Document");
        searchResultsFragment.checkTextToBeNotPresent("Sample picture");

        searchFormFragment.fillAuthoringDate("1/1/2011 3:06 PM",
                "10/10/2011 3:06 PM");
        searchFormFragment.doSearch();
        // reload results after ajax update
        searchResultsFragment = damPage.getSearchResultsFragment();
        Locator.waitForTextPresent(searchResultsFragment.getElement(),
                "No documents match your search criteria");
    }
}
