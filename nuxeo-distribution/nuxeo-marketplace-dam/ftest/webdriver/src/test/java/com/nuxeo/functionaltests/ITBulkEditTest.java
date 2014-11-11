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

import org.junit.Test;
import org.nuxeo.functionaltests.dam.BulkEditFancyBoxFragment;
import org.nuxeo.functionaltests.dam.DAMPage;
import org.nuxeo.functionaltests.dam.SearchResultsFragment;

/**
 * @since 5.7.3
 */
public class ITBulkEditTest extends AbstractDAMTest {

    @Test
    public void testBulkEdit() throws Exception {
        login("leela", "test");

        DAMPage damPage = getDAMPage();
        damPage = damPage.createAsset("File", "One File",
                "One File description", "Leela", "5/5/2012");
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
        searchResultsFragment.checkTextToBePresent("Leela");
        searchResultsFragment.checkTextToBePresent("Fry");
        searchResultsFragment.checkTextToBePresent("Bender");
        searchResultsFragment.checkTextToBePresent("5/5/2012");
        searchResultsFragment.checkTextToBePresent("1/1/2012");
        searchResultsFragment.checkTextToBePresent("1/2/2012");
        searchResultsFragment.checkTextToBeNotPresent("Zoidberg");
        searchResultsFragment.checkTextToBeNotPresent("10/10/2010");

        searchResultsFragment.selectAll();
        BulkEditFancyBoxFragment bulkEdit = searchResultsFragment.showBulkEdit(damPage);
        bulkEdit.fillOriginalAuthor("Zoidberg");
        bulkEdit.fillAuthoringDate("10/10/2010 3:06 PM");
        bulkEdit.update();

        damPage = getDAMPage();
        searchResultsFragment = damPage.getSearchResultsFragment();
        searchResultsFragment.checkTextToBeNotPresent("Leela");
        searchResultsFragment.checkTextToBeNotPresent("Fry");
        searchResultsFragment.checkTextToBeNotPresent("Bender");
        searchResultsFragment.checkTextToBeNotPresent("5/5/2012");
        searchResultsFragment.checkTextToBeNotPresent("1/1/2012");
        searchResultsFragment.checkTextToBeNotPresent("1/2/2012");
        searchResultsFragment.checkTextToBePresent("Zoidberg");
        searchResultsFragment.checkTextToBePresent("10/10/2010");
    }

}
