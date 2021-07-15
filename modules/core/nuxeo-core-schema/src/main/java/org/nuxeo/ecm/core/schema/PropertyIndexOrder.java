/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.schema;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.function.UnaryOperator;

/**
 * @since 11.5
 */
public class PropertyIndexOrder {

    protected final String path;

    protected final IndexOrder indexOrder;

    public PropertyIndexOrder(String path, String indexOrder) {
        this.path = path;
        this.indexOrder = IndexOrder.parse(indexOrder);
    }

    protected PropertyIndexOrder(String path, IndexOrder indexOrder) {
        this.path = path;
        this.indexOrder = indexOrder;
    }

    public String getPath() {
        return path;
    }

    public IndexOrder getIndexOrder() {
        return indexOrder;
    }

    public boolean isIndexNotNone() {
        return indexOrder.isNotNone();
    }

    public PropertyIndexOrder replacePath(UnaryOperator<String> operator) {
        return new PropertyIndexOrder(operator.apply(path), indexOrder);
    }

    public enum IndexOrder {
        ASCENDING, DESCENDING, NONE;

        public boolean isNotNone() {
            return this != NONE;
        }

        public static IndexOrder parse(String indexOrder) {
            if ("ascending".equalsIgnoreCase(indexOrder)) {
                return ASCENDING;
            } else if ("descending".equalsIgnoreCase(indexOrder)) {
                return DESCENDING;
            } else if ("none".equalsIgnoreCase(indexOrder) || isBlank(indexOrder)) {
                return NONE;
            }
            throw new IllegalArgumentException("The indexOrder: " + indexOrder + " could not be parsed");
        }
    }
}
