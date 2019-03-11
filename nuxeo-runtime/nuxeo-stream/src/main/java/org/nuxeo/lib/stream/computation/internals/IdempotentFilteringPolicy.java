/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.lib.stream.computation.internals;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.FilteringPolicy;

/**
 * @since 11.1
 */
// TODO implement expiration ?
public class IdempotentFilteringPolicy implements FilteringPolicy {

    private static final int DEFAULT_SIZE = 100;

    protected final CircularFifoBuffer keys;

    public IdempotentFilteringPolicy() {
        this(DEFAULT_SIZE);
    }

    public IdempotentFilteringPolicy(int size) {
        keys = new CircularFifoBuffer(size);
    }

    @Override
    public boolean shouldSkip(String key, ComputationContext context) {
        boolean alreadySeen = keys.contains(key);
        keys.add(key);
        return alreadySeen;
    }

}
