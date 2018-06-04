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
package org.nuxeo.lib.stream.tests.computation;

import static org.nuxeo.lib.stream.tests.TestLibChronicle.IS_WIN;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.nuxeo.lib.stream.computation.StreamProcessor;
import org.nuxeo.lib.stream.computation.log.LogStreamProcessor;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.chronicle.ChronicleLogManager;

/**
 * @since 9.3
 */
public class TestLogStreamProcessorChronicle extends TestStreamProcessor {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    protected File basePath;

    @Before
    public void skipWindowsThatDoNotCleanTempFolder() {
        org.junit.Assume.assumeFalse(IS_WIN);
    }

    @Override
    public LogManager getLogManager() throws Exception {
        this.basePath = folder.newFolder();
        return new ChronicleLogManager(basePath.toPath());
    }

    @Override
    public LogManager getSameLogManager() {
        return new ChronicleLogManager(basePath.toPath());
    }

    @Override
    public StreamProcessor getStreamProcessor(LogManager logManager) {
        return new LogStreamProcessor(logManager);
    }
}
