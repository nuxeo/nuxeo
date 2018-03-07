/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.lib.stream.log;

/**
 * A message position in a partition.
 *
 * @since 9.3"
 */
public interface LogOffset extends Comparable<LogOffset> {
    /**
     * Returns the tuple Log name and partition.
     */
    LogPartition partition();

    /**
     * The position for the this {@link #partition()}.
     */
    long offset();

    /**
     * Returns the next offset corresponding to the next position in the same partition.
     *
     * @since 10.1
     */
    LogOffset nextOffset();
}
