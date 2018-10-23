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
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.ABORTED;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.SCHEDULED;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.UNKNOWN;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
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

    // How long we keep the command and its status in the kv store once completed
    public static final long COMPLETED_TTL_SECONDS = 3_600;

    // How long we keep the command and its status in the kv store once aborted
    public static final long ABORTED_TTL_SECONDS = 7_200;

    @Override
    public String submit(BulkCommand command) {
        if (log.isDebugEnabled()) {
            log.debug("Run action with command=" + command);
        }
        // check command
        BulkAdminService adminService = Framework.getService(BulkAdminService.class);
        if (!adminService.getActions().contains(command.getAction())) {
            throw new IllegalArgumentException("Unknown action for command: " + command);
        }
        RepositoryManager repoManager = Framework.getService(RepositoryManager.class);
        if (isEmpty(command.getRepository())) {
            command.setRepository(repoManager.getDefaultRepositoryName());
        } else {
            if (repoManager.getRepository(command.getRepository()) == null) {
                throw new IllegalArgumentException("Unknown repository: " + command);
            }
        }
        if (command.getBucketSize() == 0 || command.getBatchSize() == 0) {

            if (command.getBucketSize() == 0) {
                command.setBucketSize(adminService.getBucketSize(command.getAction()));
            }
            if (command.getBatchSize() == 0) {
                command.setBatchSize(adminService.getBatchSize(command.getAction()));
            }
        }

        // store the bulk command and status in the key/value store
        BulkStatus status = new BulkStatus(command.getId());
        status.setState(SCHEDULED);
        status.setAction(command.getAction());
        status.setSubmitTime(Instant.now());
        setStatus(status);
        byte[] commandAsBytes = setCommand(command);

        // send command to bulk processor
        LogManager logManager = Framework.getService(StreamService.class).getLogManager(BULK_LOG_MANAGER_NAME);
        LogAppender<Record> logAppender = logManager.getAppender(COMMAND_STREAM);
        logAppender.append(command.getId(), Record.of(command.getId(), commandAsBytes));
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

    /**
     * Stores the status in the kv store returns the encoded status
     */
    public byte[] setStatus(BulkStatus status) {
        KeyValueStore kvStore = getKvStore();
        byte[] statusAsBytes = BulkCodecs.getStatusCodec().encode(status);
        switch (status.getState()) {
        case ABORTED:
            kvStore.put(status.getCommandId() + STATUS_SUFFIX, statusAsBytes, ABORTED_TTL_SECONDS);
            // we remove the command from the kv store, so computation have to handle the abortion
            kvStore.put(status.getCommandId() + COMMAND_SUFFIX, (String) null);
            break;
        case COMPLETED:
            kvStore.put(status.getCommandId() + STATUS_SUFFIX, statusAsBytes, COMPLETED_TTL_SECONDS);
            kvStore.setTTL(status.getCommandId() + COMMAND_SUFFIX, COMPLETED_TTL_SECONDS);
            break;
        default:
            kvStore.put(status.getCommandId() + STATUS_SUFFIX, statusAsBytes);
        }
        return statusAsBytes;
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
    public BulkStatus abort(String commandId) {
        BulkStatus status = getStatus(commandId);
        if (COMPLETED.equals(status.getState())) {
            // too late
            return status;
        }
        // TODO: Check that the current user is either admin either the command username
        status.setState(ABORTED);
        // set the status in the KV store
        setStatus(status);
        // Send a delta to the status computation
        BulkStatus delta = BulkStatus.deltaOf(commandId);
        delta.setCompletedTime(Instant.now());
        delta.setState(ABORTED);
        byte[] statusAsBytes = BulkCodecs.getStatusCodec().encode(delta);
        LogManager logManager = Framework.getService(StreamService.class).getLogManager(BULK_LOG_MANAGER_NAME);
        LogAppender<Record> logAppender = logManager.getAppender(STATUS_STREAM);
        logAppender.append(commandId, Record.of(commandId, statusAsBytes));
        return status;
    }

    /**
     * Stores the command in the kv store, returns the encoded command.
     */
    public byte[] setCommand(BulkCommand command) {
        KeyValueStore kvStore = getKvStore();
        byte[] commandAsBytes = BulkCodecs.getCommandCodec().encode(command);
        kvStore.put(command.getId() + COMMAND_SUFFIX, commandAsBytes);
        return commandAsBytes;
    }

    @Override
    public boolean await(String commandId, Duration duration) throws InterruptedException {
        long deadline = System.currentTimeMillis() + duration.toMillis();
        BulkStatus status;
        do {
            status = getStatus(commandId);
            switch (status.getState()) {
            case COMPLETED:
            case ABORTED:
                return true;
            case UNKNOWN:
                log.error("Unknown status for command: " + commandId);
                return false;
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
            for (;;) {
                BulkStatus.State state = getStatus(commandId).getState();
                if (state == COMPLETED || state == ABORTED || state == UNKNOWN) {
                    break;
                }
                Thread.sleep(200);
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
