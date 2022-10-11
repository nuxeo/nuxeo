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
import java.util.Set;
import java.util.stream.Stream;

import org.nuxeo.lib.stream.log.Name;

/**
 * Memory implementation of Log.
 */
public class MemLog {

    private static final int MAX_PARTITIONS = 100;

    private final MemLogPartition[] partitions;

    public MemLog(Name name, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Number of partitions must be > 0, requested: %d".formatted(size));
        }
        if (size > MAX_PARTITIONS) {
            throw new IllegalArgumentException(
                    "Cannot create more than: %d partitions for log: %s, requested: %d".formatted(MAX_PARTITIONS, name,
                            size));
        }
        partitions = new MemLogPartition[size];
        for (int i = 0; i < size; i++) {
            partitions[i] = new MemLogPartition();
        }
    }

    public int size() {
        return partitions.length;
    }

    public MemLogPartition getPartition(int partition) {
        return partitions[partition];
    }

    public List<Name> getGroups() {
        return Stream.of(partitions).map(MemLogPartition::getGroups).flatMap(Set::stream).toList();
    }

}
