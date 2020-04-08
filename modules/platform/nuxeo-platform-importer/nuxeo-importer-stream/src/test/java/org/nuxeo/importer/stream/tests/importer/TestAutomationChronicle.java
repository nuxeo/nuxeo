/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.importer.stream.tests.importer;

import java.util.Map;

import org.junit.Before;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 9.2
 */
@Deploy("org.nuxeo.importer.stream:test-stream-cq-contrib.xml")
public class TestAutomationChronicle extends TestAutomation {

    protected final static String OS = System.getProperty("os.name").toLowerCase();

    public final static boolean IS_WIN = OS.startsWith("win");

    @Before
    public void skipWindowsThatDontCleanTempFolder() {
        org.junit.Assume.assumeFalse(IS_WIN);
    }

    @Override
    public void addExtraParams(Map<String, Object> params) {
        params.put("logConfig", "chronicle");
    }
}
