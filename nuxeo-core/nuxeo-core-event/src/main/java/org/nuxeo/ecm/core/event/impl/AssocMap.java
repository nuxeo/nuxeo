/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A map used to track duplicates.
 * <p>
 * This class is not synchronized on read; this means you need to populate the map before using it.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AssocMap {

    protected final Map<String, Set<String>> peers = new HashMap<>();

    public synchronized void put(String x, String y) {
        Set<String> px = peers.get(x);
        Set<String> py = peers.get(y);
        if (px == py) {
            return; // already associated
        }
        if (px == null) {
            py.add(x);
            peers.put(x, py);
        } else if (py == null) {
            px.add(y);
            peers.put(y, px);
        } else { // both members are already in relation with other members
            Set<String> set = new HashSet<>();
            set.addAll(px);
            set.addAll(py);
            for (String key : set.toArray(new String[set.size()])) {
                peers.put(key, set);
            }
        }
    }

    public boolean isPeerOf(String x, String y) {
        Set<String> set = peers.get(x);
        return set != null && set.contains(y);
    }

}
