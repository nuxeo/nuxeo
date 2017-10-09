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
package org.nuxeo.lib.core.mqueues.tests.computation;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.nuxeo.lib.core.mqueues.computation.ComputationManager;
import org.nuxeo.lib.core.mqueues.computation.mqueue.MQComputationManager;
import org.nuxeo.lib.core.mqueues.mqueues.MQManager;
import org.nuxeo.lib.core.mqueues.mqueues.chronicle.ChronicleMQManager;

import java.io.File;
import java.io.IOException;

/**
 * @since 9.1
 */
public class TestMQComputationManagerChronicle extends TestComputationManager {

    protected File basePath;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    public MQManager getStreams() throws Exception {
        this.basePath = folder.newFolder();
        return new ChronicleMQManager(basePath.toPath());
    }

    public MQManager getSameStreams() throws IOException {
        return new ChronicleMQManager(basePath.toPath());
    }

    @Override
    public ComputationManager getManager(MQManager mqManager) {
        return new MQComputationManager(mqManager);
    }
}
