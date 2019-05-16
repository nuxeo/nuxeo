/*
 * (C) Copyright 2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.lib.stream.computation.log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogPartition;

import net.jodah.failsafe.RetryPolicy;
/**
 * Stores contexts of terminated ComputationRunners.
 * 
 * It's a low-level storage independent of Nuxeo that can be accessed by higher-level components.
 * 
 * @since 11.1
 */
public class ComputationRunnerTerminated {

    public static class ComputationRunnerTerminatedContext {

        public final String name;

        public final List<LogPartition> partitions;

        public final LogOffset logOffset;

        public final RetryPolicy retryPolicy;

        public final long timestamp;

        public ComputationRunnerTerminatedContext(//
                String name, //
                LogOffset logOffset, //
                long timestamp, //
                RetryPolicy retryPolicy, //
                Collection<LogPartition> partitions) {
            super();
            this.name = name;
            this.logOffset = logOffset;
            this.timestamp = timestamp;
            this.retryPolicy = retryPolicy;
            this.partitions = new ArrayList<>(partitions);
            Collections.sort(this.partitions, Comparator.comparing(LogPartition::toString));
        }

        public String id() {
            return name + partitions.toString();
        }

    }

    protected static final Collection<ComputationRunnerTerminatedContext> contexts = Collections.synchronizedList(
            new LinkedList<>());

    public static void registerTerminated(String name, Collection<LogPartition> partitions, LogOffset logOffset,
            RetryPolicy retryPolicy, long timestamp) {
        contexts.add(new ComputationRunnerTerminatedContext(name, logOffset, timestamp, retryPolicy, partitions));
    }

    public static boolean hasBlockedStream() {
        return !contexts.isEmpty();
    }

    public static Collection<ComputationRunnerTerminatedContext> getErrors() {
        return Collections.unmodifiableCollection(contexts);
    }

    public static void clear() {
        contexts.clear();
    }

}
