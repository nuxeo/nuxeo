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
package org.nuxeo.lib.core.mqueues.tests.pattern;

import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.nuxeo.lib.core.mqueues.mqueues.MQManager;
import org.nuxeo.lib.core.mqueues.mqueues.chronicle.ChronicleMQManager;

import java.io.IOException;
import java.nio.file.Path;

public class TestPatternQueuingChronicle extends TestPatternQueuing {
    protected Path basePath;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @After
    public void resetBasePath() throws IOException {
        basePath = null;
    }

    @Override
    public MQManager createManager() throws Exception {
        if (basePath == null) {
            basePath = folder.newFolder().toPath();
        }
        return new ChronicleMQManager(basePath);
    }
}