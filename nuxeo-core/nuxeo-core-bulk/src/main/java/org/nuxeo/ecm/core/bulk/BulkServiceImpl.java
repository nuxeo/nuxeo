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

    public static final String COMMAND = ":command";

    public static final String STATUS = ":status";

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

        byte[] commandAsBytes = BulkCodecs.getBulkCommandCodec().encode(command);

        // store the bulk command and status in the key/value store
        KeyValueStore keyValueStore = getKvStore();

        BulkStatus status = new BulkStatus();
        status.setId(commandId);
        status.setState(SCHEDULED);
        status.setSubmitTime(Instant.now());

        byte[] statusAsBytes = BulkCodecs.getBulkStatusCodec().encode(status);

        keyValueStore.put(commandId + COMMAND, commandAsBytes);
        keyValueStore.put(commandId + STATUS, statusAsBytes);

        // send it to nuxeo-stream
        LogManager logManager = Framework.getService(StreamService.class).getLogManager(BULK_LOG_MANAGER_NAME);
        LogAppender<Record> logAppender = logManager.getAppender(SET_STREAM_NAME);
        logAppender.append(commandId, Record.of(commandId, commandAsBytes));

        return commandId;
    }

    @Override
    public BulkStatus getStatus(String commandId) {
        // retrieve values from KeyValueStore
        KeyValueStore keyValueStore = getKvStore();
        byte[] statusAsBytes = keyValueStore.get(commandId + STATUS);
        return BulkCodecs.getBulkStatusCodec().decode(statusAsBytes);
    }

    @Override
    public boolean await(String commandId, Duration duration) throws InterruptedException {
        long deadline = System.currentTimeMillis() + duration.toMillis();
        KeyValueStore kvStore = getKvStore();
        do {
            if (COMPLETED.equals(BulkCodecs.getBulkStatusCodec().decode(kvStore.get(commandId + STATUS)).getState())) {
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
