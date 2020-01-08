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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.scroll;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;

import com.google.common.collect.Lists;

/**
 * Returns a static result list of identifiers.
 *
 * @since 11.1
 */
public class StaticScroll implements Scroll {

    protected StaticScrollRequest request;

    protected List<List<String>> partitions;

    protected int currentPosition;

    protected int nextPosition;

    @Override
    public void init(ScrollRequest request, Map<String, String> options) {
        if (!(request instanceof StaticScrollRequest)) {
            throw new IllegalArgumentException("Requires a StaticScrollRequest");
        }
        this.request = (StaticScrollRequest) request;
        partitions = Lists.partition(this.request.getIdentifiers(), request.getSize());
        currentPosition = 0;
    }

    @Override
    public boolean hasNext() {
        return currentPosition < partitions.size();
    }

    @Override
    public List<String> next() {
        if (currentPosition >= partitions.size()) {
            throw new NoSuchElementException();
        }
        return partitions.get(currentPosition++);
    }

    @Override
    public void close() {
        partitions = null;
    }

    @Override
    public String toString() {
        return "StaticScroll request: " + request + " currentPos: " + currentPosition + " nextPos: " + nextPosition
                + " ids: " + partitions;
    }
}
