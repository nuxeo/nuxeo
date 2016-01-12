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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession.StandardCopyOption;

public class TestCopyOptions {

    @Test
    public void testParseWithoutOption() {
        CopyOptions options = CopyOptions.parse();
        assertFalse(options.isResetLifeCycle());
        assertFalse(options.isResetCreator());
    }

    @Test
    public void testParseAllOptions() {
        // Just test all cases are implemented in parse method
        CopyOptions options = CopyOptions.parse(StandardCopyOption.values());
        assertTrue(options.isResetLifeCycle());
        assertTrue(options.isResetCreator());
    }

}
