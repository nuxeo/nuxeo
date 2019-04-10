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

import static org.nuxeo.importer.stream.tests.importer.TestAutomationChronicle.IS_WIN;

import java.nio.file.Path;

import org.junit.Before;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.chronicle.ChronicleLogManager;

/**
 * @since 9.2
 */
public class TestBlobImportChronicle extends TestBlobImport {
    protected Path basePath;

    @Before
    public void skipWindowsThatDontCleanTempFolder() {
        org.junit.Assume.assumeFalse(IS_WIN);
    }

    @Override
    public LogManager getManager() throws Exception {
        if (basePath == null) {
            basePath = folder.newFolder("log").toPath();
        }
        return new ChronicleLogManager(basePath);
    }
}
