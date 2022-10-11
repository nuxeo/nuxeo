/*
 * (C) Copyright 2022 Nuxeo.
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
 */
package org.nuxeo.lib.stream.log.mem;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.nuxeo.lib.stream.log.Name;

/**
 * Memory implementation of Logs.
 */
public class MemLogs {

    private final Map<Name, MemLog> logs = new ConcurrentHashMap<>();

    public MemLog createLog(Name name, int size) {
        MutableBoolean created = new MutableBoolean();
        MemLog log = logs.computeIfAbsent(name, k -> {
            created.setTrue();
            return new MemLog(name, size);
        });
        if (created.isFalse()) {
            throw new IllegalArgumentException("Log already exists: " + name);
        }
        return log;
    }

    public Optional<MemLog> getLogOptional(Name name) {
        return Optional.ofNullable(logs.get(name));
    }

    public MemLog getLog(Name name) {
        return getLogOptional(name).orElseThrow(() -> new IllegalArgumentException("Invalid name: " + name));
    }

    public boolean deleteLog(Name name) {
        return logs.remove(name) != null;
    }

    public List<Name> listAllNames() {
        return List.copyOf(logs.keySet());
    }

    public boolean exists(Name name) {
        return logs.containsKey(name);
    }

    public void clear() {
        logs.clear();
    }

}
