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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;

import com.google.common.collect.Lists;

/**
 * Returns a static result list of identifiers.
 *
 * @since 11.1
 */
public class StaticScroll implements Scroll {

    protected ScrollRequest request;

    protected List<List<String>> partitions;

    int currentPosition;

    int nextPosition;

    @Override
    public void init(ScrollRequest request, Map<String, String> options) {
        this.request = request;
        List<String> ids = Arrays.stream(request.getQuery().split(","))
                                 .map(String::trim)
                                 .filter(StringUtils::isNotBlank)
                                 .collect(Collectors.toList());
        partitions = Lists.partition(ids, request.getSize());
        currentPosition = -1;
        nextPosition = 0;
    }

    @Override
    public boolean fetch() {
        currentPosition = nextPosition;
        nextPosition += 1;
        return currentPosition < partitions.size();
    }

    @Override
    public List<String> getIds() {
        if (currentPosition >= partitions.size()) {
            return Collections.emptyList();
        }
        if (currentPosition < 0) {
            throw new IllegalStateException("fetch must be called first");
        }
        return partitions.get(currentPosition);
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
