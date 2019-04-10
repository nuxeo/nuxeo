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
 */
package org.nuxeo.lib.stream.tests.pattern;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.chronicle.ChronicleLogManager;

public class TestPatternBoundedQueuingChronicle extends TestPatternBoundedQueuing {
    protected Path basePath;

    @Before
    public void skipWindowsThatDontCleanTempFolder() {
        org.junit.Assume.assumeFalse(TestPatternQueuingChronicle.IS_WIN);
    }

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @After
    public void resetBasePath() throws IOException {
        basePath = null;
    }

    @Override
    public LogManager createManager() throws Exception {
        if (basePath == null) {
            basePath = folder.newFolder().toPath();
        }
        return new ChronicleLogManager(basePath);
    }
}
