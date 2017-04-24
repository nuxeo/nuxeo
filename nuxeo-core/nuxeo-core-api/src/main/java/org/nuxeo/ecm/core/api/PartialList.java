/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api;

import java.util.Iterator;
import java.util.List;

/**
 * The bundling of a list and a total size.
 */
public class PartialList<E> implements Iterable<E> {

    public final List<E> list;

    public final long totalSize;

    /**
     * Constructs a partial list.
     *
     * @param list the list
     * @param totalSize the total size
     */
    public PartialList(List<E> list, long totalSize) {
        this.list = list;
        this.totalSize = totalSize;
    }

    @Override
    public Iterator<E> iterator() {
        return list.iterator();
    }

}
