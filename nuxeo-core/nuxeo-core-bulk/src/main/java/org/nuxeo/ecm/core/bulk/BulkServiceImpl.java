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
import static org.nuxeo.ecm.core.bulk.StreamBulkProcessor.COUNTER_ACTION_NAME;
import static org.nuxeo.ecm.core.bulk.StreamBulkProcessor.KVWRITER_ACTION_NAME;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    protected static final String DOCUMENTSET_ACTION_NAME = "documentSet";

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
        LogAppender<Record> logAppender = logManager.getAppender(DOCUMENTSET_ACTION_NAME);
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

    @Override
    public boolean await(Duration duration) throws InterruptedException {
        StreamService service = Framework.getService(StreamService.class);
        LogManager logManager = service.getLogManager(BULK_LOG_MANAGER_NAME);
        BulkAdminService admin = Framework.getService(BulkAdminService.class);
        Collection<String> actions = admin.getActions();
        Collection<String> streams = new ArrayList<>(actions.size() + 3);
        streams.add(DOCUMENTSET_ACTION_NAME);
        streams.addAll(actions);
        streams.add(COUNTER_ACTION_NAME);
        streams.add(KVWRITER_ACTION_NAME);
        long deadline = System.currentTimeMillis() + duration.toMillis();
        for (String stream : streams) {
            // when there is no lag between producer and consumer we are done
            while (logManager.getLag(stream, stream).lag() > 0) {
                if (System.currentTimeMillis() > deadline) {
                    return false;
                }
                Thread.sleep(50);
            }
            // we wait for records to be actually passed to next stream
            Thread.sleep(100);
        }
        return true;
    }

    @Override
    public List<BulkStatus> getStatuses(String username) {
        KeyValueStoreProvider kv = (KeyValueStoreProvider) getKvStore();
        return kv.keyStream()
                 .filter(key -> key.endsWith(COMMAND)
                         && username.equals(BulkCodecs.getBulkCommandCodec().decode(kv.get(key)).getUsername()))
                 .map(key -> BulkCodecs.getBulkStatusCodec().decode(kv.get(key.replace(COMMAND, STATUS))))
                 .collect(Collectors.toList());
    }
}
