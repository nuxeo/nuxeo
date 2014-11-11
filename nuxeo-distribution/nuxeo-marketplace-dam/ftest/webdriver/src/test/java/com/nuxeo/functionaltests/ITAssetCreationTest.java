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
import org.nuxeo.functionaltests.dam.DAMPage;
import org.nuxeo.functionaltests.dam.SearchResultsFragment;

/**
 * @since 5.7.3
 */
public class ITAssetCreationTest extends AbstractDAMTest {

    @Test
    public void testFileCreation() throws Exception {
        login();

        DAMPage damPage = getDAMPage();
        damPage = damPage.createAsset("File", "One File",
                "One File description", "Leela", "1/1/2012");
        SearchResultsFragment searchResultsFragment = damPage.getSearchResultsFragment();
        searchResultsFragment.checkTextToBePresent("One File");
        searchResultsFragment.checkTextToBePresent("Leela");
        searchResultsFragment.checkTextToBePresent("1/1/2012");

        logout();
    }

    // TODO Picture / video / audio creation
}
