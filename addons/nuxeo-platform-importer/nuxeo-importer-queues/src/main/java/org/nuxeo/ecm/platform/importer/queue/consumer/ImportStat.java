/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.importer.queue.consumer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @since 8.3
 */
public class ImportStat implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final Map<String, Long> statMap;

    public ImportStat() {
        statMap = new HashMap<>();
    }

    public ImportStat(Map<String, Long> initMap) {
        statMap = initMap;
    }

    public void increaseStat(String key, long count) {
        if (statMap.containsKey(key)) {
            long previousCount = statMap.get(key);
            count += previousCount;
        }
        statMap.put(key, count);
    }

    public boolean containsKey(String key) {
        return statMap.containsKey(key);
    }

    public Set<String> keySet() {
        return statMap.keySet();
    }

    public long getStat(String key) {
        return statMap.get(key);
    }

    public void merge(ImportStat other) {
        for (String key : other.keySet()) {
            if (statMap.containsKey(key)) {
                increaseStat(key, other.getStat(key));
            } else {
                statMap.put(key, other.getStat(key));
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Import stat :\n");
        for (String key : statMap.keySet()) {
            sb.append("[");
            sb.append(key);
            sb.append("]: ");
            sb.append(statMap.get(key));
            sb.append("\n");
        }

        return sb.toString();
    }

    public Map<String, Long> getStatMap() {
        return new HashMap<>(statMap);
    }
}
