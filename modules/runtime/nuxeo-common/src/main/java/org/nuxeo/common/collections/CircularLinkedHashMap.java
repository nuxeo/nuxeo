/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.common.collections;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @since 2025.19
 */
public class CircularLinkedHashMap<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 1L;

    protected final int maxSize;

    public CircularLinkedHashMap(int maxSize) {
        super(maxSize);
        this.maxSize = maxSize;
    }

    public CircularLinkedHashMap(int maxSize, boolean accessOrder) {
        super(maxSize, 0.75f, accessOrder);
        this.maxSize = maxSize;
    }

    public CircularLinkedHashMap(int maxSize, float loadFactor, boolean accessOrder) {
        super(maxSize, loadFactor, accessOrder);
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}
