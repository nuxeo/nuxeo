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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.ABORTED;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.RUNNING;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.SCHEDULED;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.UNKNOWN;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.scroll.ScrollService;
import org.nuxeo.ecm.core.bulk.message.BulkBucket;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.scroll.DocumentScrollRequest;
import org.nuxeo.ecm.core.scroll.GenericScrollRequest;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;
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

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(BulkServiceImpl.class);

    // @deprecated since 11.1 log config is not needed anymore
    @Deprecated
    public static final String BULK_LOG_MANAGER_NAME = "bulk";

    public static final String BULK_KV_STORE_NAME = "bulk";

    public static final String COMMAND_STREAM = "bulk/command";

    // @since 11.1
    public static final Name COMMAND_STREAM_NAME = Name.ofUrn(COMMAND_STREAM);

    public static final String STATUS_STREAM = "bulk/status";

    // @since 11.1
    public static final Name STATUS_STREAM_NAME = Name.ofUrn(STATUS_STREAM);

    public static final String DONE_STREAM = "bulk/done";

    // @since 11.1
    public static final Name DONE_STREAM_NAME = Name.ofUrn(DONE_STREAM);

    public static final String COMMAND_PREFIX = "command:";

    // @deprecated since 11.4 not needed anymore
    @Deprecated(since = "11.4")
    public static final String RECORD_CODEC = "avro";

    public static final String STATUS_PREFIX = "status:";

    public static final String PRODUCE_IMMEDIATE_OPTION = "produceImmediate";

    // How long we keep the command and its status in the kv store once completed
    public static final long COMPLETED_TTL_SECONDS = 3_600;

    // How long we keep the command and its status in the kv store once aborted
    public static final long ABORTED_TTL_SECONDS = 43_200;

    // @since 11.5
    // How long we keep the command and its status in the kv store once completed with an error
    public static final long COMPLETED_IN_ERROR_TTL_SECONDS = 86_400;

    // @since 11.3
    protected final AtomicLong externalScrollerCounter = new AtomicLong();

    // @since 11.3
    protected final Map<String, BulkCommand> externalCommands = new PassiveExpiringMap<>(60, TimeUnit.SECONDS);

    @Override
    public String submit(BulkCommand command) {
        log.debug("Run action with command={}", command);
        // check command
        BulkAdminService adminService = Framework.getService(BulkAdminService.class);
        if (!adminService.getActions().contains(command.getAction())) {
            throw new IllegalArgumentException("Unknown action for command: " + command);
        }
        BulkActionValidation actionValidation = adminService.getActionValidation(command.getAction());

        // Try to validate the action if a validation class is provided
        if (actionValidation != null) {
            actionValidation.validate(command);
        }
        RepositoryManager repoManager = Framework.getService(RepositoryManager.class);
        if (repoManager != null) {
            if (isEmpty(command.getRepository())) {
                command.setRepository(repoManager.getDefaultRepositoryName());
            } else if (repoManager.getRepository(command.getRepository()) == null) {
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
        if (Duration.ZERO.equals(command.getBatchTransactionTimeout())) {
            command.setBatchTransactionTimeout(adminService.getBatchTransactionTimeout(command.getAction()));
        }
        if (command.getQueryLimit() == null) {
            command.setQueryLimit(adminService.getQueryLimit(command.getAction()));
        }
        if (command.getScroller() == null && !command.useExternalScroller()) {
            String actionScroller = adminService.getDefaultScroller(command.getAction());
            if (!isBlank(actionScroller)) {
                command.setScroller(actionScroller);
            }
        }
        checkIfScrollerExists(command);

        // store the bulk command and status in the key/value store
        BulkStatus status = new BulkStatus(command.getId());
        status.setState(SCHEDULED);
        status.setAction(command.getAction());
        status.setUsername(command.getUsername());
        status.setSubmitTime(Instant.now());
        setStatus(status);
        byte[] commandAsBytes = setCommand(command);

        String shardKey;
        if (adminService.isSequentialCommands(command.getAction())) {
            // no concurrency all commands for this action goes to the same partition
            shardKey = command.getAction();
        } else {
            // use a random value
            shardKey = command.getId();
        }
        // send command to bulk processor
        log.debug("Submit action with command: {}", command);
        return submit(shardKey, command.getId(), commandAsBytes);
    }

    protected void checkIfScrollerExists(BulkCommand command) {
        ScrollService scrollService = Framework.getService(ScrollService.class);
        if (command.useExternalScroller()) {
            // nothing to do
        } else if (command.useGenericScroller()) {
            if (!scrollService.exists(
                    GenericScrollRequest.builder(command.getScroller(), command.getQuery()).build())) {
                throw new IllegalArgumentException("Unknown Generic Scroller for command: " + command);
            }
        } else if (!scrollService.exists(
                DocumentScrollRequest.builder(command.getQuery()).name(command.getScroller()).build())) {
            throw new IllegalArgumentException("Unknown Document Scroller for command: " + command);
        }
    }

    @SuppressWarnings("resource") // LogManager not ours to close
    protected String submit(String shardKey, String key, byte[] bytes) {
        LogManager logManager = Framework.getService(StreamService.class).getLogManager();
        LogAppender<Record> logAppender = logManager.getAppender(COMMAND_STREAM_NAME);
        Record record = Record.of(key, bytes);
        log.debug("Append shardKey: {}, record: {}", shardKey, record);
        logAppender.append(shardKey, record);
        return key;
    }

    @Override
    public BulkStatus getStatus(String commandId) {
        KeyValueStore keyValueStore = getKvStore();
        byte[] statusAsBytes = keyValueStore.get(STATUS_PREFIX + commandId);
        if (statusAsBytes == null) {
            log.debug("Request status of unknown command: {}", commandId);
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
            kvStore.put(STATUS_PREFIX + status.getId(), statusAsBytes, ABORTED_TTL_SECONDS);
            // we remove the command from the kv store, so computation have to handle abort
            kvStore.put(COMMAND_PREFIX + status.getId(), (String) null);
            break;
        case COMPLETED:
            long ttl = status.hasError() ? COMPLETED_IN_ERROR_TTL_SECONDS : COMPLETED_TTL_SECONDS;
            kvStore.put(STATUS_PREFIX + status.getId(), statusAsBytes, ttl);
            kvStore.setTTL(COMMAND_PREFIX + status.getId(), ttl);
            break;
        default:
            kvStore.put(STATUS_PREFIX + status.getId(), statusAsBytes);
        }
        return statusAsBytes;
    }

    @Override
    public BulkCommand getCommand(String commandId) {
        KeyValueStore keyValueStore = getKvStore();
        byte[] statusAsBytes = keyValueStore.get(COMMAND_PREFIX + commandId);
        if (statusAsBytes == null) {
            return null;
        }
        return BulkCodecs.getCommandCodec().decode(statusAsBytes);
    }

    @Override
    public BulkStatus abort(String commandId) {
        BulkStatus status = getStatus(commandId);
        if (COMPLETED.equals(status.getState())) {
            log.debug("Cannot abort a completed command: {}", commandId);
            return status;
        }
        status.setState(ABORTED);
        // set the status in the KV store
        setStatus(status);
        // Send a delta to the status computation
        BulkStatus delta = BulkStatus.deltaOf(commandId);
        delta.setCompletedTime(Instant.now());
        delta.setState(ABORTED);
        Record record = Record.of(commandId, BulkCodecs.getStatusCodec().encode(delta));
        Framework.getService(StreamService.class).getStreamManager().append(STATUS_STREAM, record);
        return status;
    }

    @Override
    public Map<String, Serializable> getResult(String commandId) {
        return getStatus(commandId).getResult();
    }

    /**
     * Stores the command in the kv store, returns the encoded command.
     */
    public byte[] setCommand(BulkCommand command) {
        KeyValueStore kvStore = getKvStore();
        byte[] commandAsBytes = BulkCodecs.getCommandCodec().encode(command);
        kvStore.put(COMMAND_PREFIX + command.getId(), commandAsBytes);
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
                log.error("Unknown status for command: {}", commandId);
                return false;
            default:
                // continue
            }
            Thread.sleep(100);
        } while (deadline > System.currentTimeMillis());
        log.debug("await timeout on {} after {} ms", () -> getStatus(commandId), duration::toMillis);
        return false;
    }

    public KeyValueStore getKvStore() {
        return Framework.getService(KeyValueService.class).getKeyValueStore(BULK_KV_STORE_NAME);
    }

    @Override
    public boolean await(Duration duration) throws InterruptedException {
        KeyValueStoreProvider kv = (KeyValueStoreProvider) getKvStore();
        Set<String> commandIds = kv.keyStream(STATUS_PREFIX)
                                   .map(k -> k.replaceFirst(STATUS_PREFIX, ""))
                                   .collect(Collectors.toSet());
        log.debug("Wait for command ids: {}", commandIds);
        // nanoTime is always monotonous
        long deadline = System.nanoTime() + duration.toNanos();
        for (String commandId : commandIds) {
            log.debug("Wait for command id: {}", commandId);
            for (;;) {
                BulkStatus status = getStatus(commandId);
                log.debug("Status of command: {} = {}", commandId, status);
                BulkStatus.State state = status.getState();
                log.debug("State of command: {} = {}", commandId, state);
                if (state == COMPLETED || state == ABORTED || state == UNKNOWN) {
                    break;
                }
                Thread.sleep(200);
                if (deadline < System.nanoTime()) {
                    log.debug("await timeout, at least one uncompleted command: {}", status);
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public List<BulkStatus> getStatuses(String username) {
        KeyValueStoreProvider kv = (KeyValueStoreProvider) getKvStore();
        return kv.keyStream(STATUS_PREFIX)
                 .map(kv::get)
                 .map(BulkCodecs.getStatusCodec()::decode)
                 .filter(status -> username.equals(status.getUsername()))
                 .collect(Collectors.toList());
    }

    @Override
    public void appendExternalBucket(BulkBucket bucket) {
        String commandId = bucket.getCommandId();

        BulkCommand command = externalCommands.computeIfAbsent(commandId, this::getCommand);
        String stream = Framework.getService(BulkAdminService.class).getInputStream(command.getAction());

        String key = commandId + ":" + externalScrollerCounter.incrementAndGet();
        Record record = Record.of(key, BulkCodecs.getBucketCodec().encode(bucket));

        log.debug("Append key: {}, record: {}", key, record);
        Framework.getService(StreamService.class).getStreamManager().append(stream, record);
    }

    @Override
    public void completeExternalScroll(String commandId, long count) {
        BulkStatus delta = BulkStatus.deltaOf(commandId);
        delta.setState(RUNNING);
        delta.setScrollEndTime(Instant.now());
        delta.setTotal(count);

        Record record = Record.of(commandId, BulkCodecs.getStatusCodec().encode(delta));

        log.debug("Complete external scroll with key: {}, count: {}, record: {}", commandId, count, record);
        Framework.getService(StreamService.class).getStreamManager().append(STATUS_STREAM, record);
    }
}
