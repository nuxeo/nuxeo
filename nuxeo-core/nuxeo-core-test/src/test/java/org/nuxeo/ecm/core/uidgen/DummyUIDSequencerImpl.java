/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *
 */
package org.nuxeo.ecm.core.uidgen;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class DummyUIDSequencerImpl extends AbstractUIDSequencer {

    protected Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    protected long delta = 1L;

    @Override
    public void init() {
    }

    @Override
    public int getNext(String key) {
        return (int) getNextLong(key);
    }

    @Override
    public long getNextLong(String key) {
        counters.putIfAbsent(key, new AtomicLong());
        return counters.get(key).addAndGet(delta);
    }

    @Override
    public void dispose() {
    }

}
