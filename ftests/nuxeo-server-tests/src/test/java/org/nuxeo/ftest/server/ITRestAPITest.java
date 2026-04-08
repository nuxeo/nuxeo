/*
 * (C) Copyright 2016-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.ftest.server;

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.functionaltests.LogTestWatchman;
import org.nuxeo.functionaltests.RestTestRule;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class ITRestAPITest {

    @Rule
    public MethodRule watchman = new LogTestWatchman();

    @Rule
    public final RestTestRule restHelper = new RestTestRule();

    @Test
    public void testAPIServletForwardWithReservedCharacters() {
        String parentPath = "/default-domain";

        documentExists(parentPath, "test ; doc [with] some #");
        documentExists(parentPath, ", $, :, ; &? and =+");
    }

    protected void documentExists(String parentPath, String title) {
        restHelper.createDocument(parentPath, "File", title);

        String encodedTitle = URIUtils.quoteURIPathComponent(title, false, false);
        assertTrue(restHelper.documentExists(parentPath + "/" + encodedTitle));
    }

}
