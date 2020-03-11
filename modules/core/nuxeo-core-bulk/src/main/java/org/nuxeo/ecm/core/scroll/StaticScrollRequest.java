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
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.scroll.ScrollRequest;

/**
 * Scroll Request for a static result set.
 *
 * @since 11.1
 */
public class StaticScrollRequest implements ScrollRequest {

    protected static final String SCROLL_TYPE = "static";

    protected static final String SCROLL_NAME = "list";

    protected final int size;

    protected final List<String> identifiers;

    protected StaticScrollRequest(Builder builder) {
        this.identifiers = builder.identifiers;
        this.size = builder.getSize();
    }

    @Override
    public String getType() {
        return SCROLL_TYPE;
    }

    @Override
    public String getName() {
        return SCROLL_NAME;
    }

    @Override
    public int getSize() {
        return size;
    }

    public List<String> getIdentifiers() {
        return identifiers;
    }

    public static Builder builder(String singleIdentifier) {
        return new Builder(singleIdentifier);
    }

    public static Builder builder(List<String> identifiers) {
        return new Builder(identifiers);
    }

    @Override
    public String toString() {
        return "StaticScrollRequest{" + "size=" + size + ", identifiers=" + identifiers + '}';
    }

    public static class Builder {

        public static final int DEFAULT_SCROLL_SIZE = 10;

        protected final List<String> identifiers;

        protected int size;

        public Builder(String singleIdentifier) {
            Objects.requireNonNull(singleIdentifier, "identifier cannot be null");
            this.identifiers = Collections.singletonList(singleIdentifier);
        }

        public Builder(List<String> identifiers) {
            Objects.requireNonNull(identifiers, "identifiers cannot be null");
            List<String> ids = identifiers.stream()
                                          .map(String::trim)
                                          .filter(Predicate.not(String::isBlank))
                                          .collect(Collectors.toList());
            if (ids.isEmpty()) {
                throw new IllegalArgumentException("identifiers cannot be empty");
            }
            this.identifiers = Collections.unmodifiableList(ids);
        }

        public Builder size(int size) {
            if (size <= 0) {
                throw new IllegalArgumentException("size must be > 0");
            }
            this.size = size;
            return this;
        }

        public int getSize() {
            return size == 0 ? DEFAULT_SCROLL_SIZE : size;
        }

        public StaticScrollRequest build() {
            return new StaticScrollRequest(this);
        }
    }

}
