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
 * Contributors:
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.marklogic;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Predicate which tests if given range element index descriptor matches a key and an object type.
 *
 * @since 8.10
 */
class RangeElementIndexPredicate implements Predicate<MarkLogicRangeElementIndexDescriptor> {

    private final String element;

    private final String type;

    public RangeElementIndexPredicate(String element, String type) {
        this.element = Objects.requireNonNull(element);
        this.type = Objects.requireNonNull(type);
    }

    @Override
    public boolean test(MarkLogicRangeElementIndexDescriptor reid) {
        return element.equals(reid.element) && type.equals(reid.type);
    }

}