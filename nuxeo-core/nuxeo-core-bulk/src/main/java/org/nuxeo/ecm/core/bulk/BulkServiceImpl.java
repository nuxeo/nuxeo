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
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.SCHEDULED;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.kv.KeyValueStoreProvider;
import org.nuxeo.runtime.stream.StreamService;

/**
 * Basic implementation of {@link BulkService}.
 *
 * @since 10.2
 */
public class BulkServiceImpl implements BulkService {

    private static final Log log = LogFactory.getLog(BulkServiceImpl.class);

    public static final String BULK_LOG_MANAGER_NAME = "bulk";

    public static final String BULK_KV_STORE_NAME = "bulk";

    public static final String COMMAND_STREAM = "command";

    public static final String STATUS_STREAM = "status";

    public static final String DONE_STREAM = "done";

    public static final String RECORD_CODEC = "avro";

    public static final String COMMAND_SUFFIX = ":command";

    public static final String STATUS_SUFFIX = ":status";

    public static final String PRODUCE_IMMEDIATE_OPTION = "produceImmediate";

    @Override
    public String submit(BulkCommand command) {
        if (log.isDebugEnabled()) {
            log.debug("Run action with command=" + command);
        }
        // check command
        // TODO: check must be done on builder
        if (isEmpty(command.getRepository()) || isEmpty(command.getQuery()) || isEmpty(command.getAction())) {
            throw new IllegalArgumentException("Missing mandatory values");
        }
        // TODO: set default on command builder ?
        if (command.getBucketSize() == 0 || command.getBatchSize() == 0) {
            BulkAdminService adminService = Framework.getService(BulkAdminService.class);
            if (command.getBucketSize() == 0) {
                command.withBatchSize(adminService.getBatchSize(command.getAction()));
            }
            if (command.getBucketSize() == 0) {
                command.withBucketSize(adminService.getBucketSize(command.getAction()));
            }
        }

        // store the bulk command and status in the key/value store
        KeyValueStore keyValueStore = getKvStore();

        BulkStatus status = new BulkStatus();
        status.setCommandId(command.getId());
        status.setState(SCHEDULED);
        status.setSubmitTime(Instant.now());

        byte[] commandAsBytes = BulkCodecs.getCommandCodec().encode(command);
        byte[] statusAsBytes = BulkCodecs.getStatusCodec().encode(status);
        keyValueStore.put(command.getId() + COMMAND_SUFFIX, commandAsBytes);
        keyValueStore.put(command.getId() + STATUS_SUFFIX, statusAsBytes);

        // send command to bulk processor
        LogManager logManager = Framework.getService(StreamService.class).getLogManager(BULK_LOG_MANAGER_NAME);
        LogAppender<Record> logAppender = logManager.getAppender(COMMAND_STREAM);
        logAppender.append(command.getAction(), Record.of(command.getAction(), commandAsBytes));
        return command.getId();
    }

    @Override
    public BulkStatus getStatus(String commandId) {
        KeyValueStore keyValueStore = getKvStore();
        byte[] statusAsBytes = keyValueStore.get(commandId + STATUS_SUFFIX);
        if (statusAsBytes == null) {
            if (log.isDebugEnabled()) {
                log.debug("Request status of unknown command: " + commandId);
            }
            return BulkStatus.unknownOf(commandId);
        }
        return BulkCodecs.getStatusCodec().decode(statusAsBytes);
    }

    @Override
    public BulkCommand getCommand(String commandId) {
        KeyValueStore keyValueStore = getKvStore();
        byte[] statusAsBytes = keyValueStore.get(commandId + COMMAND_SUFFIX);
        if (statusAsBytes == null) {
            return null;
        }
        return BulkCodecs.getCommandCodec().decode(statusAsBytes);
    }

    @Override
    public boolean await(String commandId, Duration duration) throws InterruptedException {
        long deadline = System.currentTimeMillis() + duration.toMillis();
        KeyValueStore kvStore = getKvStore();
        do {
            if (COMPLETED.equals(
                    BulkCodecs.getStatusCodec().decode(kvStore.get(commandId + STATUS_SUFFIX)).getState())) {
                return true;
            }
            Thread.sleep(100);
        } while (deadline > System.currentTimeMillis());
        log.debug("await timeout for commandId(" + commandId + ") after " + duration.toMillis() + " ms");
        return false;
    }

    public KeyValueStore getKvStore() {
        return Framework.getService(KeyValueService.class).getKeyValueStore(BULK_KV_STORE_NAME);
    }

    @Override
    public boolean await(Duration duration) throws InterruptedException {
        KeyValueStoreProvider kv = (KeyValueStoreProvider) getKvStore();
        Set<String> commandIds = kv.keyStream()
                                   .filter(k -> k.endsWith(STATUS_SUFFIX))
                                   .map(k -> k.replaceFirst(STATUS_SUFFIX, ""))
                                   .collect(Collectors.toSet());
        // nanoTime is always monotonous
        long deadline = System.nanoTime() + duration.toNanos();
        for (String commandId : commandIds) {
            while (getStatus(commandId).getState() != COMPLETED) {
                Thread.sleep(100);
                if (deadline < System.nanoTime()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public List<BulkStatus> getStatuses(String username) {
        KeyValueStoreProvider kv = (KeyValueStoreProvider) getKvStore();
        return kv.keyStream()
                 .filter(key -> key.endsWith(COMMAND_SUFFIX)
                         && username.equals(BulkCodecs.getCommandCodec().decode(kv.get(key)).getUsername()))
                 .map(key -> BulkCodecs.getStatusCodec().decode(kv.get(key.replace(COMMAND_SUFFIX, STATUS_SUFFIX))))
                 .collect(Collectors.toList());
    }

}
