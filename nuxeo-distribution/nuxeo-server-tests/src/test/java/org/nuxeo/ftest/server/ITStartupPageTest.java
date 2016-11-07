/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ftest.server;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper;
import org.nuxeo.functionaltests.AbstractTest;

/**
 * Tests the Nuxeo Server startup page.
 *
 * @since 8.10
 */
public class ITStartupPageTest extends AbstractTest {

    @Test
    public void testStartupPage() {
        driver.get(NUXEO_URL);
        assertEquals(NUXEO_URL + "/" + LoginScreenHelper.DEFAULT_STARTUP_PAGE_PATH, driver.getCurrentUrl());
    }

}
