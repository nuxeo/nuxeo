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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.UNKNOWN;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Random;
import java.util.stream.Stream;

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
import org.nuxeo.runtime.transaction.TransactionHelper;

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
        BulkCommand command = new BulkCommand.Builder(WordCountAction.ACTION_NAME, myFile).useGenericScroller().build();
        String commandId = bulkService.submit(command);
        assertTrue(bulkService.await(commandId, Duration.ofSeconds(20)));
        BulkStatus status = bulkService.getStatus(commandId);
        assertEquals(COMPLETED, status.getState());
        assertFalse(status.isQueryLimitReached());
        assertEquals(wordCount, status.getResult().get("wordCount"));
    }

    @Test
    public void testWordCountWithLimitedQuery() throws Exception {
        int wordCount = 2732;
        String myFile = createFile(wordCount);
        int lines = countLines(myFile);

        // Use an action limited by default to the first 100 first lines
        BulkCommand command = new BulkCommand.Builder("testWordCountLimited", myFile).useGenericScroller().build();
        String commandId = bulkService.submit(command);
        assertTrue(bulkService.await(commandId, Duration.ofSeconds(20)));
        BulkStatus status = bulkService.getStatus(commandId);
        assertEquals(COMPLETED, status.getState());
        assertTrue(status.isQueryLimitReached());
        int result100 = (Integer) status.getResult().get("wordCount");
        assertTrue(result100 > 0);
        // the number of word for the first 100 lines is inferior to the total number of words in the file
        assertTrue(wordCount > result100);

        // Now set an explicit limit to a lower number of lines
        command = new BulkCommand.Builder("testWordCountLimited", myFile).useGenericScroller().queryLimit(10).build();
        commandId = bulkService.submit(command);
        assertTrue(bulkService.await(commandId, Duration.ofSeconds(20)));
        status = bulkService.getStatus(commandId);
        assertTrue(status.isQueryLimitReached());
        assertEquals(COMPLETED, status.getState());
        int result = (Integer) status.getResult().get("wordCount");
        // there is less words in 10 lines than 100
        assertTrue(result100 > result);

        // Check edge cases when limit is set to n-1 lines
        command = new BulkCommand.Builder("testWordCountLimited", myFile).useGenericScroller()
                                                                         .queryLimit(lines - 1)
                                                                         .build();
        commandId = bulkService.submit(command);
        assertTrue(bulkService.await(commandId, Duration.ofSeconds(20)));
        status = bulkService.getStatus(commandId);
        // the limit is reached
        assertTrue(status.isQueryLimitReached());
        assertEquals(COMPLETED, status.getState());
        result = (Integer) status.getResult().get("wordCount");
        assertTrue(wordCount >= result);

        // Check edge cases when limit is set to n lines
        command = new BulkCommand.Builder("testWordCountLimited", myFile).useGenericScroller()
                                                                         .queryLimit(lines)
                                                                         .build();
        commandId = bulkService.submit(command);
        assertTrue(bulkService.await(commandId, Duration.ofSeconds(20)));
        status = bulkService.getStatus(commandId);
        // the limit is reached
        assertTrue(status.isQueryLimitReached());
        assertEquals(COMPLETED, status.getState());
        result = (Integer) status.getResult().get("wordCount");
        assertEquals(wordCount, result);

        // Check edge cases when limit is set to n+1 lines
        command = new BulkCommand.Builder("testWordCountLimited", myFile).useGenericScroller()
                                                                         .queryLimit(lines + 1)
                                                                         .build();
        commandId = bulkService.submit(command);
        assertTrue(bulkService.await(commandId, Duration.ofSeconds(20)));
        status = bulkService.getStatus(commandId);
        // the limit is not reached
        assertFalse(status.isQueryLimitReached());
        assertEquals(COMPLETED, status.getState());
        result = (Integer) status.getResult().get("wordCount");
        assertEquals(wordCount, result);

        // Unlimited query
        command = new BulkCommand.Builder("testWordCountLimited", myFile).useGenericScroller().queryUnlimited().build();
        commandId = bulkService.submit(command);
        assertTrue(bulkService.await(commandId, Duration.ofSeconds(20)));
        status = bulkService.getStatus(commandId);
        assertEquals(COMPLETED, status.getState());
        assertFalse(status.isQueryLimitReached());
        result = (Integer) status.getResult().get("wordCount");
        assertEquals(wordCount, result);
    }

    @Test
    public void testTransactionalSubmit() throws Exception {
        int wordCount = 42;
        String myFile = createFile(wordCount);
        BulkCommand command = new BulkCommand.Builder("testWordCountLimited", myFile).useGenericScroller().build();
        String commandId = bulkService.submitTransactional(command);
        // status of bulk command is unknown until committed
        BulkStatus status = bulkService.getStatus(commandId);
        assertEquals(UNKNOWN, status.getState());
        // commit
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        status = bulkService.getStatus(commandId);
        assertNotEquals(UNKNOWN, status.getState());
        assertTrue(bulkService.await(commandId, Duration.ofSeconds(20)));
        status = bulkService.getStatus(commandId);
        assertEquals(COMPLETED, status.getState());
        assertEquals(wordCount, status.getResult().get("wordCount"));

        // Rollback case
        BulkCommand commandBis = new BulkCommand.Builder("testWordCountLimited", myFile).useGenericScroller().build();
        commandId = bulkService.submitTransactional(commandBis);
        // state of bulk command is unknown until committed
        status = bulkService.getStatus(commandId);
        assertEquals(UNKNOWN, status.getState());
        // rollback
        TransactionHelper.setTransactionRollbackOnly();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        // No bulk command executed
        status = bulkService.getStatus(commandId);
        assertEquals(UNKNOWN, status.getState());
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

    protected int countLines(String filename) throws IOException {
        try (Stream<String> stream = Files.lines(Paths.get(filename), StandardCharsets.UTF_8)) {
            return Math.toIntExact(stream.count());
        }
    }
}
