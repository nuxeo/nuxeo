/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.bulk.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.Random;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreBulkFeature.class)
@Deploy("org.nuxeo.ecm.core.bulk.test:OSGI-INF/test-scroll-contrib.xml")
@Deploy("org.nuxeo.ecm.core.bulk.test:OSGI-INF/test-bulk-contrib.xml")
public class TestWordCountAction {

    protected static final Random RANDOM = new Random(); // NOSONAR (doesn't need cryptographic strength)

    @Inject
    protected BulkService bulkService;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testWordCount() throws Exception {
        int wordCount = 2732;
        String myFile = createFile(wordCount);
        BulkCommand command = new BulkCommand.Builder(WordCountAction.ACTION_NAME, myFile)
                .useGenericScroller().build();
        String commandId = bulkService.submit(command);
        assertTrue(bulkService.await(commandId, Duration.ofSeconds(20)));
        BulkStatus status = bulkService.getStatus(commandId);
        assertEquals(COMPLETED, status.getState());
        assertEquals(wordCount, status.getResult().get("wordCount"));
    }

    protected String createFile(int wordCount) throws IOException {
        File tempFile = testFolder.newFile("file.txt");
        try (FileWriter fw = new FileWriter(tempFile, true); BufferedWriter bw = new BufferedWriter(fw)) {
            for (int i = 0; i < wordCount; i++) {
                bw.write(" word" + i);
                if (RANDOM.nextInt(10) == 1) {
                    bw.newLine();
                }
            }
        }
        return tempFile.getAbsolutePath();
    }
}
