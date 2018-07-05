/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.bulk;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.nuxeo.ecm.core.bulk.BulkComponent.BULK_KV_STORE_NAME;
import static org.nuxeo.ecm.core.bulk.BulkComponent.BULK_LOG_MANAGER_NAME;
import static org.nuxeo.ecm.core.bulk.BulkStatus.State.COMPLETED;
import static org.nuxeo.ecm.core.bulk.BulkStatus.State.SCHEDULED;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.bulk.BulkStatus.State;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.stream.StreamService;

/**
 * Basic implementation of {@link BulkService}.
 *
 * @since 10.2
 */
public class BulkServiceImpl implements BulkService {

    private static final Log log = LogFactory.getLog(BulkServiceImpl.class);

    protected static final String SET_STREAM_NAME = "documentSet";

    protected static final String COMMAND = ":command";

    protected static final String SUBMIT_TIME = ":submitTime";

    protected static final String SCROLL_START_TIME = ":scrollStartTime";

    protected static final String SCROLL_END_TIME = ":scrollEndTime";

    protected static final String STATE = ":state";

    protected static final String PROCESSED_DOCUMENTS = ":processedDocs";

    protected static final String SCROLLED_DOCUMENT_COUNT = ":count";

    @Override
    public String submit(BulkCommand command) {
        if (log.isDebugEnabled()) {
            log.debug("Run action with command=" + command);
        }
        // check command
        if (isEmpty(command.getRepository()) || isEmpty(command.getQuery()) || isEmpty(command.getAction())) {
            throw new IllegalArgumentException("Missing mandatory values");
        }
        // create the command id and status
        String commandId = UUID.randomUUID().toString();
        byte[] commandAsBytes = BulkCommands.toBytes(command);

        // store the bulk command and status in the key/value store
        KeyValueStore keyValueStore = getKvStore();
        keyValueStore.put(commandId + STATE, SCHEDULED.toString());
        keyValueStore.put(commandId + SUBMIT_TIME, Instant.now().toEpochMilli());
        keyValueStore.put(commandId + COMMAND, commandAsBytes);

        // send it to nuxeo-stream
        LogManager logManager = Framework.getService(StreamService.class).getLogManager(BULK_LOG_MANAGER_NAME);
        LogAppender<Record> logAppender = logManager.getAppender(SET_STREAM_NAME);
        logAppender.append(commandId, Record.of(commandId, commandAsBytes));

        return commandId;
    }

    @Override
    public BulkStatus getStatus(String commandId) {
        BulkStatus status = new BulkStatus();
        status.setId(commandId);

        // retrieve values from KeyValueStore
        KeyValueStore keyValueStore = getKvStore();
        String state = keyValueStore.getString(commandId + STATE);
        status.setState(State.valueOf(state));

        Long submitTime = keyValueStore.getLong(commandId + SUBMIT_TIME);
        status.setSubmitTime(Instant.ofEpochMilli(submitTime.longValue()));

        BulkCommand command = BulkCommands.fromKVStore(keyValueStore, commandId);
        status.setCommand(command);

        Long scrollStartTime = keyValueStore.getLong(commandId + SCROLL_START_TIME);
        if (scrollStartTime != null) {
            status.setScrollStartTime(Instant.ofEpochMilli(scrollStartTime));
        }
        Long scrollEndTime = keyValueStore.getLong(commandId + SCROLL_END_TIME);
        if (scrollEndTime != null) {
            status.setScrollEndTime(Instant.ofEpochMilli(scrollEndTime));
        }

        Long processedDocuments = keyValueStore.getLong(commandId + PROCESSED_DOCUMENTS);
        status.setProcessed(processedDocuments);

        Long scrolledDocumentCount = keyValueStore.getLong(commandId + SCROLLED_DOCUMENT_COUNT);
        status.setCount(scrolledDocumentCount);

        return status;
    }

    @Override
    public boolean await(String commandId, Duration duration) throws InterruptedException {
        long deadline = System.currentTimeMillis() + duration.toMillis();
        KeyValueStore kvStore = getKvStore();
        do {
            if (COMPLETED.toString().equals(kvStore.getString(commandId + STATE))) {
                return true;
            }
            Thread.sleep(500);
        } while (deadline > System.currentTimeMillis());
        log.debug("await timeout for commandId(" + commandId + ") after " + duration.toMillis() + " ms");
        return false;
    }

    public KeyValueStore getKvStore() {
        return Framework.getService(KeyValueService.class).getKeyValueStore(BULK_KV_STORE_NAME);
    }

}
