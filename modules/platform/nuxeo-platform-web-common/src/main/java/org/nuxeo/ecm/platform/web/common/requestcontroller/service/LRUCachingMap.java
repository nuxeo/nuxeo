/*
 * (C) Copyright 2006-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.web.common.requestcontroller.service;

import org.nuxeo.common.collections.CircularLinkedHashMap;

/**
 * @author tiry
 * @deprecated since 2025.19, use {@link org.nuxeo.common.collections.CircularLinkedHashMap} instead
 */
public class LRUCachingMap<K, V> extends CircularLinkedHashMap<K, V> {

    private static final long serialVersionUID = 1L;

    public LRUCachingMap(int maxCachedItems) {
        super(maxCachedItems, 1.0f, true);
    }
}
