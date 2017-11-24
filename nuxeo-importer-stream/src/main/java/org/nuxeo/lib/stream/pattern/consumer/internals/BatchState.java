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
package org.nuxeo.lib.stream.pattern.consumer.internals;

import org.nuxeo.lib.stream.pattern.consumer.BatchPolicy;

/**
 * Keep state of a batch according to a batch policy.
 *
 * @since 9.1
 */
public class BatchState {
    protected final BatchPolicy policy;

    protected int counter;

    protected long endMs;

    public enum State {
        FILLING, FULL, TIMEOUT, LAST
    }

    State state = State.FILLING;

    public BatchState(BatchPolicy policy) {
        this.policy = policy;
    }

    public void start() {
        endMs = System.currentTimeMillis() + policy.getTimeThreshold().toMillis();
        counter = 0;
        state = State.FILLING;
    }

    public State inc() {
        if (state != State.FILLING) {
            throw new IllegalStateException("Try to add an item to a batch in non filling state:" + state);
        }
        counter++;
        return getState();
    }

    public void force() {
        state = State.FULL;
    }

    public void last() {
        state = State.LAST;
    }

    public State getState() {
        if (state != State.FILLING) {
            return state;
        }
        if (counter >= policy.getCapacity()) {
            state = State.FULL;
        } else if (System.currentTimeMillis() > endMs) {
            state = State.TIMEOUT;
        }
        return state;
    }

    public int getSize() {
        return counter;
    }

}
