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
package org.nuxeo.lib.core.mqueues.tests.mqueues;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nuxeo.lib.core.mqueues.mqueues.MQAppender;
import org.nuxeo.lib.core.mqueues.mqueues.MQManager;
import org.nuxeo.lib.core.mqueues.mqueues.chronicle.ChronicleMQManager;
import org.nuxeo.lib.core.mqueues.pattern.KeyValueMessage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * @since 9.2
 */
public class TestMQueueChronicle extends TestMQueue {
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
        return new ChronicleMQManager(basePath, "3s");
    }

    @Test
    public void deleteInvalidPath() throws Exception {
        final int NB_QUEUES = 5;

        ChronicleMQManager manager = (ChronicleMQManager) createManager();
        assertTrue(manager.createIfNotExists("foo", NB_QUEUES));
        String basePath = manager.getBasePath();
        assertTrue(manager.delete("foo"));

        // recreate
        assertTrue(manager.createIfNotExists("foo", NB_QUEUES));
        // add a file in the basePath
        File aFile = new File(basePath, "foo/foo.txt");
        aFile.createNewFile();
        try {
            manager.delete("foo");
            fail("Can not delete a mqueue with external data");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testFileRetention() throws Exception {

        KeyValueMessage msg1 = KeyValueMessage.of("id1");
        KeyValueMessage msg2 = KeyValueMessage.of("id2");
        KeyValueMessage msg3 = KeyValueMessage.of("id3");
        KeyValueMessage msg4 = KeyValueMessage.of("id4");

        ChronicleMQManager manager = (ChronicleMQManager) createManager();
        manager.createIfNotExists("foo", 1);
        MQAppender<KeyValueMessage> appender = manager.getAppender("foo");

        File queueFile = new File(manager.getBasePath(), "foo/Q-00");
        assertEquals(0, queueFile.list().length);

        appender.append(0, msg1);
        assertEquals(1, queueFile.list().length);
        Thread.sleep(1001);
        appender.append(0, msg2);
        assertEquals(2, queueFile.list().length);

        Thread.sleep(4001);

        appender.append(0, msg3);
        assertEquals(3, queueFile.list().length);

        // From now, there should be at least 3 retained files in the queue
        Thread.sleep(1001);
        appender.append(0, msg4);
        assertEquals(3, queueFile.list().length);

    }

}
